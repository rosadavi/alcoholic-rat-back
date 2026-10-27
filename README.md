# alcoholic-rat

SaaS para publicação de imagens com metadados. O usuário envia uma imagem acompanhada de metadados, o sistema armazena ambos e, a partir deles, monta um **feed** com as imagens e suas informações.

> **Status:** estágio inicial. Já existem o cadastro de usuários (`POST /auth/register`), o login com token JWT (`POST /auth/login`), o filtro que valida o token a cada requisição, um seed que cria o administrador inicial, o cadastro e a listagem de bares (`/bars`) e de bebidas (`/drinks`), com cadastro restrito a admin, a criação de posts com as bebidas consumidas (`POST /posts`) e a adição de bebidas a um post existente (`POST /posts/{id}/drinks`). O upload de fotos (na criação ou depois) e o feed ainda não foram implementados.

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

## Bebidas

Catálogo de bebidas cadastrado pelo admin. Os usuários escolhem uma bebida dele ao publicar.

### `POST /drinks` (só `ADMIN`)

**Requisição:**

```json
{
  "title": "Heineken Long Neck",
  "description": "cerveja lager",
  "volumeMl": 330,
  "alcoholPercentage": 5.0
}
```

| Campo | Regras |
|---|---|
| `title` | obrigatório, até 100 caracteres. Salvo com `trim()`. Único, sem diferenciar maiúsculas e minúsculas |
| `description` | opcional, até 1000 caracteres |
| `volumeMl` | obrigatório, inteiro maior que `0` e até `5000` |
| `alcoholPercentage` | obrigatório, de `0` a `100` (porcentagem) |

**Respostas:**

| Status | Quando | Corpo |
|---|---|---|
| `201 Created` | bebida criada. Header `Location: /drinks/{id}` | `{ "id": "...", "title": "...", "description": "...", "volumeMl": ..., "alcoholPercentage": ... }` |
| `400 Bad Request` | falha de validação | `{ "<campo>": "<mensagem>" }` |
| `401 Unauthorized` | sem token, ou token inválido ou expirado | `{ "error": "You must be logged in to access this resource!" }` |
| `403 Forbidden` | token válido, mas de um usuário `USER` | `{ "error": "Only administrators can perform this action!" }` |
| `409 Conflict` | já existe uma bebida com esse título | `{ "error": "Drink already exists: ..." }` |

```bash
curl -i -X POST localhost:8080/drinks \
  -H 'Authorization: Bearer <token-de-admin>' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Heineken Long Neck","description":"cerveja lager","volumeMl":330,"alcoholPercentage":5.0}'
```

### `GET /drinks` (qualquer usuário autenticado)

Devolve `200 OK` com a lista de todas as bebidas, no mesmo formato do `201` acima. Se não houver nenhuma, devolve `[]`.

```bash
curl -i localhost:8080/drinks -H 'Authorization: Bearer <token>'
```

## Posts

Um post registra o que o usuário bebeu. Cada bebida do post (`PostDrink`) é uma **cópia** dos dados no momento do registro e pode vir de dois jeitos:

- **Do catálogo:** informe `drinkId`. Título, ml e teor vêm da bebida cadastrada pelo admin, e qualquer um desses campos enviado no item substitui o valor do catálogo só nesse post.
- **Própria:** sem `drinkId`. `title`, `volumeMl` e `alcoholPercentage` passam a ser obrigatórios.

Nos dois casos **nada é gravado em `drinks`**. Se o admin alterar a bebida depois, o post continua com os valores de quando foi registrado.

### `POST /posts` (qualquer usuário autenticado)

O autor é sempre o usuário do token, e não um campo da requisição. Por enquanto a requisição é só JSON, sem fotos.

**Requisição:**

```json
{
  "barId": "<id-do-bar>",
  "description": "sextou",
  "drinks": [
    { "drinkId": "<id-do-catalogo>", "volumeMl": 600 },
    { "title": "caipirinha da casa", "volumeMl": 300, "alcoholPercentage": 15 }
  ]
}
```

| Campo | Regras |
|---|---|
| `barId` | opcional. Se enviado, precisa existir em `bars` |
| `description` | opcional, até 1000 caracteres |
| `drinks` | obrigatório, com pelo menos um item |
| `drinks[].drinkId` | opcional. Se enviado, precisa existir em `drinks` |
| `drinks[].title` | até 100 caracteres. Obrigatório sem `drinkId`. Salvo com `trim()` |
| `drinks[].volumeMl` | inteiro maior que `0` e até `5000`. Obrigatório sem `drinkId` |
| `drinks[].alcoholPercentage` | de `0` a `100`. Obrigatório sem `drinkId` |

