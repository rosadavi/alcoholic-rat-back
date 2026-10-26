# alcoholic-rat

SaaS para publicação de imagens com metadados. O usuário envia uma imagem acompanhada de metadados, o sistema armazena ambos e, a partir deles, monta um **feed** com as imagens e suas informações.

> **Status:** estágio inicial. Já existem o cadastro de usuários (`POST /auth/register`), o login com token JWT (`POST /auth/login`), o filtro que valida o token a cada requisição, um seed que cria o administrador inicial e o cadastro e a listagem de bares (`/bars`, com cadastro restrito a admin). O upload de imagens, os metadados e o feed ainda não foram implementados.

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
| `401 Unauthorized` | login inexistente ou senha errada | `{ "error": "Invalid login or password" }` |

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

A requisição sem autenticação só passa se a rota for pública. Nas outras, a resposta é `401`. Veja as respostas de erro em [Segurança](#respostas-de-erro-de-segurança-401-e-403).

## Bares

Os bares são cadastrados pelo admin e listados para os usuários escolherem ao publicar.

### `POST /bars` (só `ADMIN`)

**Requisição:**

```json
{
  "name": "Porks",
  "description": "bar foda",
  "latitude": -15.8463984,
  "longitude": -47.9799406
}
```

| Campo | Regras |
|---|---|
| `name` | obrigatório, até 100 caracteres. Único, sem diferenciar maiúsculas e minúsculas (`Porks` e `porks` são o mesmo bar) |
| `description` | opcional, até 1000 caracteres |
| `latitude` | obrigatório, entre `-90` e `90` |
| `longitude` | obrigatório, entre `-180` e `180` |

A latitude e a longitude devem ir como número. Como string (`"-15.84"`) também são aceitas, porque o Jackson converte.

**Respostas:**

| Status | Quando | Corpo |
|---|---|---|
| `201 Created` | bar criado. Header `Location: /bars/{id}` | `{ "id": "...", "name": "...", "description": "...", "latitude": ..., "longitude": ... }` |
| `400 Bad Request` | falha de validação | `{ "<campo>": "<mensagem>" }` |
| `401 Unauthorized` | sem token, ou token inválido ou expirado | `{ "error": "You must be logged in to access this resource!" }` |
| `403 Forbidden` | token válido, mas de um usuário `USER` | `{ "error": "Only administrators can perform this action!" }` |
| `409 Conflict` | já existe um bar com esse nome | `{ "error": "Bar already exists: ..." }` |

```bash
curl -i -X POST localhost:8080/bars \
  -H 'Authorization: Bearer <token-de-admin>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Porks","description":"bar foda","latitude":-15.8463984,"longitude":-47.9799406}'
```

### `GET /bars` (qualquer usuário autenticado)

Devolve `200 OK` com a lista de todos os bares, no mesmo formato do `201` acima. Se não houver nenhum, devolve `[]`.

```bash
curl -i localhost:8080/bars -H 'Authorization: Bearer <token>'
```

## Estrutura

```
src/main/java/com/rosadavi/alcoholic_rat/
├── AlcoholicRatApplication.java          # entrada da aplicação
├── auth/security/
│   ├── SecurityConfiguration.java        # regras de acesso, respostas 401/403, AuthenticationManager e PasswordEncoder (BCrypt)
│   ├── SecurityFilter.java               # lê o Bearer token de cada requisição e autentica o usuário
│   └── TokenService.java                 # gera e valida tokens JWT (HMAC256)
├── config/
│   └── AdminSeeder.java                  # cria o usuário admin na inicialização, se não existir
├── controller/
│   ├── AuthenticationController.java     # /auth (register, login)
│   ├── BarController.java                # /bars (cadastro e listagem)
│   └── HelloWorld.java                   # GET / -> "Hello World!"
├── DTO/bar/
│   ├── BarRequestDTO.java                # entrada do cadastro de bar (name, description, latitude, longitude) com validação
│   └── BarResponseDTO.java               # saída de bar (ainda não usado: o controller devolve a entidade Bar)
├── DTO/user/
│   ├── LoginRequestDTO.java              # entrada do login (login, password)
│   ├── LoginResponseDTO.java             # saída do login (token)
│   ├── RegisterRequestDTO.java           # entrada do cadastro (login, password) com validação
│   └── RegisterResponseDTO.java          # saída do cadastro (id, login, role), sem senha
├── domain/bar/
│   └── Bar.java                          # entidade de bar
├── domain/user/
│   ├── User.java                         # entidade de usuário (implementa UserDetails)
│   └── UserRole.java                     # papéis: ADMIN, USER
├── exception/
│   ├── BarAlreadyExistsException.java    # bar com nome duplicado
│   ├── GlobalExceptionHandler.java       # converte exceções dos controllers em respostas HTTP (400, 409, 401)
│   └── LoginAlreadyExistsException.java  # login duplicado no cadastro
├── repository/
│   ├── BarRepository.java                # acesso a bares (existsByNameIgnoreCase)
│   └── UserRepository.java               # acesso a usuários (findByLogin, existsByLogin)
└── services/
    ├── AuthorizationService.java         # UserDetailsService (carrega usuário por login ou lança UsernameNotFoundException)
    ├── BarService.java                   # regra de cadastro de bar (nome único) e listagem
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

### Bar (`bars`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `name` | String | obrigatório. A unicidade é checada no `BarService` (sem diferenciar maiúsculas e minúsculas), não por constraint no banco |
| `description` | String (até 1000) | opcional |
| `latitude` | Double | obrigatório |
| `longitude` | Double | obrigatório |

## Segurança

- API **stateless**: sem sessão no servidor e com CSRF desabilitado. A autenticação é feita só pelo token JWT.
- Senhas com hash **BCrypt** (bean `PasswordEncoder` em `SecurityConfiguration`).
- Rotas públicas: `GET /`, `POST /auth/register`, `POST /auth/login` e `/error` (a rota de erro do Spring Boot).
- `POST /bars` exige `ROLE_ADMIN` (`hasRole("ADMIN")`).
- **Todas as outras rotas exigem um token válido.**

### Respostas de erro de segurança (401 e 403)

As duas são configuradas no `exceptionHandling` da `SecurityConfiguration`, e não no `GlobalExceptionHandler`, porque a requisição é barrada no filtro de segurança, antes de chegar ao controller. O Spring escolhe qual das duas chamar:

| Situação | Quem responde | Resposta |
|---|---|---|
| Rota protegida sem token, ou com token inválido ou expirado | `authenticationEntryPoint` | `401 { "error": "You must be logged in to access this resource!" }` |
| Token válido, mas sem o papel exigido (ex.: `USER` em `POST /bars`) | `accessDeniedHandler` | `403 { "error": "Only administrators can perform this action!" }` |

- A mensagem do `403` vale para **qualquer** rota negada por papel. Se outras rotas forem restritas a outros papéis no futuro, convém trocar para um texto neutro.
- O `401` não diferencia o motivo (sem token, token inválido, token expirado), porque o `SecurityFilter` só segue sem autenticar em todos esses casos.

### Problemas conhecidos

Testado em 2026-10-22 com a aplicação rodando localmente:

- **O nome do bar é checado sem espaços nas pontas, mas é salvo como veio.** O `BarService` usa `data.name().trim()` no `existsByNameIgnoreCase`, mas cria o `Bar` com `data.name()`. Assim, `" Porks"` é barrado se `Porks` já existir, mas, se for o primeiro, é gravado com o espaço.
- **Erros não tratados por um handler saem no formato padrão do Spring Boot**, como `{ "timestamp": ..., "status": 400, "error": "Bad Request", "path": ... }` (ex.: JSON malformado), diferente do `{ "error": "..." }` usado no resto da API.

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/auth/register` | Cadastra um usuário com papel `USER` | público |
| POST | `/auth/login` | Autentica e devolve um token JWT | público |
| GET | `/` | Retorna "Hello World!" | público |
| POST | `/bars` | Cadastra um bar | `ADMIN` |
| GET | `/bars` | Lista todos os bares | autenticado |

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
- [x] Corrigir o import de `AuthenticationException` no `GlobalExceptionHandler`
- [x] Liberar `/error` (antes estava `/errors`) na configuração de segurança
- [x] Corpo JSON nas respostas `401` (não autenticado) e `403` (sem permissão)
- [ ] Diferenciar o motivo do `401` (sem token, token inválido, token expirado)

### Bares
- [x] Entidade `Bar` (nome, descrição, latitude, longitude)
- [x] `POST /bars` restrito a `ROLE_ADMIN`
- [x] `GET /bars` para os usuários escolherem o bar ao publicar
- [x] Nome único sem diferenciar maiúsculas e minúsculas (`409` no duplicado)
- [ ] Salvar o nome com `trim()` (hoje só a checagem de duplicidade usa)
- [ ] Usar o `BarResponseDTO` na resposta em vez da entidade `Bar`

### Bebidas (catálogo do admin)
- [ ] Entidade `Drink`: título, descrição, volume em ml e teor alcoólico (%)
- [ ] `POST /drinks` restrito a `ROLE_ADMIN`, com título único sem diferenciar maiúsculas e minúsculas (`409` no duplicado)
- [ ] `GET /drinks` para os usuários escolherem a bebida ao publicar

### Posts e feed
- [ ] Entidade `Post`: autor, bar (opcional), descrição (opcional) e data de criação. Os dados do post não mudam depois de criado
- [ ] Entidade `PostDrink` (bebida consumida): cópia dos dados (título, ml, teor alcoólico) no momento do registro, com referência opcional à `Drink` do catálogo. O usuário pode alterar ml e teor de uma bebida do catálogo ou informar uma bebida própria, sem criar nada em `drinks`
- [ ] Entidade `PostPhoto`: foto do post, com data em que foi adicionada
- [ ] Armazenamento das fotos em disco local (`uploads/`, fora do Git), com nome gerado por UUID
- [ ] `POST /posts` (multipart: JSON `data` + arquivos `photos`), com o autor sendo o usuário do token
- [ ] `POST /posts/{id}/drinks` e `POST /posts/{id}/photos`: só **adicionam** itens, só o autor pode usar e só até 24h depois da criação do post. Não existe rota para editar ou remover bebidas e fotos já salvas
- [ ] `GET /posts/{id}/photos/{photoId}` para servir a foto
- [ ] `GET /posts` com o feed paginado, do mais recente para o mais antigo
- [x] Remover a regra legada de `/product` da configuração de segurança
