# 🧪 04. Guia da Pirâmide de Testes Automatizados

> **Objetivo:** Compreender a estratégia de testes do Nutri Express, a diferença prática entre testes de unidade e testes de integração, e como utilizar Mockito e MockMvc.

---

## 1. O que é a Pirâmide de Testes?

A Pirâmide de Testes (conceito popularizado por Mike Cohn e Martin Fowler) estabelece uma diretriz de proporção para testes automatizados de software:

```
                  ▲
                 / \
                /   \        1 Teste de Banco e Integridade Referencial
               /     \       (@SpringBootTest + H2: teste físico de Foreign Key)
              /───────\
             /         \     33 Testes de Integração de Controllers
            /Integração \    (MockMvc / WebMvcTest: rotas HTTP, status e validação)
           /  (MockMvc)  \
          /───────────────\
         /     Testes      \  47 Testes Unitários de Serviços e Domínio
        /    Unitários      \ (JUnit 5 + Mockito: cálculos, estoque, invariantes,
       /    dos Services     \ snapshots e sincronização de relacionamentos)
      └───────────────────────┘
```

---

## 2. Testes Unitários de Serviços (JUnit 5 + Mockito)

### O que testamos aqui?
Testamos **regras de negócio puras** da camada `Service` sem encostar no banco de dados e sem subir o Spring.

### O que é um "Mock"?
Um **Mock** é um objeto "dublê" ou simulado. Se a classe `CategoriaService` precisa de `CategoriaRepository`, não queremos que o teste faça uma consulta SQL de verdade no disco. Criamos um mock que finge ser o repositório.

### As Principais Anotações do Mockito:

```java
@ExtendWith(MockitoExtension.class) // Inicializa os mocks sem subir o Spring
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repository; // Nosso dublê simulado

    @InjectMocks
    private CategoriaService service; // Classe real sendo testada com o dublê injetado
}
```

### Anatomia de um Teste Unitário (Padrão AAA: Arrange, Act, Assert):

```java
@Test
@DisplayName("Deve buscar categoria por ID quando existir")
void getCategoriaById_QuandoExiste_DeveRetornarCategoria() {
    // 1. ARRANGE (Preparação e Mock)
    Categoria categoriaMock = new Categoria(1L, "Saladas", "Pratos leves");
    when(repository.findById(1L)).thenReturn(Optional.of(categoriaMock));

    // 2. ACT (Ação sendo testada)
    Categoria resultado = service.getCategoriaById(1L);

    // 3. ASSERT (Validações)
    assertNotNull(resultado);
    assertEquals("Saladas", resultado.getNome());
    verify(repository, times(1)).findById(1L); // Garante que o método foi chamado exatamente 1 vez
}
```

---

## 3. Testes de Integração de Controllers (MockMvc & `@WebMvcTest`)

### O que testamos aqui?
O teste unitário do Service não sabe se a rota é `/api/v1/categorias` ou se um JSON malformado é rejeitado.
É aqui que entra o **MockMvc**: ele simula o envio de requisições HTTP reais diretamente contra o despachante do Spring (`DispatcherServlet`) na memória, sem precisar ligar a placa de rede ou o servidor Tomcat.

### Exemplo Real: Validando o Bean Validation (HTTP 400):

```java
@Test
@DisplayName("POST /api/v1/categorias - Com nome em branco deve barrar no Bean Validation e retornar 400 Bad Request")
void criarCategoria_ComNomeEmBranco_DeveRetornar400() throws Exception {
    String jsonInvalido = """
        {
            "nome": "",
            "descricao": "Descricao qualquer"
        }
        """;

    mockMvc.perform(post("/api/v1/categorias")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonInvalido))
            .andExpect(status().isBadRequest())              // Garante código HTTP 400
            .andExpect(jsonPath("$.status").value(400))       // Garante campo status no JSON
            .andExpect(jsonPath("$.messages.nome").exists()); // Garante que o erro do campo nome foi reportado
}
```

### O que é o `jsonPath`?
O `jsonPath` é uma sintaxe de consulta para inspecionar árvores JSON retornadas na resposta:
- `$.id` -> Busca a propriedade `id` na raiz do JSON.
- `$.status` -> Busca o código de status HTTP na raiz do JSON.
- `$[0].nome` -> Se a resposta for uma lista `[]`, busca o `nome` do primeiro elemento.
- `$.messages['endereco.cep']` -> Busca o erro do campo aninhado `cep` do objeto `endereco`.

---

## 4. Testes de Integridade Referencial no Banco de Dados (`@SpringBootTest`)

### Por que Mocks não bastam para testar Chaves Estrangeiras?
Testes com Mockito simulam métodos Java (`when(repository.deleteById(...))`), mas **não executam comandos SQL reais**. Eles são incapazes de verificar se o banco de dados relacional realmente bloqueará a exclusão de um registro referenciado por uma `FOREIGN KEY`.

