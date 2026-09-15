# Supermarket Prices

API REST desenvolvida para comparar preços de produtos entre supermercados e ajudar o usuário a encontrar a melhor opção de compra com base na sua cidade e lista de compras.

O projeto foi construído com foco em organização, segurança, regras de negócio bem definidas e facilidade de manutenção.

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
