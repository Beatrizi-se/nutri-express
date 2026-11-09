# 🧪 04. Guia da Pirâmide de Testes Automatizados

> **Objetivo:** Compreender a estratégia de testes do Nutri Express, a diferença prática entre testes de unidade e testes de integração, e como utilizar Mockito e MockMvc.

---

## 1. O que é a Pirâmide de Testes?

A Pirâmide de Testes (conceito popularizado por Mike Cohn e Martin Fowler) estabelece uma diretriz de proporção para testes automatizados de software:

```
                  ▲
                 / \
                /   \        Testes End-to-End (E2E)
               / E2E \       (Testam a aplicação inteira com navegador real;
              /───────\       são lentos e frágeis)
             /         \
            /Integração \    Testes de Integração (MockMvc / WebMvcTest)
           /  (MockMvc)  \   (Testam a camada web, rotas HTTP, JSON e validação)
          /───────────────\
         /     Testes      \  Testes Unitários (JUnit 5 + Mockito)
        /    Unitários      \ (Testam métodos Java isolados em milissegundos;
       /    dos Services     \(base sólida, rápidos e de alta cobertura)
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

## 4. Inventário dos 70 Testes do Nutri Express

| Suíte de Testes | Tipo de Teste | Quantidade | O que é validado |
|---|---|---|---|
| **`CategoriaServiceTest`** | Unitário (Mockito) | 9 | CRUD de categorias, duplicidade de nome, verificação de ID inexistente |
| **`PratoServiceTest`** | Unitário (Mockito) | 9 | CRUD de pratos, filtros nutricionais, atualização de preço e estoque |
| **`ClienteServiceTest`** | Unitário (Mockito) | 8 | Soft delete, reativação, unicidade de CPF e email |
| **`PedidoServiceTest`** | Unitário (Mockito) | 8 | Checkout, baixa atômica de estoque, cancelamento com estorno e transições de status |
| **`TokenServiceTest`** | Unitário | 3 | Geração de token JWT, validação de assinatura HMAC-256 e rejeição de token adulterado |
| **`CategoriaControllerTest`** | Integração (MockMvc) | 6 | Endpoints `/api/v1/categorias`: 200, 201, 204, 400 e 404 |
| **`AutenticacaoControllerTest`** | Integração (MockMvc) | 5 | Endpoints `/auth`: login com token JWT, registro com hash BCrypt, conflito 409 e validações 400 |
| **`ClienteControllerTest`** | Integração (MockMvc) | 7 | Endpoints `/api/v1/clientes`: validação regex de CEP e telefone, dataNascimento no passado, soft delete |
| **`PratoControllerTest`** | Integração (MockMvc) | 8 | Endpoints `/api/v1/pratos`: filtros, validação de preço positivo, patch de estoque e preço |
| **`PedidoControllerTest`** | Integração (MockMvc) | 7 | Endpoints `/api/v1/pedidos`: criação com itens, validação de lista vazia, cancelamento e status |
| **TOTAL** | | **70 Testes** | **100% de Aprovação no Maven (`BUILD SUCCESS`)** |

---

## 5. Como Executar os Testes no Terminal

Executar **todos os 70 testes** do projeto:
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
