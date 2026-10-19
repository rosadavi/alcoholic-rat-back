# alcoholic-rat

SaaS para publicação de imagens com metadados. O usuário envia uma imagem acompanhada de metadados, o sistema armazena ambos e, a partir deles, monta um **feed** com as imagens e suas informações.

> **Status:** estágio inicial. Já existem o cadastro de usuários com papéis e um seed que cria o administrador inicial. A autenticação (JWT) ainda não foi implementada, e o upload de imagens, os metadados e o feed também não.

## Stack

- Java 21
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security)
- PostgreSQL
- Lombok
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
# depois preencha ADMIN_EMAIL e ADMIN_PASSWORD
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

> O `ddl-auto=update` só **adiciona** tabelas e colunas: ele nunca altera o tipo de uma coluna existente. Se o mapeamento de um campo mudar (ex.: um enum que passou de número para texto), é preciso ajustar ou recriar a tabela manualmente.

### Variáveis de ambiente (`.env`)

O `.env` **não é versionado** (está no `.gitignore`). Use o `.env.example` como modelo.

| Variável | Descrição |
|---|---|
| `ADMIN_EMAIL` | email (login) do usuário administrador criado na inicialização |
| `ADMIN_PASSWORD` | senha desse administrador (salva no banco com hash BCrypt) |

## Usuário admin inicial (seed)

Em toda inicialização, o `AdminSeeder` roda uma vez, depois que a aplicação termina de subir:

1. Se `ADMIN_EMAIL` ou `ADMIN_PASSWORD` estiverem vazios, registra um aviso no log e não faz nada.
2. Se já existir um usuário com esse login, não faz nada. Ele também **não** atualiza a senha caso ela mude no `.env`.
3. Caso contrário, cria o usuário com papel `ADMIN` e a senha com hash BCrypt.

Para recriar o admin, apague o registro em `users` e reinicie a aplicação.

## Estrutura

```
src/main/java/com/rosadavi/alcoholic_rat/
├── AlcoholicRatApplication.java      # entrada da aplicação
├── auth/security/
│   └── SecurityConfiguration.java    # filtros do Spring Security + PasswordEncoder (BCrypt)
├── config/
│   └── AdminSeeder.java              # cria o usuário admin na inicialização, se não existir
├── controller/
│   └── HelloWorld.java               # GET / -> "Hello World!"
├── domain/user/
│   ├── User.java                     # entidade de usuário (implementa UserDetails)
│   └── UserRole.java                 # papéis: ADMIN, USER
├── repository/
│   └── UserRepository.java           # acesso a usuários (findByLogin, existsByLogin)
└── services/
    └── AuthorizationService.java     # UserDetailsService (carrega usuário por login)
```

## Domínio atual

### Usuário (`users`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `login` | String | email do usuário, usado como username |
| `password` | String | hash BCrypt |
| `role` | `UserRole` | `ADMIN` ou `USER`, gravado como texto (`@Enumerated(EnumType.STRING)`) |

O papel `ADMIN` recebe as authorities `ROLE_ADMIN` e `ROLE_USER`. O papel `USER` recebe só `ROLE_USER`.

## Segurança

- API **stateless**: sem sessão no servidor e com CSRF desabilitado.
- Senhas com hash **BCrypt** (bean `PasswordEncoder` em `SecurityConfiguration`).
- `POST /product` exige `ROLE_ADMIN`. Essa regra ficou da configuração inicial e a rota ainda não existe.
- **Todas as outras rotas estão liberadas (`permitAll`), temporariamente,** até existir a autenticação JWT. Depois disso, voltam para `.authenticated()`.

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/` | Retorna "Hello World!" | público |

## Roadmap

### Usuários e autenticação
- [x] Corrigir `User.getPassword()`, que retornava `""`
- [x] Adicionar `@NoArgsConstructor` em `User` (o JPA exige)
- [x] Mapear `role` com `@Enumerated(EnumType.STRING)`
- [x] `PasswordEncoder` (BCrypt)
- [x] Seed do usuário admin via `.env` (`AdminSeeder`)
- [ ] Lançar `UsernameNotFoundException` quando o login não existir
- [ ] Endpoints `POST /auth/register` e `POST /auth/login`
- [ ] Geração e validação de token JWT, com filtro de segurança
- [ ] Voltar `anyRequest()` para `.authenticated()` depois do JWT

### Imagens e feed
- [ ] Entidade de publicação (imagem + metadados + autor + data)
- [ ] Upload de imagem com metadados
- [ ] Armazenamento das imagens (disco local / object storage, a definir)
- [ ] Endpoint de feed paginado
- [ ] Remover a regra legada de `/product` da configuração de segurança