**Respostas:**

| Status | Quando | Corpo |
|---|---|---|
| `201 Created` | post criado. Header `Location: /posts/{id}` | veja abaixo |
| `400 Bad Request` | falha de validação de campo | `{ "<campo>": "<mensagem>" }` |
| `400 Bad Request` | `drinks` vazio ou ausente | `{ "error": "Post must contain at least one drink" }` |
| `400 Bad Request` | bebida própria sem título, ml ou teor | `{ "error": "Drink must have a title, volume and alcohol percentage" }` |
| `401 Unauthorized` | sem token, ou token inválido ou expirado | `{ "error": "You must be logged in to access this resource!" }` |
| `404 Not Found` | `barId` inexistente | `{ "error": "Bar not found: ..." }` |
| `404 Not Found` | `drinkId` inexistente | `{ "error": "Drink not found: ..." }` |

Corpo do `201`:

```json
{
  "id": "...",
  "authorLogin": "usuario@email.com",
  "barId": "...",
  "description": "sextou",
  "createdAt": "2026-10-26T22:10:00Z",
  "drinks": [
    { "id": "...", "drinkId": "...", "title": "...", "volumeMl": 600, "alcoholPercentage": 5.0, "addedAt": "..." },
    { "id": "...", "drinkId": null, "title": "caipirinha da casa", "volumeMl": 300, "alcoholPercentage": 15, "addedAt": "..." }
  ],
  "photos": []
}
```

`photos` volta sempre vazio enquanto o upload não existir.

```bash
curl -i -X POST localhost:8080/posts \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"description":"sextou","drinks":[{"title":"caipirinha da casa","volumeMl":300,"alcoholPercentage":15}]}'
```

### `POST /posts/{id}/drinks` (só o autor do post)

Adiciona bebidas a um post que já existe. Só o autor do post pode usar, e só até 24h depois de `createdAt` (`Post.EDIT_WINDOW`). As bebidas já salvas não são alteradas nem removidas, e `barId` e `description` do post não mudam.

**Requisição:**

```json
{
  "drinks": [
    { "drinkId": "<id-do-catalogo>" },
    { "title": "dose de cachaça", "volumeMl": 50, "alcoholPercentage": 40 }
  ]
}
```

Cada item de `drinks` segue as mesmas regras do `POST /posts`. A lista é obrigatória e precisa ter pelo menos um item (`@NotEmpty`).

**Respostas:**

| Status | Quando | Corpo |
|---|---|---|
| `200 OK` | bebidas adicionadas | o post completo, no mesmo formato do `201` do `POST /posts` |
| `400 Bad Request` | falha de validação de campo, incluindo `drinks` vazio ou ausente | `{ "<campo>": "<mensagem>" }` |
| `400 Bad Request` | bebida própria sem título, ml ou teor | `{ "error": "Drink must have a title, volume and alcohol percentage" }` |
| `401 Unauthorized` | sem token, ou token inválido ou expirado | `{ "error": "You must be logged in to access this resource!" }` |
| `403 Forbidden` | o usuário do token não é o autor do post | `{ "error": "Only the author can add drinks to this post." }` |
| `403 Forbidden` | já se passaram 24h desde a criação do post | `{ "error": "Post can no longer be edited." }` |
| `404 Not Found` | `id` do post inexistente | `{ "error": "Post not found: ..." }` |
| `404 Not Found` | `drinkId` inexistente | `{ "error": "Drink not found: ..." }` |

O post é buscado e alterado dentro de um `@Transactional`. As novas `PostDrink` são gravadas no commit por causa do `CascadeType.PERSIST`, sem chamar `postRepository.save`.

