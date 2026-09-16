# Gomo

API REST desenvolvida para comparar preços de produtos entre supermercados e ajudar o usuário a encontrar a melhor opção de compra com base na sua cidade e lista de compras.

O projeto foi construído com foco em organização, segurança, regras de negócio bem definidas e facilidade de manutenção.

Stack do backend: Java 21, Spring Boot 4.1.1, Maven Wrapper, Spring MVC, Data JPA, Security, Bean Validation, Actuator, PostgreSQL, Flyway e OpenAPI. A autenticação usa tokens opacos persistidos somente por hash. A interface Gomo fica em `frontend/` e utiliza React, TypeScript, Vite, Tailwind CSS e TanStack Query.

## Frontend Gomo

Com a API disponível em `http://localhost:8080`, execute:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

A interface fica em `http://localhost:5173`. Para permitir as chamadas locais, configure `CORS_ALLOWED_ORIGINS=http://localhost:5173` no backend. A URL da API pode ser alterada em `frontend/.env` por meio de `VITE_API_BASE_URL`.

O checkout e o paywall visual não simulam pagamento. Para concluir a assinatura, o backend ainda precisa oferecer consulta do status atual, criação e consulta de checkout, portal ou cancelamento e confirmação de pagamentos por webhook.

## Funcionalidades

* Cadastro e autenticação de usuários
* Verificação de e-mail e recuperação de senha
* Catálogo de produtos, redes e supermercados
* Histórico e comparação de preços
* Criação e gerenciamento de listas de compras
* Comparação completa de uma lista entre supermercados
* Recomendação da melhor opção de compra
* Preferência de cidade e lojas favoritas
* Alertas de preço
* Notificações internas
* Envio de preços pela comunidade
* Moderação de contribuições
* Controle de acesso para administradores
* Auditoria de operações administrativas

## Tecnologias

* Java 21
* Spring Boot 4
* Spring MVC
* Spring Data JPA
* Spring Security
* PostgreSQL
* Flyway
* Maven
* Docker
* OpenAPI
* Bean Validation
* Spring Boot Actuator

## Arquitetura

O projeto é organizado por funcionalidades, mantendo responsabilidades bem definidas entre as diferentes camadas da aplicação.

```text
Controller
    |
Service
    |
Repository
    |
PostgreSQL
```

Os controllers são responsáveis pela comunicação HTTP, os services concentram as regras de negócio e transações, enquanto os repositories realizam a persistência dos dados.

DTOs são utilizados para evitar a exposição direta das entidades da aplicação.

## Segurança

A API possui autenticação baseada em tokens opacos.

Os tokens são armazenados somente através de hash e possuem tempo de expiração.

Também estão implementados:

* BCrypt para armazenamento de senhas
* Controle de acesso por roles
* Rate limiting
* Revogação de sessões
* Tokens de uso único para recuperação de senha
* Verificação de e-ma
