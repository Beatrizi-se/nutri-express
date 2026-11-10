# ⚙️ 03. Regras de Negócio e Transações

> **Objetivo:** Compreender a lógica de negócio do Nutri Express, integridade de estoque, máquina de estados de pedidos e consistência de dados.

---

## 1. Controle Atômico de Estoque e Transações (`@Transactional`)

### O Desafio Clássico do Estoque
Imagine a seguinte situação:
Um cliente faz um pedido com 2 pratos:
- **Item 1:** Salada Caesar (Quantidade: 1, Estoque disponível: 10) -> **Tem estoque!**
- **Item 2:** Suco Detox (Quantidade: 5, Estoque disponível: 2) -> **Estoque insuficiente!**

Se o código executasse sem controle transacional:
1. O sistema daria baixa de 1 unidade na Salada Caesar (estoque cai de 10 para 9).
2. O sistema tentaria processar o Suco Detox e lançaria um erro.
3. **O problema:** O pedido não foi criado, mas o estoque da Salada já foi reduzido! O estoque do restaurante acabou de sofrer uma **inconsistência de dados**.

---

### A Solução: A Anotação `@Transactional`
No método `criarPedido` de `PedidoService.java`:

```java
@Transactional
public Pedido criarPedido(PedidoRequestDTO dto) {
    // 1. Valida cliente
    // 2. Itera sobre os itens
    // 3. Decrementa o estoque de cada prato
    // 4. Salva o pedido
}
```

O `@Transactional` instrui o Spring a abrir uma **Transação ACID** com o banco de dados:
- **A (Atomicidade):** Ou **todas** as operações acontecem juntas com sucesso (*Commit*), ou **nenhuma** acontece (*Rollback*).
- Se qualquer exceção (`RegraNegocioException`) for lançada no meio do loop, o Spring intercepta e desfaz imediatamente todas as baixas de estoque efetuadas até aquele instante.

### Estorno de Estoque no Cancelamento
Se um pedido for cancelado pelo cliente antes de sair para entrega:
```java
@Transactional
public Pedido cancelarPedido(Long id) {
    Pedido pedido = buscarPorId(id);

    // Só pode cancelar se não tiver saído para entrega ou sido entregue
    if (pedido.getStatus() == StatusPedido.ENTREGUE || pedido.getStatus() == StatusPedido.A_CAMINHO) {
        throw new RegraNegocioException("Não é possível cancelar um pedido que já está em rota ou foi entregue");
    }

    // Estorna as quantidades de volta para o prato via associacao direta
    for (ItemPedido item : pedido.getItens()) {
        Prato prato = item.getPrato();
        if (prato != null) {
            prato.incrementarEstoque(item.getQuantidade());
            pratoRepository.save(prato);
        }
    }

    pedido.setStatus(StatusPedido.CANCELADO);
    return pedidoRepository.save(pedido);
}
```

---

## 2. O Conceito de Snapshot de Preço (Congelamento Histórico)

### O Problema do Preço Dinâmico
Considere o seguinte cenário:
1. No dia **01/01**, o prato *"Filé de Frango Grelhado"* custava **R$ 25,00**.
2. A cliente Maria comprou 2 pratos. Total do pedido: **R$ 50,00**.
3. No dia **15/01**, a inflação aumentou os custos e o restaurante atualizou o preço do prato no cardápio para **R$ 30,00**.

Se a entidade `ItemPedido` guardasse apenas a referência ao prato (`prato.getPreco()`), quando a Maria abrisse o histórico de compras dela em fevereiro:
- O sistema recalcularia: 2 pratos × R$ 30,00 = **R$ 60,00**!
- O comprovante fiscal e o valor da fatura do cartão não bateriam com o banco de dados.

### A Solução no Nutri Express
Na entidade `ItemPedido.java`, o construtor recebe a referência do `Prato`, a quantidade e o preço capturado no checkout:
```java
public ItemPedido(Prato prato, Integer quantidade, Double precoUnitario) {
    if (prato == null) {
        throw new IllegalArgumentException("O prato do item não pode ser nulo.");
    }
    if (quantidade == null || quantidade <= 0) {
        throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");
    }
    if (precoUnitario == null || precoUnitario <= 0.0) {
        throw new IllegalArgumentException("O preço unitário do item deve ser informado e maior que zero.");
    }
    this.prato = prato;
    this.pratoNome = prato.getNome();
    this.quantidade = quantidade;
    this.precoUnitario = precoUnitario; // PREÇO CONGELADO NO INSTANTE DA VENDA
    this.subtotal = precoUnitario * quantidade;
}
```

Dessa forma:
- O `Prato` no cardápio pode mudar de preço amanhã, semana que vem ou no ano que vem.
- O `ItemPedido` registrado preserva eternamente o preço exato pelo qual ele foi comercializado naquele dia.
- A validação rejeita qualquer valor nulo ou zerado sem recorrer a fallbacks mágicos silenciosos.

---

## 3. Máquina de Estados de Pedidos (`StatusPedido`)

Os pedidos seguem um ciclo de vida estrito para evitar transições ilegais:

```
                  ┌──────────────┐
                  │   PENDENTE   │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │  CONFIRMADO  │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │  EM_PREPARO  │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │  A_CAMINHO   │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │   ENTREGUE   │ (Estado Final de Sucesso)
                  └──────────────┘

        * Em qualquer estado anterior a 'A_CAMINHO', o pedido pode ir para:
                  ┌──────────────┐
                  │  CANCELADO   │ (Estado Final com Estorno de Estoque)
                  └──────────────┘
```

---

## 4. Soft Delete vs. Hard Delete (Exclusão Lógica de Clientes)