```bash
curl -i -X POST localhost:8080/posts/<id-do-post>/drinks \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"drinks":[{"title":"dose de cachaça","volumeMl":50,"alcoholPercentage":40}]}'
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
│   ├── DrinkController.java              # /drinks (cadastro e listagem)
│   ├── PostController.java               # /posts (criação e adição de bebidas)
│   └── HelloWorld.java                   # GET / -> "Hello World!"
├── DTO/bar/
│   ├── BarRequestDTO.java                # entrada do cadastro de bar (name, description, latitude, longitude) com validação
│   └── BarResponseDTO.java               # saída de bar (ainda não usado: o controller devolve a entidade Bar)
├── DTO/drink/
│   └── DrinkRequestDTO.java              # entrada do cadastro de bebida (title, description, volumeMl, alcoholPercentage) com validação
├── DTO/post/
│   ├── PostCreateRequestDTO.java         # entrada da criação de post (barId, description, drinks)
│   ├── PostDrinkAppendDTO.java           # entrada da adição de bebidas a um post (drinks, @NotEmpty)
│   ├── PostDrinkRequestDTO.java          # bebida do post (drinkId opcional, title, volumeMl, alcoholPercentage)
│   ├── PostDrinkResponseDTO.java         # saída de uma bebida do post
│   ├── PostPhotoResponseDTO.java         # saída de uma foto do post (id, url, contentType, addedAt)
│   └── PostResponseDTO.java              # saída do post, com bebidas e fotos, sem expor a entidade User
├── DTO/user/
│   ├── LoginRequestDTO.java              # entrada do login (login, password)
│   ├── LoginResponseDTO.java             # saída do login (token)
│   ├── RegisterRequestDTO.java           # entrada do cadastro (login, password) com validação
│   └── RegisterResponseDTO.java          # saída do cadastro (id, login, role), sem senha
├── domain/bar/
│   └── Bar.java                          # entidade de bar
├── domain/drink/
│   └── Drink.java                        # entidade de bebida do catálogo
├── domain/post/
│   ├── Post.java                         # entidade de post (janela de edição de 24h em EDIT_WINDOW)
│   ├── PostDrink.java                    # bebida consumida no post (cópia dos dados, imutável)
│   └── PostPhoto.java                    # foto do post (ainda sem upload)
├── domain/user/
│   ├── User.java                         # entidade de usuário (implementa UserDetails)
│   └── UserRole.java                     # papéis: ADMIN, USER
├── exception/
│   ├── BarAlreadyExistsException.java    # bar com nome duplicado
│   ├── BarNotFoundException.java         # barId inexistente (404)
│   ├── DrinkAlreadyExistsException.java  # bebida com título duplicado
│   ├── DrinkNotFoundException.java       # drinkId inexistente (404)
│   ├── GlobalExceptionHandler.java       # converte exceções dos controllers em respostas HTTP (400, 401, 403, 404, 409)
│   ├── InvalidPostException.java         # post sem bebidas ou bebida própria incompleta (400)
│   ├── PostForbiddenException.java       # não é o autor do post ou a janela de 24h já fechou (403)
│   ├── PostNotFoundException.java        # id de post inexistente (404)
│   └── LoginAlreadyExistsException.java  # login duplicado no cadastro
├── repository/
│   ├── BarRepository.java                # acesso a bares (existsByNameIgnoreCase)
│   ├── DrinkRepository.java              # acesso a bebidas (existsByTitleIgnoreCase)
│   ├── PostRepository.java               # acesso a posts
│   └── UserRepository.java               # acesso a usuários (findByLogin, existsByLogin)
└── services/
    ├── AuthorizationService.java         # UserDetailsService (carrega usuário por login ou lança UsernameNotFoundException)
    ├── BarService.java                   # regra de cadastro de bar (nome único) e listagem
    ├── DrinkService.java                 # regra de cadastro de bebida (título único) e listagem
    ├── PostService.java                  # criação de post e adição de bebidas (autor do token, bebidas do catálogo ou próprias, janela de 24h)
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

### Bebida (`drinks`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `title` | String | obrigatório. A unicidade é checada no `DrinkService` (sem diferenciar maiúsculas e minúsculas), não por constraint no banco |
| `description` | String (até 1000) | opcional |
| `volumeMl` | Integer | obrigatório, volume em ml |
| `alcoholPercentage` | BigDecimal | obrigatório, teor alcoólico em % |

### Post (`post`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `author_id` | FK → `users` | obrigatório, o usuário do token |
| `bar_id` | FK → `bars` | opcional |
| `description` | String (até 1000) | opcional |
| `createdAt` | Instant | definido na criação. A janela de 24h para adicionar itens conta a partir daqui (`Post.EDIT_WINDOW`) |

Todas as colunas têm `updatable = false`: o post não muda depois de criado. As listas `drinks` e `photos` usam só `CascadeType.PERSIST`, sem `orphanRemoval`, então adicionar itens grava as linhas novas e nunca apaga as existentes.

### Bebida do post (`post_drink`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `post_id` | FK → `post` | obrigatório |
| `drink_id` | FK → `drinks` | opcional. `null` quando é uma bebida própria |
| `title` | String | obrigatório |
| `volumeMl` | Integer | obrigatório |
| `alcoholPercentage` | BigDecimal (4,1) | obrigatório |
| `addedAt` | Instant | definido na criação do item |

Todas as colunas têm `updatable = false`.

### Foto do post (`post_photo`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `post_id` | FK → `post` | obrigatório |
| `fileName` | String | nome do arquivo no disco (não é exposto na API) |
| `contentType` | String | tipo da imagem |
| `addedAt` | Instant | quando a foto foi adicionada |

A tabela já existe, mas ainda não há upload.

## Segurança

- API **stateless**: sem sessão no servidor e com CSRF desabilitado. A autenticação é feita só pelo token JWT.
- Senhas com hash **BCrypt** (bean `PasswordEncoder` em `SecurityConfiguration`).
- Rotas públicas: `GET /`, `POST /auth/register`, `POST /auth/login` e `/error` (a rota de erro do Spring Boot).
- `POST /bars` e `POST /drinks` exigem `ROLE_ADMIN` (`hasRole("ADMIN")`).
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

- **Um item `null` em `drinks` (ex.: `"drinks": [null]`) gera `500`**, porque o `PostService` chama `d.drinkId()` sem checar se o item é nulo. Vale para `POST /posts` e `POST /posts/{id}/drinks`, já que o `@NotEmpty` aceita uma lista com um item nulo. (Identificado lendo o código em 2026-10-26 e 2026-10-27, não testado com a aplicação rodando.)
- **Erros não tratados por um handler saem no formato padrão do Spring Boot**, como `{ "timestamp": ..., "status": 400, "error": "Bad Request", "path": ... }` (ex.: JSON malformado), diferente do `{ "error": "..." }` usado no resto da API.

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/auth/register` | Cadastra um usuário com papel `USER` | público |
| POST | `/auth/login` | Autentica e devolve um token JWT | público |
| GET | `/` | Retorna "Hello World!" | público |
| POST | `/bars` | Cadastra um bar | `ADMIN` |
| GET | `/bars` | Lista todos os bares | autenticado |
| POST | `/drinks` | Cadastra uma bebida | `ADMIN` |
| GET | `/drinks` | Lista todas as bebidas | autenticado |
| POST | `/posts` | Cria um post com as bebidas consumidas | autenticado |
| POST | `/posts/{id}/drinks` | Adiciona bebidas a um post (até 24h depois da criação) | autor do post |

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
- [x] Salvar o nome com `trim()`
- [ ] Usar o `BarResponseDTO` na resposta em vez da entidade `Bar`

