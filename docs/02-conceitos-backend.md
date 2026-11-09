# 📖 02. Dicionário de Conceitos Backend & Spring Boot

> **Guia de Consulta para Estudantes de Ciência da Computação e Engenharia de Software**  
> Este documento explica os conceitos teóricos fundamentais aplicados no projeto Nutri Express, acompanhados de exemplos reais do código-fonte.

---

## 1. Java Records e DTOs

### O que é um DTO (Data Transfer Object)?
Um **DTO** é um objeto cujo único propósito é transportar dados entre camadas ou pela rede (cliente HTTP ↔ API).

### Por que NUNCA devemos expor a Entidade JPA diretamente no Controller?
1. **Segurança (Over-posting Attack):** Se você receber a entidade `Usuario` diretamente no `@RequestBody`, um invasor pode enviar no JSON `"role": "ADMIN"` e `"ativo": true`, forjando privilégios.
2. **Vazamento de Dados Sensíveis:** Ao retornar a entidade `Usuario`, o campo `senha` (mesmo em hash) seria enviado no JSON.
3. **Loop Infinito de Serialização (Recursão Jackson):** Em relacionamentos `@OneToMany` bidirecionais (ex: Pedido ↔ ItemPedido), serializar a entidade crua faz o Jackson entrar em loop infinito e estourar a memória (`StackOverflowError`).
4. **Acoplamento do Banco com a API:** Se você renomear uma coluna no banco de dados, você quebraria todos os aplicativos front-end e mobile que consomem a API. O DTO atua como uma barreira de proteção.

### Por que usamos Java Records para os DTOs?
Introduzidos de forma definitiva no Java 16, os **Records** são classes especiais voltadas exclusivamente para dados imutáveis.

#### Comparação: Classe Tradicional vs. Java Record

**Abordagem Antiga (Mais de 30 linhas de boilerplate):**
```java
public class CategoriaResponseDTO {
    private final Long id;
    private final String nome;
    private final String descricao;

    public CategoriaResponseDTO(Long id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    // + equals, hashCode e toString...
}
```

**Abordagem Moderna no Nutri Express (1 linha):**
```java
public record CategoriaResponseDTO(Long id, String nome, String descricao) {
    public CategoriaResponseDTO(Categoria categoria) {
        this(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}
```

> ⚠️ **Atenção ao acessar dados de um Record:**  
> Records **não** utilizam o prefixo `get`. O método gerado tem o mesmo nome do atributo:  
> - Em vez de `dto.getNome()`, você chama **`dto.nome()`**.  
> - Em vez de `dto.getPreco()`, você chama **`dto.preco()`**.

---

## 2. Injeção de Dependências por Construtor

### O que é Inversão de Controle (IoC)?
Em Java puro, quando uma classe precisa de outra, ela faz:
```java
private CategoriaRepository repository = new CategoriaRepositoryImpl();
```
Com o Spring Boot, quem instancia e gerencia as classes é o **Contêiner do Spring**.

### Por que NÃO usar `@Autowired` diretamente nos campos?
Você frequentemente verá tutoriais antigos fazendo:
```java
@Autowired
private CategoriaRepository repository; // NÃO RECOMENDADO!
```

**Problemas do `@Autowired` em campo:**
1. **Oculta dependências:** É impossível saber à primeira vista o que a classe precisa para funcionar.
2. **Dificulta Testes Unitários:** Para testar a classe sem o Spring, você é obrigado a usar *reflection* para injetar os mocks.
3. **Não permite campos `final`:** O campo precisa ser mutável para o Spring injetá-lo depois que o objeto for instanciado.

### O Padrão Recomendado (e adotado no Nutri Express):
Injeção explícita via **Construtor com campos `final`**:
```java
@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    // Dependência explícita, imutável e fácil de testar com 'new CategoriaService(mockRepo)'
    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }
}
```

---

## 3. Bean Validation (Validação Declarativa)

Em vez de encher seus Services com dezenas de `if (nome == null || nome.isEmpty())`, usamos o **Jakarta Bean Validation**.

As anotações são colocadas diretamente nos atributos do DTO de requisição:

