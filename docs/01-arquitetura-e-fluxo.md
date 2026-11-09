# 🏛️ 01. Arquitetura em Camadas e Ciclo de Vida HTTP

> **Objetivo:** Compreender a divisão de responsabilidades no Spring Boot e o caminho exato que uma requisição percorre desde a internet até o banco de dados.

---

## 1. Por que usamos uma Arquitetura em Camadas?

Imagine um restaurante tradicional:
- O **Garçom** anota o pedido e entrega para a cozinha. Ele não cozinha o prato.
- O **Cozinheiro** prepara a receita seguindo regras rigorosas. Ele não atende a mesa nem busca insumos na fazenda.
- O **Almoxarife/Estoquista** cuida da despensa e busca os ingredientes.

Se o garçom resolvesse cozinhar ou o cozinheiro fosse cobrar a conta, o restaurante entraria em colapso.

No desenvolvimento de software com Spring Boot, aplicamos exatamente o mesmo princípio através do padrão **Separation of Concerns (Separação de Responsabilidades)**:

| Componente | Papel no Restaurante | Papel no Nutri Express |
|---|---|---|
| **Controller** | Garçom | Recebe a requisição HTTP, valida se os dados vieram no formato esperado e devolve a resposta final. |
| **Service** | Cozinheiro | Executa as regras de negócio: calcula valores, confere estoque, decide se a operação é permitida. |
| **Repository** | Almoxarife | Comunica-se diretamente com o banco de dados para salvar, consultar, atualizar ou deletar registros. |
| **Model / Entity** | Ingrediente / Prato real | Representa como a informação existe e é armazenada nas tabelas do banco de dados. |
| **DTO (Data Transfer Object)** | Cardápio / Comanda | Representa apenas o que o cliente pode ver ou enviar, protegendo o banco de dados real. |

---

## 2. Diagrama da Arquitetura do Nutri Express

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE HTTP                                    │
│                 (Navegador, Postman, Front-end React/Vue)                    │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │  1. HTTP POST /api/v1/pedidos + JWT
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         CAMADA DE SEGURANÇA (FILTROS)                        │
│                                                                              │
│  [ SecurityFilter (OncePerRequestFilter) ]                                   │
│  - Intercepta a requisição antes de chegar ao Controller                     │
│  - Extrai o cabeçalho Authorization: Bearer <token>                          │
│  - TokenService valida a assinatura HMAC-256 e a expiração                   │
│  - Define o usuário autenticado no SecurityContextHolder                     │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │  2. Requisição autorizada
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         CAMADA WEB (CONTROLLERS)                             │
│                                                                              │
│  [ PedidoController ]                                                        │
│  - Mapeia o endpoint (@PostMapping("/api/v1/pedidos"))                       │
│  - Jackson faz a desserialização do JSON para PedidoRequestDTO               │
│  - Bean Validation (@Valid) valida campos (@NotNull, @NotEmpty)              │
│  - Se houver erro de validação: aciona o GlobalExceptionHandler (HTTP 400)   │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │  3. Delega para a regra de negócio
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         CAMADA DE NEGÓCIO (SERVICES)                         │
│                                                                              │
│  [ PedidoService ] (@Transactional)                                          │
│  - Valida se o cliente existe e está ativo                                   │
│  - Para cada item: busca o prato no PratoRepository                          │
│  - Confere se o prato está disponível e se tem saldo em estoque              │
│  - Decrementa a quantidade do estoque do prato                               │
│  - Congela o preço unitário atual no ItemPedido (Snapshot de Preço)          │
│  - Calcula o subtotal dos itens e o valor total do pedido                    │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │  4. Executa persistência no banco
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         CAMADA DE PERSISTÊNCIA (REPOSITORIES)                │
│                                                                              │
│  [ PedidoRepository & PratoRepository ] (Spring Data JPA)                    │
│  - Traduz as chamadas de métodos Java para comandos SQL nativos              │
│  - Hibernate gerencia a sessão, estado das entidades e dirty-checking        │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │  5. SQL INSERT / UPDATE
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              BANCO DE DADOS                                  │
│                 H2 (em memória)  /  PostgreSQL (produção)                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Passo a Passo: O Ciclo de Vida de um Pedido