### O que é Hard Delete?
É o clássico comando SQL `DELETE FROM cliente WHERE id = 1`. O registro é fisicamente extirpado do banco de dados.

**Por que o Hard Delete é perigoso em e-commerce / delivery?**
Se um cliente comprou no seu restaurante nos últimos 2 anos e decide "apagar a conta":
- Se você executar um `DELETE` físico na tabela de clientes, o banco de dados acusará erro de chave estrangeira (`ConstraintViolationException`), pois existem centenas de pedidos atrelados àquele ID de cliente.
- Se você usar `CASCADE DELETE`, apagará todos os pedidos históricos do cliente, gerando um rombo contábil no faturamento da empresa.

### A Abordagem Adotada: Soft Delete (Inativação Lógica)
Na entidade `Cliente.java`, adicionamos o campo booleano:
```java
@Column(nullable = false)
private Boolean ativo = true;
```

Quando o endpoint `DELETE /api/v1/clientes/{id}` é chamado:
```java
@Transactional
public void inativarCliente(Long id) {
    Cliente cliente = buscarPorId(id);
    cliente.setAtivo(false); // NÃO deleta do banco!
    repository.save(cliente);
}
```

**Benefícios:**
1. Os relatórios financeiros e pedidos anteriores permanecem intactos.
2. O cliente inativo fica proibido de fazer novos pedidos (o `PedidoService` verifica `if (!cliente.getAtivo())`).
3. O cliente pode ser reativado a qualquer momento via `PATCH /api/v1/clientes/{id}/reativar`.

---

## 5. Relacionamentos JPA vs. Snapshots Históricos e Integridade Referencial

Na **Aula 12**, o projeto evoluiu de chaves estrangeiras escalares soltas (`categoriaId`, `clienteId`, `pratoId`) para associações JPA gerenciadas, mantendo regras estritas de integridade de dados e auditoria fiscal:

### A. Coexistência de Snapshots Históricos e Chaves Estrangeiras

Um dos maiores erros em sistemas transacionais é substituir campos de snapshot por navegação direta de entidades:
- **No Pedido:** O relacionamento `@ManyToOne` aponta para o registro atual de `Cliente`. No entanto, os campos `clienteNome` e `enderecoEntrega` são **copiados e mantidos na tabela `pedido`**. Se o cliente mudar de endereço ou retificar seu nome no futuro, o histórico contábil de entregas passadas não é corrompido.
- **No ItemPedido:** O relacionamento `@ManyToOne` aponta para a entidade `Prato` (garantindo chave estrangeira `fk_item_pedido_prato` no banco). Contudo, `pratoNome`, `precoUnitario` e `subtotal` são campos congelados na tabela `item_pedido`. Mesmo que o prato sofra reajuste no cardápio, a venda mantém exatamente o valor negociado no instante da finalização da compra.

### B. Invariantes de Domínio e Bloqueio de Reatribuição

- **Pedido.setCliente(Cliente):** É idempotente para a mesma instância ou instâncias com mesmo ID. Qualquer tentativa de transferir um pedido existente para outro cliente de ID diferente lança `RegraNegocioException`. Um pedido pertence permanentemente ao cliente que o realizou.
- **ItemPedido.setPrato(Prato):** Permite a atribuição inicial (e hidratação pelo Hibernate), mas rejeita qualquer troca para outro prato de ID diferente.
- **Encapsulamento de Coleções:** As coleções `Categoria.getPratos()` e `Cliente.getPedidos()` retornam visualizações imutáveis (`Collections.unmodifiableList(...)`), impedindo adições ou remoções diretas que burlem a sincronização dos métodos de negócio (`trocarCategoria` e `adicionarPedido`).

### C. Ausência de Cascatas Destrutivas

- O relacionamento entre `Pedido` e `ItemPedido` é de **Composição**: utiliza `CascadeType.ALL` e `orphanRemoval = true`, pois o item não existe sem o pedido.
- Já os relacionamentos com entidades independentes (`Prato`, `Cliente`, `Categoria`) **não possuem `CascadeType.REMOVE` nem `orphanRemoval`**. Se alguém tentar excluir fisicamente uma categoria com pratos ou um prato com pedidos, o banco de dados rejeita a operação pela chave estrangeira.

---

## 6. Quadro de Rastreabilidade e Validação Técnica

| Pilar | Status no Projeto | Detalhamento Técnico |
|---|---|---|
| **O que foi implementado** | ✅ Concluído no código | Três relacionamentos JPA formalizados (`Prato ↔ Categoria`, `Pedido ↔ Cliente`, `ItemPedido → Prato`), snapshots históricos blindados, validações estritas de construtor e DTOs mantendo o contrato REST inalterado. |
| **O que foi testado no H2** | 🧪 Comprovado em execução | Suíte com **81 testes aprovados** (execução registrada via `mvnw.cmd test`), incluindo testes unitários de domínio e o teste de integração `IntegridadeReferencialPratoTest` que comprova o lançamento de `DataIntegrityViolationException` pelo H2 ao tentar deletar prato com pedidos. |
| **O que depende de validação no PostgreSQL** | 🐘 Pendente de teste em ambiente real | Verificação do DDL gerado pelo dialeto PostgreSQL (`fk_pedido_cliente`, `fk_item_pedido_prato`), validação de queries de auditoria para exclusão de registros órfãos antes da migração em bases existentes, e confirmação do tratamento de erro quando o Postgres aborta transações violadas. |
| **Recomendações de melhorias futuras** | 💡 Evolução planejada | Testes físicos de integridade de banco para `Categoria` e `Cliente` análogos ao de `Prato`, e instrumentação de contadores de queries SQL para medição empírica de consultas N+1 nas listagens. |
