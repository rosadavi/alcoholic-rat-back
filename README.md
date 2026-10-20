# alcoholic-rat

SaaS para publicação de imagens com metadados. O usuário envia uma imagem acompanhada de metadados, o sistema armazena ambos e, a partir deles, monta um **feed** com as imagens e suas informações.

> **Status:** estágio inicial. Já existem o cadastro de usuários (`POST /auth/register`), o login com token JWT (`POST /auth/login`), o filtro que valida o token a cada requisição e um seed que cria o administrador inicial. O upload de imagens, os metadados e o feed ainda não foram implementados.

## Stack

- Java 21
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- PostgreSQL
- Lombok
- java-jwt 4.5.0 (Auth0), para gerar e validar os tokens
- Maven (wrapper incluso: `./mvnw`)

## Como rodar

### Pré-requisitos

- JDK 21
- PostgreSQL rodando em `localhost:5432` com um banco chamado `alcoholic-rat`
- Arquivo `.env` na raiz do projeto (veja [Variáveis de ambiente](#variáveis-de-ambiente-env))

Para subir um Postgres rápido com Docker:

```bash
docker run -d --name alcoholic-rat-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=alcoholic-rat \
  -p 5432:5432 postgres
```

### Criar o `.env`

```bash
cp .env.example .env
# depois preencha ADMIN_EMAIL, ADMIN_PASSWORD e JWT_SECRET
```

### Executar a aplicação

```bash
./mvnw spring-boot:run
```

O `.env` é procurado no diretório de trabalho. Pela linha de comando, rode a partir da raiz do projeto. No IntelliJ, o *Working directory* da run configuration precisa ser a raiz.

### Rodar os testes

```bash
./mvnw test
```

> O teste `contextLoads` sobe o contexto completo, então precisa do Postgres disponível.

### IDE (IntelliJ)

O projeto usa Lombok (`@Getter`, `@Slf4j`, `@RequiredArgsConstructor`...). Se a IDE acusar construtores faltando ou `log` não resolvido mesmo com o `./mvnw compile` passando:

1. Recarregue o Maven (*Reload All Maven Projects*).
2. Confira se o plugin **Lombok** está habilitado (*Settings → Plugins*).
3. Confira se *Enable annotation processing* está marcado (*Settings → Build → Compiler → Annotation Processors*).
4. Faça *Build → Rebuild Project* e, se não resolver, *File → Invalidate Caches*.

## Configuração

### `application.properties`

Fica em `src/main/resources/application.properties`:

| Propriedade | Valor atual |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/alcoholic-rat` |
| `spring.datasource.username` / `password` | `postgres` / `postgres` |
| `spring.jpa.hibernate.ddl-auto` | `update` (o schema é criado e atualizado automaticamente) |
| `spring.jpa.show-sql` | `true` |
| `spring.config.import` | `optional:file:.env[.properties]` (lê o `.env` como properties) |
| `app.admin.email` | `${ADMIN_EMAIL:}` |
| `app.admin.password` | `${ADMIN_PASSWORD:}` |
| `app.jwt.secret` | `${JWT_SECRET:}` |
| `app.jwt.expiration-hours` | `2` (validade do token, em horas) |

> O `ddl-auto=update` só **adiciona** tabelas e colunas: ele nunca altera o tipo de uma coluna existente. Se o mapeamento de um campo mudar (ex.: um enum que passou de número para texto), é preciso ajustar ou recriar a tabela manualmente.

### Variáveis de ambiente (`.env`)

O `.env` **não é versionado** (está no `.gitignore`). Use o `.env.example` como modelo.

| Variável | Descrição |
|---|---|
| `ADMIN_EMAIL` | email (login) do usuário administrador criado na inicialização |
| `ADMIN_PASSWORD` | senha desse administrador (salva no banco com hash BCrypt) |
| `JWT_SECRET` | chave usada para assinar e validar os tokens (HMAC256). Quem tiver essa chave consegue gerar tokens válidos, então use um valor longo e aleatório e não compartilhe |

## Usuário admin inicial (seed)

Em toda inicialização, o `AdminSeeder` roda uma vez, depois que a aplicação termina de subir:

1. Se `ADMIN_EMAIL` ou `ADMIN_PASSWORD` estiverem vazios, registra um aviso no log e não faz nada.
2. Se já existir um usuário com esse login, não faz nada. Ele também **não** atualiza a senha caso ela mude no `.env`.
3. Caso contrário, cria o usuário com papel `ADMIN`, login em minúsculas e sem espaços, e a senha com hash BCrypt.

Para recriar o admin, apague o registro em `users` e reinicie a aplicação.

## Cadastro de usuários

`POST /auth/register` cria um usuário **sempre com papel `USER`**. O papel é definido no servidor e o cliente não consegue escolhê-lo.

- O login é normalizado (`trim` + minúsculas) antes de checar duplicidade e salvar.
- A senha é gravada com hash BCrypt.
- A resposta nunca inclui a senha.

**Requisição:**

```json
{ "login": "usuario@email.com", "password": "12345678" }
```

| Campo | Regras |
|---|---|
| `login` | obrigatório, email válido |
| `password` | obrigatório, de 8 a 72 caracteres (o BCrypt ignora o que passa de 72 bytes) |

**Respostas:**

| Status | Quando | Corpo |
|---|---|---|
| `201 Created` | usuário criado | `{ "id": "...", "login": "...", "role": "USER" }` |
| `400 Bad Request` | falha de validação | `{ "<campo>": "<mensagem>" }` |
| `409 Conflict` | login já cadastrado | `{ "error": "Login already exists: ..." }` |

```bash
curl -i -X POST localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"login":"teste@email.com","password":"12345678"}'
```

## Login (JWT)

`POST /auth/login` recebe login e senha e devolve um token JWT.

- O login é normalizado (`trim` + minúsculas) antes da autenticação, igual ao cadastro.
- A autenticação passa pelo `AuthenticationManager`, que usa o `AuthorizationService` para buscar o usuário e o BCrypt para comparar a senha.
- O token é assinado com HMAC256 usando `JWT_SECRET`, tem issuer `alcoholic-rat`, o login do usuário como `subject` e vale `app.jwt.expiration-hours` horas (hoje, 2).

**Requisição:**

```json
{ "login": "usuario@email.com", "password": "12345678" }
```

| Campo | Regras |
|---|---|
| `login` | obrigatório |
| `password` | obrigatório |

**Respostas:**

| Status | Quando | Corpo |
|---|---|---|
| `200 OK` | credenciais válidas | `{ "token": "..." }` |
| `400 Bad Request` | campo em branco | `{ "<campo>": "<mensagem>" }` |
| `401 Unauthorized` | login inexistente ou senha errada | vazio (veja [Problemas conhecidos](#problemas-conhecidos)) |

```bash
curl -i -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"teste@email.com","password":"12345678"}'
```

### Usando o token

Envie o token no header `Authorization` com o prefixo `Bearer `:

```bash
curl -i localhost:8080/<rota> -H 'Authorization: Bearer <token>'
```

O `SecurityFilter` roda uma vez por requisição, antes do filtro padrão de usuário e senha:

1. Lê o header `Authorization`. Se ele não existir ou não começar com `Bearer `, segue sem autenticar.
2. Valida assinatura, issuer e expiração do token. Se o token for inválido ou estiver expirado, segue sem autenticar.
3. Busca o usuário pelo login do token. Se ele existir, coloca-o como autenticado no `SecurityContext`, com as authorities do papel dele.

A requisição sem autenticação só passa se a rota for pública. Nas outras, a resposta é `401`.

## Estrutura

```
src/main/java/com/rosadavi/alcoholic_rat/
├── AlcoholicRatApplication.java          # entrada da aplicação
├── auth/security/
│   ├── SecurityConfiguration.java        # regras de acesso, AuthenticationManager e PasswordEncoder (BCrypt)
│   ├── SecurityFilter.java               # lê o Bearer token de cada requisição e autentica o usuário
│   └── TokenService.java                 # gera e valida tokens JWT (HMAC256)
├── config/
│   └── AdminSeeder.java                  # cria o usuário admin na inicialização, se não existir
├── controller/
│   ├── AuthenticationController.java     # /auth (register, login)
│   └── HelloWorld.java                   # GET / -> "Hello World!"
├── DTO/user/
│   ├── LoginRequestDTO.java              # entrada do login (login, password)
│   ├── LoginResponseDTO.java             # saída do login (token)
│   ├── RegisterRequestDTO.java           # entrada do cadastro (login, password) com validação
│   └── RegisterResponseDTO.java          # saída do cadastro (id, login, role), sem senha
├── domain/user/
│   ├── User.java                         # entidade de usuário (implementa UserDetails)
│   └── UserRole.java                     # papéis: ADMIN, USER
├── exception/
│   ├── GlobalExceptionHandler.java       # converte exceções em respostas HTTP (400, 409, 401)
│   └── LoginAlreadyExistsException.java  # login duplicado no cadastro
├── repository/
│   └── UserRepository.java               # acesso a usuários (findByLogin, existsByLogin)
└── services/
    ├── AuthorizationService.java         # UserDetailsService (carrega usuário por login ou lança UsernameNotFoundException)
    └── UserService.java                  # regra de cadastro (role USER, BCrypt, duplicidade)
