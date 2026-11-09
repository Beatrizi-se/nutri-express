# 🍽️ Nutri Express

API REST para gerenciamento de cardápio, clientes, pedidos e estoque.

O Nutri Express é uma aplicação backend desenvolvida com Java e Spring Boot, com o objetivo de gerenciar as principais operações de um sistema de pedidos de refeições.

O projeto foi desenvolvido como uma oportunidade de aprofundamento prático em desenvolvimento backend, aplicando conceitos de arquitetura em camadas, construção de APIs REST, persistência de dados, validação, tratamento de exceções e gerenciamento de transações.

Atualmente, a aplicação contempla funcionalidades de gerenciamento de categorias, pratos, clientes, pedidos e controle de estoque.

---

## 📌 Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Arquitetura do projeto](#arquitetura-do-projeto)
- [Estrutura de diretórios](#estrutura-de-diretórios)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Documentação da API](#documentação-da-api)
- [Status do desenvolvimento](#status-do-desenvolvimento)
- [Próximos passos](#próximos-passos)
- [Aprendizados](#aprendizados)

---

## 💡 Sobre o projeto

O Nutri Express foi pensado como uma API para dar suporte às operações de um sistema de alimentação.

Sua proposta é organizar os dados e as regras de negócio relacionadas ao cardápio, aos clientes e aos pedidos, mantendo as responsabilidades separadas entre as diferentes camadas da aplicação.

Além de atender aos requisitos funcionais, o projeto busca aplicar boas práticas de desenvolvimento, priorizando:

- Separação de responsabilidades.
- Código organizado e de fácil manutenção.
- Validação de dados de entrada.
- Tratamento centralizado de exceções.
- Integridade das operações.
- Testabilidade das regras de negócio.

---

## 🚀 Funcionalidades

### Categorias

- Cadastro de categorias.
- Consulta de todas as categorias.
- Consulta de categoria por identificador.
- Atualização de categorias.
- Exclusão de categorias.
- Verificação de nomes duplicados.

### Pratos

- Cadastro de pratos.
- Consulta de pratos.
- Consulta por identificador.
- Atualização de informações.
- Exclusão de pratos.
- Consulta por categoria.
- Filtros por nome e quantidade máxima de calorias.
- Atualização específica de preço.
- Controle de disponibilidade e quantidade em estoque.

### Clientes e endereços

- Cadastro de clientes.
- Consulta e atualização de clientes.
- Desativação e reativação de clientes.
- Armazenamento de informações de endereço.
- Validação de dados cadastrais.
- Restrição de duplicidade de CPF e e-mail.

### Pedidos

- Criação de pedidos.
- Associação de pedidos a clientes.
- Inclusão de itens e quantidades na criação.
- Cálculo de subtotais e valor total.
- Consulta de pedidos.
- Consulta por cliente e status.
- Controle de transições de status.
- Cancelamento de pedidos.

### Controle de estoque e transações

- Controle da quantidade disponível de cada prato.
- Baixa automática de estoque durante a criação do pedido.
- Bloqueio de pedidos com quantidade insuficiente.
- Estorno de estoque no cancelamento.
- Gerenciamento transacional das operações de negócio.

### Autenticação e Usuários (Segurança)

- Autenticação Stateless baseada em **JWT (JSON Web Token)**.
- Criptografia de senhas com algoritmo **BCrypt**.
- Controle de perfis de acesso (**ROLE_ADMIN** e **ROLE_CLIENTE**).
- Endpoint para registro de usuários (`POST /auth/registrar`).
- Endpoint para login e emissão de token (`POST /auth/login`).
- Proteção de rotas administrativas e liberação pública de leitura do cardápio.
- Suporte a Bearer Token no Swagger UI.

### Tratamento de erros

A aplicação utiliza um tratamento global de exceções para padronizar as respostas da API, incluindo:

- **400 Bad Request:** dados de entrada inválidos.
- **404 Not Found:** recurso não encontrado.
- **409 Conflict:** conflito com uma regra de negócio.

---

## 🛠️ Tecnologias utilizadas

| Tecnologia | Finalidade |
|---|---|
| Java 25 | Linguagem de programação e versão configurada no projeto |
| Spring Boot | Estrutura principal da aplicação |
| Spring Web MVC | Construção dos endpoints REST |
| Spring Data JPA | Persistência e acesso aos dados |
| Hibernate | Implementação ORM utilizada pelo JPA |
| Bean Validation | Validação de dados |
| H2 Database | Banco de dados utilizado atualmente |
| Maven | Gerenciamento de dependências e build |
| Lombok | Apoio à redução de código repetitivo, onde utilizado |
| JUnit 5 | Testes automatizados |
| Mockito | Simulação de dependências nos testes unitários |
| SpringDoc OpenAPI | Documentação interativa da API |

---

## 🏗️ Arquitetura do projeto

O projeto utiliza uma arquitetura em camadas, separando as responsabilidades de acordo com o papel de cada componente.

O fluxo geral de uma requisição é:

```
Cliente HTTP → Controller → Service → Repository → Banco de dados
```

### Responsabilidades das camadas

| Camada | Responsabilidade |
|---|---|
| Controller | Receber requisições HTTP, validar entradas e retornar respostas |
| Service | Implementar regras de negócio e coordenar operações |
| Repository | Realizar operações de persistência e consultas |
| Model | Representar as entidades e os objetos do domínio |
| DTO | Definir os dados utilizados na comunicação com a API |
| Exception | Centralizar exceções e padronizar respostas de erro |
| Config | Reunir configurações da aplicação |

Essa organização busca evitar que regras de negócio fiquem concentradas nos Controllers ou que detalhes de persistência sejam expostos diretamente nas respostas HTTP.

---

## 📂 Estrutura de diretórios

A estrutura principal do projeto segue a organização abaixo:

```
nutri-express/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/com/nutriexpress/demo/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       │   ├── categoria/
│   │   │       │   ├── cliente/
│   │   │       │   ├── pedido/
│   │   │       │   └── prato/
│   │   │       ├── exception/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       └── NutriExpressApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── br/com/nutriexpress/demo/
│               └── service/
├── pom.xml
└── README.md
```

---

## ✅ Pré-requisitos

Para executar o projeto localmente, é necessário ter instalado:

- **Java 25** ou versão compatível.
- **Maven** (ou utilizar o Maven Wrapper incluído no projeto).
- Conexão com a internet para o download das dependências na primeira execução.

---

## ▶️ Como executar

1. Clone o repositório:

```bash
git clone https://github.com/seu-usuario/nutri-express.git
```

2. Acesse o diretório do projeto:

```bash
cd nutri-express
```

3. Execute a aplicação:

```bash
./mvnw spring-boot:run
```

> No Windows, utilize `.\mvnw.cmd spring-boot:run`.

4. A aplicação estará disponível em:

```
http://localhost:8080
```

> O banco de dados H2 é criado automaticamente em memória ao iniciar a aplicação. Os dados são perdidos ao encerrar a execução.

---

## 🧪 Testes

Para executar os testes automatizados:

```bash
./mvnw test
```

> No Windows, utilize `.\mvnw.cmd test`.

O projeto conta com uma pirâmide de testes consistente:
- **Testes unitários dos Services:** utilizando JUnit 5 e Mockito para validação isolada de regras de negócio, cálculos, integridade de estoque e soft delete.
- **Testes de integração dos Controllers:** utilizando `MockMvc` e `@WebMvcTest` para validação de endpoints HTTP, serialização JSON, códigos de status (200, 201, 204, 400, 404) e Bean Validation.
- **Testes de autenticação e segurança:** validação de criação, assinatura e expiração de tokens JWT.

---

## 📖 Documentação da API

A documentação interativa da API está disponível via Swagger UI após iniciar a aplicação:

```
http://localhost:8080/swagger-ui/index.html
```

A documentação é gerada automaticamente pelo SpringDoc OpenAPI e permite visualizar e testar os endpoints diretamente pelo navegador.

---

## 📊 Status do desenvolvimento

| Etapa | Descrição | Status |
|---|---|---|
| Etapa 1 | Categorias | ✅ Concluída |
| Etapa 2 | Pratos | ✅ Concluída |
| Etapa 3 | Banco de dados | ✅ Concluída (H2 dev + PostgreSQL) |
| Etapa 4 | Clientes e endereços | ✅ Concluída |
| Etapa 5 | Pedidos | ✅ Concluída |
| Etapa 6 | Estoque e transações | ✅ Concluída |
| Etapa 7 | Autenticação e usuários | ✅ Concluída (Spring Security + JWT) |
| Etapa 8 | Testes e documentação | ✅ Concluída (Unitários + Integração MockMvc) |

---

## 🔭 Próximos passos

- Migração para PostgreSQL com perfis de ambiente separados.
- Implementação de autenticação com Spring Security e JWT.
- Ampliação da cobertura de testes com testes de integração nos Controllers.
- Adição de paginação nas consultas que retornam listas.

---

## 📚 Aprendizados

Durante o desenvolvimento deste projeto, foram colocados em prática conceitos como:

- Construção de APIs REST seguindo boas práticas de design.
- Separação de responsabilidades entre camadas da aplicação.
- Uso de DTOs para controle do contrato da API.
- Validação de dados com Bean Validation.
- Tratamento centralizado de exceções com `@ControllerAdvice`.
- Gerenciamento de transações com `@Transactional`.
- Soft delete como alternativa à exclusão física de registros.
- Congelamento de preço em itens de pedido para garantir consistência histórica.
- Controle de estoque integrado ao fluxo de pedidos.
- Escrita de testes unitários com JUnit 5 e Mockito.
