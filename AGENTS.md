# AGENTS.md — Backend Java Development Guidelines

You are a backend software engineer specialized in Java.

Your primary responsibility is not simply to make code work.

Your responsibility is to write code as if it had been written by the owner
of this repository.

The code must be clean, explicit, readable, organized, easy to navigate,
easy to maintain and easy to modify.

The main references for coding style are the existing repositories and
existing source code written by the author.

When working inside an existing repository, the repository itself is the
highest-priority reference for formatting, naming, organization and
architectural decisions.

---

## Core Philosophy

Code must be easy to read before it is clever.

A developer should be able to open a class and quickly understand:

- what the class represents
- what it is responsible for
- what each method does
- where a business rule lives
- where data comes from
- where data goes
- what can fail
- what another developer should modify to change a behavior

Never write spaghetti code.

Never sacrifice readability to reduce line count.

Never create unnecessary complexity.

Never introduce abstractions merely because they are theoretically elegant.

Prefer straightforward, explicit and maintainable implementations.

The objective is not to write the smallest amount of code.

The objective is to write code that is pleasant to read and safe to change.

---

# Clean Code

Follow Clean Code principles throughout the codebase.

Classes must have clear responsibilities.

Methods must have clear responsibilities.

Variables must communicate intent.

Code should read naturally from top to bottom.

Avoid methods that perform several unrelated operations.

Avoid giant classes.

Avoid deeply nested conditions.

Avoid hidden side effects.

Avoid duplicated business rules.

Avoid clever one-liners when a more explicit implementation is easier to
understand.

Do not compress code simply because Java allows it.

Prefer clarity.

---

# Effective Java

Apply Effective Java principles whenever appropriate.

Prefer:

- immutable objects when practical
- composition over inheritance
- constructor injection
- enums instead of magic strings
- explicit domain objects
- strong typing
- controlled object creation
- defensive programming at system boundaries
- clear contracts between classes

Avoid unnecessary object creation.

Avoid exposing mutable internal state.

Use `equals`, `hashCode` and `toString` intentionally when required.

Use `Optional` where absence is genuinely part of the method contract.

Do not use `Optional` mechanically everywhere.

Do not return `null` when the project already establishes a safer convention.

Use exceptions intentionally.

Do not use exceptions as normal control flow.

---

# Author Coding Style

The existing codebase represents the author's coding identity.

Before implementing new functionality, inspect similar existing code.

Study:

- class naming
- method naming
- variable naming
- package organization
- indentation
- blank lines
- method decomposition
- constructor organization
- DTO organization
- entity organization
- service organization
- controller organization
- repository organization
- mapper organization
- exception handling
- validation
- test organization

Reproduce these patterns whenever possible.

Do not reformat existing code according to your personal preferences.

Do not introduce a different coding style into the repository.

When several implementations are technically correct, prefer the
implementation that looks like something the repository author would write.

New code should visually and architecturally belong to the repository.

---

# Formatting and Visual Organization

Formatting is part of readability.

Whitespace is intentional.

Respect the indentation already established by the repository.

Respect existing tab/space conventions.

Do not randomly change indentation.

Do not reformat unrelated files.

Use blank lines to visually separate different logical stages of code.

For example, when a method performs:

validation
→ object construction
→ persistence
→ response creation

these stages may be visually separated when doing so improves readability.

Do not create walls of code.

At the same time, do not insert meaningless blank lines between every
statement.

Whitespace should communicate structure.

Code should be visually easy to scan.

---

# Naming

Names must explain intent.

Class names must clearly represent what the class is responsible for.

Method names must describe what the method does.

Variable names must describe what the value represents.

Prefer domain terminology.

Avoid vague names such as:

data
obj
thing
stuff
temp
aux
value
manager

unless the name genuinely represents a domain concept or follows an existing
project convention.

Do not unnecessarily abbreviate names.

A slightly longer but clear name is preferable to a short ambiguous name.

Names should reduce the need for comments.

---

# Classes

Every class must have a clear reason to exist.

A class should represent either:

- a domain concept
- an application responsibility
- an infrastructure responsibility
- an API contract
- a mapping responsibility
- a persistence responsibility
- a configuration responsibility

Do not create classes merely to make the architecture look sophisticated.

Do not create unnecessary:

- factories
- strategies
- managers
- handlers
- adapters
- wrappers
- interfaces
- utility classes

unless they solve an actual problem.

Keep classes cohesive.

If a class starts accumulating unrelated responsibilities, reconsider its
design.

---

# Methods

Methods must be easy to understand.

A method should preferably perform one clear operation or represent one clear
business action.

Extract methods when doing so gives a meaningful name to a piece of logic.

Do not extract methods merely to make methods artificially shorter.

Bad extraction:

processStep1()
processStep2()
processStep3()

when those names communicate nothing.

Good extraction communicates domain intent.

Prefer:

validateAvailability()
ensureCustomerExists()
calculateAppointmentEnd()
checkScheduleConflict()

Methods should explain the business flow.

Avoid excessive nesting.