```

## Domínio atual

### Usuário (`users`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `login` | String | email do usuário, usado como username. **Único e obrigatório** |
| `password` | String | hash BCrypt |
| `role` | `UserRole` | `ADMIN` ou `USER`, gravado como texto (`@Enumerated(EnumType.STRING)`) |

O papel `ADMIN` recebe as authorities `ROLE_ADMIN` e `ROLE_USER`. O papel `USER` recebe só `ROLE_USER`.

## Segurança

- API **stateless**: sem sessão no servidor e com CSRF desabilitado. A autenticação é feita só pelo token JWT.
- Senhas com hash **BCrypt** (bean `PasswordEncoder` em `SecurityConfiguration`).
- Rotas públicas: `GET /`, `POST /auth/register`, `POST /auth/login` e `/errors`.
- **Todas as outras rotas exigem um token válido.** Sem token, ou com token inválido ou expirado, a resposta é `401 Unauthorized` (configurado com `HttpStatusEntryPoint`).
- Nenhuma rota exige `ROLE_ADMIN` por enquanto.

### Problemas conhecidos

Testado em 2026-10-20 subindo a aplicação localmente:

- **O handler de `401` do `GlobalExceptionHandler` não é usado.** Ele importa `javax.naming.AuthenticationException`, e não a do Spring Security (`org.springframework.security.core.AuthenticationException`). Por isso o erro de credenciais inválidas não chega nele, e o `401` do login sai com corpo vazio, em vez de `{ "error": "Invalid login or password" }`.
- **A rota de erro liberada é `/errors`, mas a do Spring Boot é `/error`.** Como `/error` exige autenticação, todo erro que não é tratado por um handler vira `401` sem corpo. Exemplo: um JSON malformado no `POST /auth/register` responde `401`, quando deveria ser `400`. É também por isso que o login inválido acaba em `401`.

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/auth/register` | Cadastra um usuário com papel `USER` | público |
| POST | `/auth/login` | Autentica e devolve um token JWT | público |
| GET | `/` | Retorna "Hello World!" | público |

