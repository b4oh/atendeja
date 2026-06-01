# AtendeJá

Sistema web para organização de filas, emissão de senhas, atendimento em guichês, agendamento de exames, transparência pública, indicadores gerenciais e notificações SMS em unidades de saúde.

O projeto foi desenvolvido em Java com Spring Boot e segue uma arquitetura em camadas, com persistência em PostgreSQL e telas renderizadas com Thymeleaf.

## Objetivo

O AtendeJá simula um fluxo de atendimento em unidades de saúde, permitindo que:

- cidadãos emitam senhas pelo totem;
- atendentes chamem e finalizem atendimentos em guichês;
- o painel público exiba chamadas em tempo real;
- a população consulte informações no painel de transparência pública;
- cidadãos agendem exames;
- gestores acompanhem indicadores da unidade;
- o sistema envie notificações SMS via AWS SNS.

## Stack

- Java 21
- Spring Boot 3.5
- Spring MVC
- Spring Security
- Spring Data JPA / Hibernate
- Thymeleaf
- PostgreSQL
- AWS SDK for Java, com SMS via Amazon SNS
- SSE, Server-Sent Events
- Bootstrap e Bootstrap Icons
- Maven

## Funcionalidades

- Login com controle de acesso por perfil.
- Perfis de usuário: `ADMIN`, `GESTOR`, `ATENDENTE` e `CIDADAO`.
- Cadastro, edição e ativação/inativação de usuários.
- Cadastro, edição e ativação/inativação de unidades de saúde.
- Cadastro, edição e ativação/inativação de serviços por unidade.
- Totem público para emissão de senhas.
- Fila com prioridade e ordem de emissão.
- Painel do atendente para abrir guichê, chamar senha e finalizar atendimento.
- Atualização automática da fila do atendente via SSE.
- Painel público de chamadas com atualização em tempo real.
- Painel de transparência pública para consulta de agendamentos.
- Agendamento de exames com validação de datas, horários e vagas.
- Dashboard gerencial com indicadores do dia.
- Notificações SMS via AWS SNS.

## Arquitetura

O projeto segue a estrutura:

```text
Controller -> Service -> Repository -> PostgreSQL
```

Responsabilidades principais:

- `controller`: recebe requisições HTTP, chama os services e direciona as telas.
- `service`: concentra regras de negócio.
- `repository`: acessa o banco com Spring Data JPA.
- `model`: contém as entidades JPA que representam as tabelas.
- `enums`: define perfis, status e tipos fixos do domínio.
- `templates`: contém as telas Thymeleaf.
- `static`: contém arquivos estáticos, como CSS.

Entidades principais:

- `Usuario`
- `UnidadeSaude`
- `Servico`
- `Senha`
- `Guiche`
- `Atendimento`
- `Agendamento`
- `TipoExame`
- `Notificacao`

## Pré-requisitos

- Java 21
- Maven
- PostgreSQL
- Git
- Conta AWS com acesso ao SNS, apenas se desejar testar SMS

## Banco de dados

O script oficial do banco está em:

```text
database/atendeja_banco_v2.sql
```

Para criar e importar o banco localmente via terminal:

```powershell
createdb -U postgres atendeja
psql -U postgres -d atendeja -f database/atendeja_banco_v2.sql
```

Também é possível criar o banco `atendeja` pelo pgAdmin e executar o conteúdo do script pela ferramenta de query.

O projeto usa:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Isso significa que o Hibernate valida se o banco existe e está compatível com as entidades, mas não cria as tabelas automaticamente. Por isso, o script SQL deve ser importado antes de iniciar a aplicação.

## Variáveis de ambiente

O projeto usa variáveis de ambiente para evitar credenciais fixas no código.

Para desenvolvimento local, crie um arquivo `.env` na raiz do projeto com base no `.env.example`.

Exemplo:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/atendeja
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres123

AWS_ACCESS_KEY_ID=sua_access_key
AWS_SECRET_ACCESS_KEY=sua_secret_key
AWS_REGION=us-east-1
```

O arquivo `.env` real não deve ser enviado ao GitHub.

## Como rodar

Com o banco criado, o script SQL importado e o `.env` preenchido:

```powershell
mvn spring-boot:run
```

A aplicação ficará disponível em:

```text
http://localhost:8080
```

Para executar os testes:

```powershell
mvn test
```

## Usuários de teste

Todos os usuários abaixo usam a senha:

```text
123456
```

| Perfil | E-mail |
| --- | --- |
| ADMIN | admin@atendeja.al.gov.br |
| GESTOR | gestor@atendeja.al.gov.br |
| ATENDENTE | atendente@atendeja.al.gov.br |
| CIDADAO | cidadao@atendeja.al.gov.br |

## Rotas principais

| Rota | Descrição |
| --- | --- |
| `/login` | Login do sistema |
| `/dashboard` | Menu principal após login |
| `/admin/usuarios` | Gestão de usuários |
| `/admin/unidades` | Gestão de unidades de saúde |
| `/admin/unidades/{id}/servicos` | Gestão de serviços da unidade |
| `/totem` | Totem público para emissão de senha |
| `/senha/consultar` | Consulta pública de posição na fila |
| `/painel` | Seleção de unidade para painel público |
| `/painel/{unidadeId}` | Painel público de chamadas |
| `/transparencia` | Painel de transparência pública |
| `/atendente/home` | Seleção de guichê pelo atendente |
| `/gestor/home` | Entrada da área gerencial |
| `/agendamento` | Área de agendamentos |

## Fluxo principal

1. O cidadão acessa o totem.
2. Escolhe uma unidade ativa.
3. Escolhe um serviço disponível.
4. O sistema emite uma senha e salva no banco.
5. A fila do atendente é atualizada via SSE.
6. O atendente abre um guichê.
7. O atendente chama a próxima senha.
8. O painel público recebe a chamada em tempo real.
9. O atendente finaliza o atendimento.
10. O gestor acompanha os indicadores no dashboard.

## Transparência pública

A rota `/transparencia` exibe agendamentos de exames em uma tela pública, sem necessidade de login.

O painel permite filtrar por:

- unidade de saúde;
- data.

As informações exibidas incluem:

- cidadão;
- CPF parcialmente mascarado;
- exame;
- unidade;
- data;
- hora;
- status do agendamento.

Essa funcionalidade usa dados persistidos no PostgreSQL e é renderizada com Thymeleaf a partir dos dados enviados pelo controller.

## SMS com AWS SNS

O envio de SMS é feito pelo Amazon SNS.

Para testar:

1. Configure `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` e `AWS_REGION`.
2. No console da AWS, confirme que sua conta tem acesso ao serviço de SMS.
3. Se a conta estiver em sandbox, cadastre e valide o número de destino.
4. Inicie a aplicação.
5. Emita uma senha com celular informado.
6. Confira o log da aplicação e os registros na tabela `notificacao`.

Quando o envio é aceito pela AWS, o sistema registra a notificação como `ENVIADO` e exibe o `MessageId` no log.

Se a AWS bloquear por sandbox, região, permissão, assinatura do serviço ou credenciais inválidas, o sistema registra a notificação como `FALHOU`.

## Segurança

- Senhas de usuários são armazenadas com BCrypt.
- O controle de acesso é feito por Spring Security.
- Rotas administrativas exigem perfil `ADMIN`.
- Rotas de gestor exigem `ADMIN` ou `GESTOR`.
- Rotas de atendente exigem `ADMIN`, `GESTOR` ou `ATENDENTE`.
