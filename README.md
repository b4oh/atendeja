# AtendeJá

Sistema web para organização de filas, senhas, atendimentos, agendamentos e notificações SMS em unidades de saúde.

## Stack

- Java 21
- Spring Boot 3.5
- Spring MVC, Spring Security, Spring Data JPA
- Thymeleaf
- PostgreSQL
- AWS SDK for Java, com envio de SMS via Amazon SNS
- Maven

## Banco de dados

O script oficial do banco está em:

```text
database/atendeja_banco_v2.sql
```

Para criar o banco localmente:

```powershell
createdb -U postgres atendeja
psql -U postgres -d atendeja -f database/atendeja_banco_v2.sql
```

Também é possível criar o banco `atendeja` pelo pgAdmin e executar o conteúdo do script pela ferramenta de query.

## Variáveis de ambiente

O projeto usa variáveis de ambiente para evitar credenciais fixas no código. Para desenvolvimento local, crie um arquivo `.env` na raiz do projeto com base no `.env.example`.

Exemplo:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/atendeja
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres123

AWS_ACCESS_KEY_ID=sua_access_key
AWS_SECRET_ACCESS_KEY=sua_secret_key
AWS_REGION=us-east-1
```

Nunca publique chaves da AWS, senhas de banco ou arquivos `.env` no GitHub.

## Como rodar

Com o banco criado e o `.env` preenchido:

```powershell
mvn spring-boot:run
```

A aplicação fica disponível em:

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

## Teste de SMS

Para testar SMS:

1. Configure `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` e `AWS_REGION`.
2. No console da AWS, confirme que a conta tem acesso ao serviço de SMS.
3. Se a conta estiver em sandbox, cadastre e valide o número de destino.
4. Inicie a aplicação e gere uma senha com celular informado.
5. Confira o registro na tabela `notificacao` e o log da aplicação.

Quando o envio é aceito pela AWS, o sistema registra o status como `ENVIADO` e exibe o `MessageId`. Se a AWS bloquear por sandbox, região, permissão ou assinatura do serviço, o status fica como `FALHOU` e o erro aparece no log.

## Observações para GitHub público

- O arquivo `.gitignore` já bloqueia `.env`, logs, temporários, `target/`, `.idea/` e `.vscode/`.
- O script SQL contém apenas usuários de teste com senha BCrypt.
- Credenciais reais devem ser configuradas somente no ambiente local ou no serviço de hospedagem.