Para entender o que acontece por baixo dos panos no Spring Boot, vamos dissecar o que acontece no milissegundo em que você faz um `POST /api/v1/pedidos`:

### Passo 1: Chegada na Rede e Filtro JWT
1. A requisição HTTP chega ao servidor Tomcat embutido na porta `8080`.
2. A cadeia de filtros do Spring Security intercepta a chamada.
3. A nossa classe `SecurityFilter` lê o cabeçalho:
   `Authorization: Bearer eyJhbGciOiJIUz...`
4. Se o token for válido e não tiver expirado, o Spring reconhece o usuário como autenticado. Se o token estiver corrompido ou ausente em rotas protegidas, o Spring retorna **HTTP 403 Forbidden** imediatamente, sem gastar processamento com o Controller ou o banco de dados.

### Passo 2: O Controller e a Validação
1. O Spring localiza o método responsável: `PedidoController.criarPedido(...)`.
2. A biblioteca Jackson lê o JSON da requisição e cria uma instância do record `PedidoRequestDTO`.
3. O Bean Validation inspeciona as anotações do DTO:
   - Se a lista de itens estiver vazia (`@NotEmpty`), a execução é abortada e o `GlobalExceptionHandler` devolve **HTTP 400 Bad Request** com a lista de erros em JSON.
4. Se os dados forem válidos, o Controller chama `pedidoService.criarPedido(dto)`.

### Passo 3: O Service e a Atomicidade Transacional (`@Transactional`)
1. O método `criarPedido` está anotado com `@Transactional`. Isso abre uma **transação no banco de dados**.
2. O serviço busca o cliente:
   - Se o cliente não existir, lança `ResourceNotFoundException` (o `GlobalExceptionHandler` converte para **404 Not Found**).
   - Se o cliente estiver com `ativo = false`, lança `RegraNegocioException` (o `GlobalExceptionHandler` converte para **409 Conflict**).
3. O serviço percorre cada item solicitado:
   - Busca o prato correspondente.
   - Verifica: `prato.getQuantidadeEstoque() < itemDto.quantidade()`. Se faltar estoque, lança `RegraNegocioException("Estoque insuficiente")`.
   - **O milagre do `@Transactional`:** Quando a exceção é lançada, o Spring desfaz automaticamente (*rollback*) qualquer alteração que já tinha sido feita nos pratos anteriores! Nada fica inconsistente.
4. Se todos os itens tiverem saldo:
   - Cada prato tem seu estoque reduzido: `prato.decrementarEstoque(qtd)`.
   - Uma entidade `ItemPedido` é criada com o preço do prato **naquele instante**.
5. O pedido é salvo chamando `pedidoRepository.save(pedido)`.

### Passo 4: Retorno e DTO de Resposta
1. O Service devolve a entidade `Pedido` persistida com seu novo `id`.
2. O Controller converte a entidade para `PedidoResponseDTO`.
3. O Spring devolve o status **HTTP 201 Created** com o JSON do pedido criado no corpo da resposta.

---

## 4. Por que não colocar regras no Controller ou no Repository?

- **Controllers sem regra de negócio:** Se colocássemos o cálculo de estoque dentro do Controller, não conseguiríamos reaproveitar essa mesma lógica em outro lugar (por exemplo, se criássemos um consumidor de fila RabbitMQ ou um agendador de tarefas cron).
- **Services sem detalhes HTTP:** O Service não deve saber o que é `HttpServletRequest`, cabeçalho HTTP ou código de status. Ele trabalha apenas com objetos Java e regras puras. Isso torna o Service 100% testável com testes unitários rápidos.
- **Repositories sem regra de negócio:** Repositories apenas realizam operações I/O com o banco. Eles não decidem se uma pessoa pode ou não comprar.