### Bebidas (catálogo do admin)
- [x] Entidade `Drink`: título, descrição, volume em ml e teor alcoólico (%)
- [x] `POST /drinks` restrito a `ROLE_ADMIN`, com título único sem diferenciar maiúsculas e minúsculas (`409` no duplicado)
- [x] `GET /drinks` para os usuários escolherem a bebida ao publicar

### Posts e feed
- [x] Entidade `Post`: autor, bar (opcional), descrição (opcional) e data de criação. Os dados do post não mudam depois de criado
- [x] Entidade `PostDrink` (bebida consumida): cópia dos dados (título, ml, teor alcoólico) no momento do registro, com referência opcional à `Drink` do catálogo. O usuário pode alterar ml e teor de uma bebida do catálogo ou informar uma bebida própria, sem criar nada em `drinks`
- [x] Entidade `PostPhoto`: foto do post, com data em que foi adicionada
- [ ] Armazenamento das fotos em disco local (`uploads/`, fora do Git), com nome gerado por UUID
- [x] `POST /posts` (JSON) com bebidas do catálogo ou próprias, com o autor sendo o usuário do token
- [ ] Aceitar fotos no `POST /posts` (multipart: JSON `data` + arquivos `photos`)
- [x] `POST /posts/{id}/drinks`: só **adiciona** bebidas, só o autor pode usar e só até 24h depois da criação do post
- [ ] `POST /posts/{id}/photos`: mesmas regras do `/drinks` (só adiciona, só o autor, até 24h). Não existe rota para editar ou remover bebidas e fotos já salvas
- [ ] `GET /posts/{id}/photos/{photoId}` para servir a foto
- [ ] `GET /posts` com o feed paginado, do mais recente para o mais antigo
- [x] Remover a regra legada de `/product` da configuração de segurança