### A Solução em `IntegridadeReferencialPratoTest.java`:
Para comprovar que a regra de negócio e a restrição de banco `fk_item_pedido_prato` protegem o histórico:
1. Subimos o contexto Spring com `@SpringBootTest`.
2. Criamos e persistimos uma categoria, um prato e um pedido com seu respectivo `ItemPedido`.
3. Executamos uma tentativa de exclusão do prato (`pratoRepository.deleteById(pratoId)`) isolada em uma transação com `TransactionTemplate` forçando o `flush()` para o banco.
4. O H2 intercepta a violação da FK e lança `DataIntegrityViolationException`.
5. Em uma transação independente subsequente, confirmamos que o prato, o pedido e o item continuam **100% preservados e íntegros**.

---

## 5. Inventário dos 81 Testes Registrados do Nutri Express

> [!NOTE]
> A contagem de **81 testes aprovados** reflete o resultado comprovado da execução registrada na suíte automatizada do projeto, atestando a cobertura dos fluxos de negócio, segurança e relacionamentos da Aula 12.

| Suíte de Testes | Tipo de Teste | Quantidade | O que é validado |
|---|---|---|---|
| **`CategoriaServiceTest`** | Unitário (Mockito) | 12 | CRUD, duplicidade de nome, amarração bidirecional com pratos e proteção de coleção imutável |
| **`PratoServiceTest`** | Unitário (Mockito) | 11 | CRUD de pratos, filtros nutricionais, estoque, vínculo de categoria e troca controlada |
| **`ClienteServiceTest`** | Unitário (Mockito) | 8 | Soft delete, reativação, unicidade de CPF e email |
| **`PedidoServiceTest`** | Unitário (Mockito) | 13 | Checkout, baixa/estorno de estoque, associação com cliente, bloqueio de transferência, validações de ItemPedido e imutabilidade de snapshot |
| **`TokenServiceTest`** | Unitário | 3 | Geração de token JWT, validação de assinatura HMAC-256 e rejeição de token adulterado |
| **`CategoriaControllerTest`** | Integração (MockMvc) | 6 | Endpoints `/api/v1/categorias`: 200, 201, 204, 400 e 404 |
| **`AutenticacaoControllerTest`** | Integração (MockMvc) | 5 | Endpoints `/auth`: login com token JWT, registro com hash BCrypt, conflito 409 e validações 400 |
| **`ClienteControllerTest`** | Integração (MockMvc) | 7 | Endpoints `/api/v1/clientes`: validação regex de CEP e telefone, dataNascimento no passado, soft delete |
| **`PratoControllerTest`** | Integração (MockMvc) | 8 | Endpoints `/api/v1/pratos`: filtros, validação de preço positivo, patch de estoque e preço |
| **`PedidoControllerTest`** | Integração (MockMvc) | 7 | Endpoints `/api/v1/pedidos`: criação com itens, validação de lista vazia, cancelamento e status |
| **`IntegridadeReferencialPratoTest`** | Integração (H2 DB) | 1 | Proteção física de integridade referencial: bloqueio de deleção de prato vinculado a pedido via `fk_item_pedido_prato` |
| **TOTAL REGISTRADO** | | **81 Testes** | **100% de Aprovação no Maven (`BUILD SUCCESS`)** |

---

## 6. Escopo de Validação, Limitações e Próximos Passos

Para manter o rigor de engenharia de software e a clareza didática, a documentação distingue explicitamente os níveis de validação:

* **✅ Implementado e Comprovado:**
  - Sincronização em memória e encapsulamento dos relacionamentos bidirecionais.
  - Imutabilidade e validação estrita de snapshots históricos (`clienteNome`, `enderecoEntrega`, `pratoNome`, `precoUnitario`, `subtotal`).
  - Integridade referencial física comprovada em banco de dados H2 via `IntegridadeReferencialPratoTest`.
  - 81 testes passando com zero falhas no Maven.
* **🐘 Pendente de Validação no PostgreSQL:**
  - Execução dos testes e validação das constraints físicas em uma instância real de PostgreSQL (em vez de banco em memória H2).
  - Verificação de scripts de migração estrutural para dados pré-existentes.
* **💡 Melhorias Futuras Recomendadas:**
  - Criação de testes de integridade referencial em banco para `Categoria` e `Cliente` (análogos ao de `Prato`).
  - Instrumentação de contadores formais de queries SQL (ex.: `datasource-proxy`) para medir empiricamente a ausência de consultas N+1 nas listagens.

---

## 7. Como Executar os Testes no Terminal

Executar **todos os 81 testes** do projeto:
```powershell
.\mvnw.cmd test
```

Executar apenas os **testes unitários dos Services**:
```powershell
.\mvnw.cmd test "-Dtest=*ServiceTest,TokenServiceTest"
```

Executar apenas os **testes de integração dos Controllers**:
```powershell
.\mvnw.cmd test "-Dtest=*ControllerTest"
```

Executar apenas o **teste de integridade referencial do banco**:
```powershell
.\mvnw.cmd test "-Dtest=IntegridadeReferencialPratoTest"
```
