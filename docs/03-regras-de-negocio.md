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

    // Estorna as quantidades de volta para o prato
    for (ItemPedido item : pedido.getItens()) {
        Prato prato = pratoRepository.findById(item.getPratoId()).orElse(null);
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
Na entidade `ItemPedido.java`, criamos o campo `precoUnitario`:
```java
public ItemPedido(Long pratoId, String pratoNome, Integer quantidade, Double precoUnitario) {
    this.pratoId = pratoId;
    this.pratoNome = pratoNome;
    this.quantidade = quantidade;
    this.precoUnitario = precoUnitario; // PREÇO CONGELADO NO INSTANTE DA VENDA
    this.subtotal = precoUnitario * quantidade;
}
```

Dessa forma:
- O `Prato` no cardápio pode mudar de preço amanhã, semana que vem ou no ano que vem.
- O `ItemPedido` registrado preserva eternamente o preço exato pelo qual ele foi comercializado naquele dia.

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