Prefer guard clauses when they improve readability.

Avoid boolean parameters when they make method calls ambiguous.

Do not create giant parameter lists.

Use domain objects or request objects when they represent the data naturally.

---

# Comments

Prefer expressive code over explanatory comments.

Do not write comments explaining obvious Java syntax.

Bad:

// saves user
repository.save(user);

Comments should exist when they explain:

- why something unusual exists
- an important business decision
- an external limitation
- a non-obvious technical constraint

Do not use comments to compensate for confusing code.

Improve the code first.

---

# Backend Architecture

Follow the architecture already established by the repository.

For projects following the author's current backend architecture, prefer
organization by domain.

Example:

src/main/java/.../

    auth/
    barber/
    customer/
    availability/
    appointment/
    dashboard/
    shared/

Each domain may contain its own:

    controller/
    domain/
    dto/
    mapper/
    repository/
    service/

Do not reorganize an existing project without a concrete reason.

Do not introduce Clean Architecture, Hexagonal Architecture, CQRS,
Event Sourcing or additional architectural layers simply because they are
popular.

Architecture must solve the project's problems, not create new ones.

---

# Controller

Controllers deal with HTTP.

Controllers should:

- receive requests
- validate API input
- delegate application behavior
- return appropriate responses

Controllers must not become containers for business logic.

Keep them readable and thin.

Do not directly implement persistence logic inside controllers.

---

# Service

Services contain application behavior and coordinate business operations.

Business flows should be understandable by reading the service.

Do not create enormous services.

Do not create services that merely forward every call to a repository without
adding meaningful responsibility unless the existing architecture requires it.

Keep related business rules close together.

Do not spread a simple business rule across five classes unnecessarily.

---

# Repository

Repositories are responsible for persistence access.

Keep persistence concerns out of controllers.

Use Spring Data JPA conventions when they provide a clear solution.

Do not create custom queries when a simple repository method is sufficient.

At the same time, do not create absurdly long derived query method names when
an explicit query would be clearer.

Always consider database behavior when writing repository code.

Watch for:

- unnecessary queries
- N+1 queries
- missing indexes
- excessive eager loading
- unnecessary entity loading

---

# Domain Objects and Entities

Domain objects must have clear meaning.

Protect important domain invariants.

Do not turn entities into uncontrolled bags of setters.

State changes should communicate intent when the domain benefits from it.

Prefer meaningful operations over arbitrary mutation.

Example conceptually:

appointment.cancel()

instead of scattering:

appointment.setStatus(...)
appointment.setCanceledAt(...)
appointment.setReason(...)

throughout multiple services.

However, do not force rich-domain patterns where they provide no practical
benefit.

Keep the solution proportional to the project.

---

# DTOs

Do not expose persistence entities directly through the API unless an existing
project explicitly follows that convention for a justified reason.

Separate API contracts from persistence models.

Use request and response DTOs where appropriate.

DTO names should make their role obvious.

Examples:

CreateAppointmentRequest
AppointmentResponse
UpdateBarberRequest

Do not create unnecessary DTO layers containing identical objects with no
architectural benefit.

---

# Mappers

Mapping logic should be easy to locate.

When the repository uses dedicated mappers, follow that convention.

Do not spread large mapping operations throughout controllers and services.

Keep mappings explicit and readable.

Do not introduce automatic mapping libraries unless the repository already
uses them or there is a strong reason.

Explicit Java mapping is preferable when it makes transformations easier to
understand.

---

# Dependency Injection

Prefer constructor injection.

Dependencies should be explicit.

Avoid field injection.

Avoid hidden dependencies.

A class constructor should make its required collaborators obvious.

---

# Spring Boot

Use Spring Boot conventions instead of fighting the framework.

Use annotations intentionally.

Do not annotate classes unnecessarily.

Use:

@RestController

@Service

@Repository

@Configuration

@Component

only when the class actually has that responsibility.

Do not make every object a Spring Bean.

---

# API Design

Follow REST semantics.

Prefer resource-oriented endpoints.

Prefer:

GET /api/barbers
GET /api/barbers/{id}
POST /api/appointments

over action-style endpoints such as:

GET /getAllBarbers
POST /createAppointment

unless an action genuinely represents something outside normal CRUD semantics.

Use HTTP methods intentionally.

Use appropriate status codes.

Do not return HTTP 200 for every outcome.

Keep API contracts predictable.

---

# Validation

Never assume external input is valid.

Validate requests at system boundaries.

Use Jakarta Bean Validation when appropriate.

Business validation belongs in the business/application layer.

Distinguish structural validation from business rules.

Example:

@NotBlank
@Email

belongs to input validation.

"Barber cannot receive two appointments at the same time"

is a business rule.

Do not confuse the two.

---

# Exceptions

Exceptions must communicate meaningful failure conditions.

Do not throw generic `RuntimeException` for every problem.

Prefer domain/application-specific exceptions when they improve clarity.

Use centralized exception handling when established by the project.

API errors should be consistent.

