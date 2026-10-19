# alcoholic-rat

SaaS para publicação de imagens com metadados. O usuário envia uma imagem acompanhada de metadados, o sistema armazena ambos e, a partir deles, monta um **feed** com as imagens e suas informações.

> **Status:** estágio inicial. Por enquanto existe só a base de autenticação e autorização de usuários. Upload de imagens, metadados e feed ainda não foram implementados.

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

Para subir um Postgres rápido com Docker:

```bash
docker run -d --name alcoholic-rat-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=alcoholic-rat \
  -p 5432:5432 postgres
```

### Executar a aplicação

```bash
./mvnw spring-boot:run
```

### Rodar os testes

```bash
./mvnw test
```

> O teste `contextLoads` sobe o contexto completo, então precisa do Postgres disponível.

## Configuração

Fica em `src/main/resources/application.properties`:

| Propriedade | Valor atual |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/alcoholic-rat` |
| `spring.datasource.username` / `password` | `postgres` / `postgres` |
| `spring.jpa.hibernate.ddl-auto` | `update` (o schema é criado e atualizado automaticamente) |
| `spring.jpa.show-sql` | `true` |

## Estrutura

```
src/main/java/com/rosadavi/alcoholic_rat/
├── AlcoholicRatApplication.java      # entrada da aplicação
├── auth/security/
│   └── SecurityConfiguration.java    # cadeia de filtros do Spring Security
├── controller/
│   └── HelloWorld.java               # GET / -> "Hello World!"
├── domain/user/
│   ├── User.java                     # entidade de usuário (implementa UserDetails)
│   └── UserRole.java                 # papéis: ADMIN, USER
├── repository/
│   └── UserRepository.java           # acesso a usuários (findByLogin)
└── services/
    └── AuthorizationService.java     # UserDetailsService (carrega usuário por login)
```

## Domínio atual

### Usuário (`users`)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | UUID (String) | gerado automaticamente |
| `login` | String | usado como username |
| `password` | String | |
| `role` | `UserRole` | `ADMIN` ou `USER` |

O papel `ADMIN` recebe as authorities `ROLE_ADMIN` e `ROLE_USER`. O papel `USER` recebe só `ROLE_USER`.

## Segurança

- API **stateless**: sem sessão no servidor e com CSRF desabilitado.
- `POST /product` exige `ROLE_ADMIN`. Essa regra ficou da configuração inicial e a rota ainda não existe.
- Todas as outras rotas exigem autenticação.
- O mecanismo de autenticação (token JWT) ainda não foi implementado, então hoje nenhuma requisição consegue se autenticar.

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/` | Retorna "Hello World!" | autenticado |

## Roadmap

### Autenticação
- [ ] Corrigir `User.getPassword()`, que hoje retorna `""`
- [ ] Adicionar `@NoArgsConstructor` em `User` (o JPA exige)
- [ ] Mapear `role` com `@Enumerated(EnumType.STRING)`
- [ ] Lançar `UsernameNotFoundException` quando o login não existir
- [ ] `PasswordEncoder` (BCrypt)
- [ ] Endpoints `POST /auth/register` e `POST /auth/login`
- [ ] Geração e validação de token JWT, com filtro de segurança

### Imagens e feed
- [ ] Entidade de publicação (imagem + metadados + autor + data)
- [ ] Upload de imagem com metadados
- [ ] Armazenamento das imagens (disco local / object storage, a definir)
- [ ] Endpoint de feed paginado
- [ ] Remover a regra legada de `/product` da configuração de segurança