| Anotação | O que valida | Exemplo no Nutri Express |
|---|---|---|
| `@NotBlank` | Garante que o texto não é nulo e não contém apenas espaços | `@NotBlank(message = "O nome é obrigatório") String nome` |
| `@Email` | Garante que a string possui formato válido de endereço de email | `@Email(message = "Formato de e-mail inválido") String email` |
| `@Past` | Garante que uma data é anterior à data de hoje | `@Past(message = "Data de nascimento deve ser no passado") LocalDate dataNascimento` |
| `@Positive` | Garante que um número é estritamente maior que zero | `@Positive(message = "O preço deve ser maior que zero") Double preco` |
| `@Pattern` | Valida uma Expressão Regular (Regex) customizada | Telefone: `^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$`<br>CEP: `^\\d{5}-?\\d{3}$` |
| `@Valid` | Aciona a validação recursiva de um DTO aninhado | `@NotNull @Valid EnderecoDTO endereco` |

No Controller, para acionar essa validação antes que o método execute, colocamos a anotação **`@Valid`** antes do `@RequestBody`:
```java
@PostMapping
public ResponseEntity<ClienteResponseDTO> criarCliente(@Valid @RequestBody ClienteRequestDTO dto) { ... }
```

---

## 4. Tratamento Global de Exceções (`@RestControllerAdvice`)

Quando uma validação falha ou um registro não é encontrado, o Spring não deve devolver a tela de erro padrão com rastreamento de pilha (*stack trace*) feio e inseguro.

Usamos a classe `GlobalExceptionHandler` anotada com `@RestControllerAdvice`:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", 404);
        error.put("error", "Não Encontrado");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

**Resultado:** Se o Service lançar `throw new ResourceNotFoundException("Prato não encontrado")`, o cliente recebe automaticamente um JSON limpo com status HTTP 404:
```json
{
  "timestamp": "2026-11-09T15:00:05",
  "status": 404,
  "error": "Não Encontrado",
  "message": "Prato não encontrado com o ID: 99"
}
```

---

## 5. Criptografia de Senhas com BCrypt

### Por que nunca salvar senhas em texto puro?
Se o banco de dados for comprometido por um vazamento, as senhas de todos os usuários estariam expostas.

### O que é o algoritmo BCrypt?
O BCrypt é uma função de derivação de chave baseada na cifra Blowfish que possui duas características vitais:
1. **É Unidirecional (Hash):** É matematicamente impossível converter o hash de volta para a senha original.
2. **Possui Salt Aleatório Embutido:** Toda vez que você criptografa a mesma senha, o resultado gerado é completamente diferente!

Exemplo:
- Criptografando `"admin123"` hoje: `$2a$10$wKkY7v3.0bX...`
- Criptografando `"admin123"` de novo: `$2a$10$9qZ8M2.1kR...`

Isso impede os chamados **Rainbow Table Attacks** (ataques baseados em listas pré-computadas de senhas comuns).

No Spring Security, usamos o `BCryptPasswordEncoder`:
```java
// No cadastro:
String senhaCriptografada = passwordEncoder.encode(dto.senha());

// No login (o Spring faz internamente):
passwordEncoder.matches(senhaDigitada, hashDoBanco);
```

---

## 6. Spring Data JPA e Consultas Derivadas (Query Methods)

Uma das maiores forças do Spring Data JPA é a capacidade de criar consultas SQL complexas apenas pelo nome do método na interface!

Veja os exemplos reais do `ClienteRepository`:

- `findByEmail(String email)` -> Gera: `SELECT * FROM cliente WHERE email = ?`
- `findByAtivoTrue()` -> Gera: `SELECT * FROM cliente WHERE ativo = true`
- `findByNomeContainingIgnoreCase(String nome)` -> Gera: `SELECT * FROM cliente WHERE UPPER(nome) LIKE UPPER('%nome%')`
- `existsByCpfAndIdNot(String cpf, Long id)` -> Gera: `SELECT COUNT(*) > 0 FROM cliente WHERE cpf = ? AND id <> ?` (perfeito para validar duplicidade na atualização sem acusar o próprio registro!)