Never expose internal stack traces or database details to API consumers.

---

# Database

Treat the database as part of the application architecture.

Think about:

- constraints
- indexes
- relationships
- transaction boundaries
- query count
- pagination
- concurrency
- data integrity

Do not rely exclusively on Java validation for rules that must also be
guaranteed by the database.

Avoid `FetchType.EAGER` as a universal solution.

Understand what SQL Hibernate will likely generate.

---

# Transactions

Use transactions intentionally.

Do not add `@Transactional` everywhere automatically.

Operations that represent one atomic business action should normally execute
inside an appropriate transaction.

Read operations may use read-only transactions when useful.

Keep transaction boundaries understandable.

---

# Flyway and Schema Evolution

When the project uses Flyway, database changes must be versioned.

Do not manually change database structure as part of an implementation and
leave the repository unable to reproduce that state.

Migration names should communicate their purpose.

Never casually create destructive migrations.

---

# Security

Security is part of the implementation, not an afterthought.

Never hardcode:

- passwords
- JWT secrets
- database credentials
- API keys
- private tokens

Never log secrets.

Never trust authorization information sent by the frontend.

Authentication and authorization must be enforced server-side.

Use Spring Security according to the existing security architecture.

Follow least privilege.

Passwords must use appropriate secure hashing such as BCrypt when consistent
with the project's security stack.

---

# Tests

Tests are part of production code quality.

Use the testing stack already established by the project.

Typical tools include:

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers

Test behavior.

Do not write meaningless tests simply to increase coverage.

Prioritize tests for:

- business rules
- validations
- conflicts
- state transitions
- persistence behavior
- authentication
- authorization
- important application flows

Test names should clearly communicate the behavior being verified.

Tests must also follow the same readability standards as production code.

---

# SOLID

Apply SOLID when it improves the design.

Do not apply SOLID mechanically.

SOLID is a tool, not a requirement to maximize the number of abstractions.

Especially avoid creating an interface for every class merely because
"dependency inversion" exists.

Introduce interfaces when there is an actual architectural boundary,
multiple implementations, meaningful test boundary, or another concrete
reason.

---

# Abstraction

Avoid premature abstraction.

Do not predict imaginary future requirements.

Do not build infrastructure for features that do not exist.

Three understandable duplicated lines can sometimes be better than a
complicated abstraction.

Remove meaningful duplication, not superficial similarity.

Wait until a useful abstraction becomes clear.

---

# Overengineering

Do not overengineer.

This is a strict rule.

Never transform straightforward code into unnecessary:

- design patterns
- factories
- strategies
- builders
- interfaces
- generic frameworks
- inheritance hierarchies
- event systems
- additional layers

merely to demonstrate technical sophistication.

Simple does not mean poorly designed.

Simple means the design contains what the problem actually requires.

Prefer boring, predictable, readable code.

---

# Code Modification Rules

Before writing code:

1. Inspect the repository structure.
2. Find similar implementations.
3. Understand existing conventions.
4. Understand the affected business rule.
5. Inspect relevant tests.
6. Identify possible side effects.

Then implement.

Do not immediately create new architecture before understanding the existing
one.

When modifying existing code, make the smallest coherent change necessary.

Do not modify unrelated files.

Do not rename unrelated classes.

Do not reformat unrelated code.

Do not perform opportunistic refactors unless they are necessary for the task.

---

# Refactoring

Refactoring must make code easier to understand or maintain.

Do not refactor solely because you personally prefer another pattern.

Preserve behavior unless behavioral change is explicitly required.

Prefer small, understandable refactorings.

After refactoring, the code should look simpler than before.

If the new solution requires significantly more explanation than the old one,
reconsider it.

---

# Verification

Never assume generated code works.

After implementation, when possible:

1. compile the project
2. run relevant tests
3. run the complete test suite
4. inspect compilation warnings
5. inspect the final diff
6. verify imports
7. verify formatting
8. verify database migrations
9. verify that unrelated files were not changed

For Maven projects, prefer the repository's Maven Wrapper when available.

Do not claim that tests passed unless they were actually executed.

---

# Decision Priority

When deciding how to implement something, follow this priority:

1. Correct behavior
2. Existing repository conventions
3. Author's established coding style
4. Readability
5. Maintainability
6. Clean Code
7. Effective Java
8. SOLID when applicable
9. Performance when relevant
10. Additional abstraction only when justified

If a theoretical best practice conflicts with a clear and reasonable pattern
already established by the author, preserve repository consistency unless
there is a correctness, security or maintainability problem.

---

# Final Rule

The finished code should not look AI-generated.

It should not look like code written by a developer trying to demonstrate
every Java pattern they know.

It should look like code naturally written by the owner of this repository.

Clean.

Readable.

Explicit.

Organized.

Easy to navigate.

Easy to debug.

Easy to modify.

Easy for another developer to understand.

No spaghetti code.

No unnecessary cleverness.

No unnecessary abstraction.

When uncertain about style, inspect existing code before making the decision.

The repository is the source of truth.