## Roadmap

### Usuários e autenticação
- [x] Corrigir `User.getPassword()`, que retornava `""`
- [x] Adicionar `@NoArgsConstructor` em `User` (o JPA exige)
- [x] Mapear `role` com `@Enumerated(EnumType.STRING)`
- [x] `PasswordEncoder` (BCrypt)
- [x] Seed do usuário admin via `.env` (`AdminSeeder`)
- [x] Endpoint `POST /auth/register` (sempre `USER`, validação, login único)
- [x] Voltar `anyRequest()` para `.authenticated()`
- [x] Lançar `UsernameNotFoundException` quando o login não existir
- [x] Endpoint `POST /auth/login`
- [x] Geração e validação de token JWT, com filtro de segurança
- [x] Responder `401` (em vez de `403`) para requisições sem autenticação
- [ ] Corrigir o import de `AuthenticationException` no `GlobalExceptionHandler`
- [ ] Liberar `/error` (hoje está `/errors`) na configuração de segurança

### Imagens e feed
- [ ] Entidade de publicação (imagem + metadados + autor + data)
- [ ] Upload de imagem com metadados
- [ ] Armazenamento das imagens (disco local / object storage, a definir)
- [ ] Endpoint de feed paginado
- [x] Remover a regra legada de `/product` da configuração de segurança
