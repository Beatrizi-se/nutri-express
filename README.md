# 🍽️ Nutri Express

> **API RESTful completa para delivery de refeições saudáveis, gestão de cardápio, pedidos, controle transacional de estoque e segurança com JWT.**

Desenvolvido com **Java 25** e **Spring Boot 4**, o **Nutri Express** foi concebido como uma aplicação prática e robusta de backend, aplicando os principais padrões de arquitetura de software, persistência com Spring Data JPA, validações com Bean Validation, segurança stateless com Spring Security e uma pirâmide completa de testes automatizados (unitários com Mockito e de integração com MockMvc).

---

## 📌 Sumário

- [Visão Geral e Destaques](#-visão-geral-e-destaques)
- [Tecnologias e Bibliotecas](#-tecnologias-e-bibliotecas)
- [Arquitetura e Separação de Camadas](#-arquitetura-e-separação-de-camadas)
- [Pré-requisitos e Como Executar](#-pré-requisitos-e-como-executar)
- [Acesso ao Swagger UI e Autenticação JWT](#-acesso-ao-swagger-ui-e-autenticação-jwt)
- [Console do Banco de Dados H2](#-console-do-banco-de-dados-h2)
- [Endpoints Principais da API](#-endpoints-principais-da-api)
- [Pirâmide de Testes Automatizados](#-pirâmide-de-testes-automatizados)
- [Status do Desenvolvimento](#-status-do-desenvolvimento)
- [Guias Educativos Aprofundados (Pasta docs/)](#-guias-educativos-aprofundados-pasta-docs)

---

## 💡 Visão Geral e Destaques

O projeto resolve o fluxo operacional completo de um restaurante saudável:
1. **Cardápio Inteligente:** Categorias e pratos com filtros nutricionais (calorias máximas, disponibilidade e busca por nome).
2. **Clientes e Soft Delete:** Gestão de clientes com validação de CPF único, regex de telefone e CEP, e inativação lógica (*soft delete*) para preservar o histórico contábil.
3. **Pedidos com Snapshot de Preço:** Na criação do item do pedido, o preço unitário do prato é congelado, impedindo que alterações futuras no cardápio alterem retroativamente o histórico de vendas.
4. **Controle Atômico de Estoque:** Operações de pedido utilizam `@Transactional`. Se um item não tiver saldo suficiente, a transação sofre *rollback* automático. No cancelamento do pedido, o estoque é estornado.
5. **Segurança Stateless com JWT:** Senhas criptografadas com hash **BCrypt**, tokens assinados via **HMAC-256** e controle de permissões por papéis (`ROLE_ADMIN` e `ROLE_CLIENTE`).

---

## 🛠️ Tecnologias e Bibliotecas

| Tecnologia / Lib | Versão / Padrão | Função no Nutri Express |
|---|---|---|
| **Java** | 25 | Linguagem base, utilizando recursos modernos como **Java Records** para DTOs imutáveis |
| **Spring Boot** | 4.1.0 | Framework central da aplicação (Inversão de Controle e Injeção de Dependências) |
| **Spring Web MVC** | 4.x | Construção dos controllers REST, roteamento e serialização JSON |
| **Spring Security** | 6.x | Camada de segurança stateless, autenticação e autorização baseada em Roles |
| **Auth0 Java JWT** | 4.5.0 | Geração, assinatura criptográfica (HMAC-256) e validação de tokens JWT |
| **Spring Data JPA / Hibernate** | 7.x | Mapeamento Objeto-Relacional (ORM) e abstração de consultas no banco |
| **Bean Validation** | Jakarta Validation | Validação declarativa de entrada (`@NotBlank`, `@Email`, `@Past`, `@Pattern`) |
| **H2 Database** | Em memória | Banco de dados relacional rápido para desenvolvimento e execução de testes |
| **PostgreSQL Driver** | Runtime | Driver oficial pronto para conexão em ambiente de homologação e produção |
| **SpringDoc OpenAPI** | 2.8.5 | Geração automática da documentação Swagger UI interativa com suporte a Bearer Token |
| **JUnit 5 & Mockito** | Modern | Execução de testes unitários isolados com mocks |
| **Spring MockMvc** | WebMvcTest | Testes de integração na camada HTTP sem necessidade de servidor real |

---

## 🏗️ Arquitetura e Separação de Camadas

A aplicação segue uma arquitetura em camadas bem definida, garantindo baixo acoplamento e alta coesão:

```
[ Cliente HTTP (Postman / Swagger / Front-end) ]
                      │
                      ▼
             [ SecurityFilter ]  ──► Valida Token JWT no cabeçalho Authorization
                      │
                      ▼
               [ Controller ]    ──► Recebe requisições HTTP, valida DTOs (@Valid)
                      │
                      ▼
                [ Service ]      ──► Implementa regras de negócio e transações (@Transactional)
                      │
                      ▼
              [ Repository ]     ──► Spring Data JPA realiza persistência e queries
                      │
                      ▼
           [ Banco de Dados ]    ──► H2 (perfil dev) ou PostgreSQL (perfil postgres)
```

### Papel de cada pasta no projeto:

- `config/`: Configurações de infraestrutura (Swagger OpenAPI, Segurança Spring Security, Seed inicial).
- `controller/`: Portas de entrada HTTP da aplicação. Não contêm regras de negócio.
- `dto/`: Objetos de Transferência de Dados (**Java Records**) divididos por contexto (`auth`, `categoria`, `cliente`, `pedido`, `prato`).
- `model/`: Entidades persistíveis pelo JPA com mapeamento de tabelas e colunas.
- `repository/`: Interfaces que estendem `JpaRepository`, fornecendo operações CRUD e consultas derivadas.
- `service/`: O "coração" da aplicação. Onde as regras de negócio e transações acontecem.
- `exception/`: `@RestControllerAdvice` central que intercepta exceções e formata respostas de erro (400, 404, 409).

---

## 🚀 Pré-requisitos e Como Executar

### Pré-requisitos:
- **Java 25** (ou JDK 21+ compatível) instalado e configurado no `PATH`.
- Git instalado.

### 1. Clonar o repositório:
```bash
git clone https://github.com/seu-usuario/nutri-express.git
cd nutri-express
```

### 2. Executar a aplicação:

No **Windows (PowerShell ou Prompt de Comando)**:
```powershell
.\mvnw.cmd spring-boot:run
```

No **Linux ou macOS**:
```bash
./mvnw spring-boot:run
```

A aplicação subirá na porta padrão **8080**.

---

## 🔐 Acesso ao Swagger UI e Autenticação JWT

A documentação interativa da API está disponível no navegador assim que o projeto estiver rodando:

👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Como se autenticar no Swagger (Passo a Passo):

Por padrão, ao iniciar o projeto, um usuário administrador é criado automaticamente pelo `LoadDatabase`:
- **Login:** `admin`
- **Senha:** `admin123`

1. No Swagger UI, abra o endpoint **`POST /auth/login`**.
2. Clique em **Try it out** e envie o payload:
   ```json
   {
     "login": "admin",
     "senha": "admin123"
   }
   ```
3. Clique em **Execute**. Você receberá a resposta `200 OK` contendo o token:
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "tipo": "Bearer",
     "login": "admin",
     "role": "ADMIN"
   }
   ```
4. Copie o valor do campo `"token"`.
5. No topo da página do Swagger, clique no botão verde **Authorize 🔓**.
6. No campo **Value**, cole apenas o token (o prefixo `Bearer` já é adicionado automaticamente pelo Swagger).
7. Clique em **Authorize** e feche a janela. Pronto! O cadeado ficará fechado 🔒 e todas as suas requisições enviarão o token JWT.

---

## 🗄️ Console do Banco de Dados H2

Para visualizar as tabelas criadas pelo Hibernate e executar consultas SQL em tempo real:

👉 **[http://localhost:8080/h2-console](http://localhost:8080/h2-console)**

Preencha os campos exatamente assim:
- **JDBC URL:** `jdbc:h2:mem:nutridb`
- **User Name:** `sa`
- **Password:** *(deixe em branco)*
- Clique em **Connect**.

---

## 📡 Endpoints Principais da API

| Módulo | Método | Rota | Acesso | Descrição |
|---|---|---|---|---|
| **Auth** | `POST` | `/auth/login` | Público | Autentica usuário e emite Token JWT |
| **Auth** | `POST` | `/auth/registrar` | Público | Registra novo usuário com hash BCrypt |
| **Categorias** | `GET` | `/api/v1/categorias` | Público | Lista todas as categorias do cardápio |
| **Categorias** | `POST` | `/api/v1/categorias` | `ROLE_ADMIN` | Cria nova categoria |
| **Pratos** | `GET` | `/api/v1/pratos` | Público | Consulta pratos com filtros (calorias, categoria) |
| **Pratos** | `POST` | `/api/v1/pratos` | `ROLE_ADMIN` | Cadastra novo prato |
| **Pratos** | `PATCH`| `/api/v1/pratos/{id}/preco` | `ROLE_ADMIN` | Atualiza especificamente o preço do prato |
| **Pratos** | `PATCH`| `/api/v1/pratos/{id}/estoque`| `ROLE_ADMIN` | Ajusta a quantidade em estoque |
| **Clientes** | `POST` | `/api/v1/clientes` | Autenticado | Cadastra cliente com validação de CEP e telefone |
| **Clientes** | `DELETE`| `/api/v1/clientes/{id}` | Autenticado | Inativa cliente (*Soft Delete*) |
| **Clientes** | `PATCH`| `/api/v1/clientes/{id}/reativar` | Autenticado | Reativa cadastro de cliente inativo |
| **Pedidos** | `POST` | `/api/v1/pedidos` | Autenticado | Cria pedido, congela preço e dá baixa no estoque |
| **Pedidos** | `PATCH`| `/api/v1/pedidos/{id}/cancelar` | Autenticado | Cancela pedido e estorna quantidade ao estoque |
| **Pedidos** | `PATCH`| `/api/v1/pedidos/{id}/status` | Autenticado | Atualiza status (`EM_PREPARO`, `ENTREGUE`, etc.) |

---

## 🧪 Pirâmide de Testes Automatizados

O Nutri Express possui **70 testes automatizados**, cobrindo 100% dos fluxos de negócio e da camada HTTP com **zero falhas**:

```
               ▲
              / \
             /   \     33 Testes de Integração de Controllers
            / MVC \    (@WebMvcTest + MockMvc: rotas, códigos de status,
           /───────\   Bean Validation, serialização JSON e filtros de erro)
          /         \
         /  SERVICES \  37 Testes Unitários de Regras de Negócio
        /─────────────\ (JUnit 5 + Mockito: cálculos, estoque, soft delete e JWT)
```

Para executar toda a suíte de testes:
```powershell
.\mvnw.cmd test
```

Para executar apenas os testes de integração dos Controllers:
```powershell
.\mvnw.cmd test "-Dtest=*ControllerTest"
```

---

## 📊 Status do Desenvolvimento

| Etapa | Módulo | Status | Evidências no Código |
|---|---|---|---|
| **Etapa 1** | Categorias | ✅ Concluída | CRUD completo, Records, validação de duplicidade e 9 testes unitários |
| **Etapa 2** | Pratos | ✅ Concluída | Filtros nutricionais, preço, estoque integrado e 9 testes unitários |
| **Etapa 3** | Banco de dados | ✅ Concluída | Driver PostgreSQL no `pom.xml`, perfis `dev` (H2) e `postgres` |
| **Etapa 4** | Clientes e endereços | ✅ Concluída | `dataNascimento`, regex de CEP e telefone, soft delete e 8 testes unitários |
| **Etapa 5** | Pedidos | ✅ Concluída | Snapshot de preço em `ItemPedido`, cálculo de totais e 8 testes unitários |
| **Etapa 6** | Estoque e transações | ✅ Concluída | Baixa no checkout, bloqueio por saldo e estorno no cancelamento via `@Transactional` |
| **Etapa 7** | Autenticação e usuários | ✅ Concluída | Spring Security Stateless, tokens JWT (HMAC-256) e senhas em BCrypt |
| **Etapa 8** | Testes e documentação | ✅ Concluída | 70 testes automatizados (Mockito + MockMvc) e documentação Swagger interativa |

---

## 📖 Guias Educativos Aprofundados (Pasta `docs/`)

Para estudantes e desenvolvedores que desejam se aprofundar nos conceitos teóricos e decisões arquiteturais aplicadas neste projeto, consulte nossos guias dedicados:

- 📑 **[01. Arquitetura em Camadas e Ciclo de Vida HTTP](docs/01-arquitetura-e-fluxo.md)**: Como uma requisição trafega desde a internet até o banco de dados.
- 📑 **[02. Dicionário de Conceitos Backend](docs/02-conceitos-backend.md)**: Records, DTO vs. Entity, Injeção de Dependências, BCrypt e Bean Validation explicados com exemplos reais.
- 📑 **[03. Regras de Negócio e Transações](docs/03-regras-de-negocio.md)**: Máquina de estados do pedido, controle atômico de estoque e congelamento de preço.
- 📑 **[04. Guia da Pirâmide de Testes](docs/04-guia-testes.md)**: Diferença prática entre Mockito e MockMvc, e como testar controllers de forma isolada.

---

## 👩‍💻 Autoria e Aprendizado

Projeto desenvolvido e aprimorado por **Ana Beatriz Sotero Oliveira**, como parte da minha jornada acadêmica em Ciência da Computação e do aprofundamento em desenvolvimento backend com Java e Spring Boot.

O Nutri Express teve como ponto de partida um projeto-base disponibilizado pelo meu professor de Back-end em contexto acadêmico. A partir dessa base, desenvolvi minha própria versão, realizando estudos, implementações, melhorias estruturais, testes e documentação técnica.

Este repositório representa meu processo de aprendizado e evolução prática, com foco em arquitetura backend, APIs REST, persistência de dados, regras de negócio e boas práticas de desenvolvimento.
