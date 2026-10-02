# OOP Concept Map: Railway Reservation System

This document maps every OOP concept used in the project to its concrete implementation.

---

## 1. Encapsulation

| Where | How |
|---|---|
| `Person`, `Ticket`, `Train`, `Payment` | All fields are `private`/`private final`; access only through getters/setters. Setters validate via `Require` helper. |
| `PasswordHasher` | The hash algorithm, salt generation, and constant-time comparison are hidden behind a static API. Callers never see raw bytes. |
| `DBConnection` | Connection pooling internals are hidden. Callers just call `getConnection()`. |
| `ConsoleHelper` | The `Scanner` instance is private. No other class touches `System.in` directly. |

## 2. Inheritance

| Parent | Children | Purpose |
|---|---|---|
| `Person` (abstract) | `User`, `Admin` | Shared identity fields (name, email, phone, password hash). Each subclass overrides `getRole()`. |
| `RuntimeException` | `DataAccessException`, `InvalidInputException`, `AuthenticationException`, etc. | Custom exception hierarchy for clean error handling. |

## 3. Polymorphism

| Type | Where |
|---|---|
| **Runtime (override)** | `Person.getRole()` — `User` returns `USER`, `Admin` returns `ADMIN`. `Main.java` calls `person.isAdmin()` on the base type to decide which menu to show. |
| **Interface** | `FareCalculator` interface with `DistanceFareCalculator` implementation. `BookingService` depends on the interface, not the concrete class. |
| **Enum polymorphism** | `SeatClass`, `BookingStatus`, `Payment.Method` — behaviour encoded in enum constants. |

## 4. Abstraction

| Abstraction | Concrete |
|---|---|
| `FareCalculator` (interface) | `DistanceFareCalculator` |
| `TrainRepository` (interface) | `JdbcTrainRepository` |
| `TicketRepository` (interface) | `JdbcTicketRepository` |
| `UserRepository` (interface) | `JdbcUserRepository` |
| `Person` (abstract class) | `User`, `Admin` |

## 5. Composition & Aggregation

| Owner | Part | Relationship |
|---|---|---|
| `Train` | `Route`, `TrainClass` (collection) | A train *has-a* route and *has* classes. |
| `Route` | `RouteStop` (ordered list) | A route *is composed of* stops. |
| `RouteStop` | `Station` | Each stop *has-a* station reference. |
| `Ticket` | `Passenger` (list) | A ticket *contains* 1–6 passengers. |
| `BookingService` | `TrainRepository`, `TicketRepository`, `FareCalculator`, `PaymentService` | Dependencies injected via constructor. |

## 6. Design Patterns

| Pattern | Where |
|---|---|
| **Strategy** | `FareCalculator` interface → swap pricing logic without changing `BookingService`. |
| **Repository** | `TrainRepository`, `TicketRepository`, `UserRepository` — data access is behind interfaces. |
| **Dependency Injection** | `Main.java` constructs all objects and wires them via constructors (manual DI). |
| **Factory Method** | `PNRGenerator.generate()` — encapsulates PNR creation logic. |
| **Template Method** | `Person` defines the contract; subclasses fill in `getRole()`. |
| **Object Pool** | `DBConnection` / `PooledConnection` — reuses database connections. |

## 7. SOLID Principles

| Principle | Evidence |
|---|---|
| **S**ingle Responsibility | Each service class does one thing: `AuthService` authenticates, `BookingService` books, `CancellationService` cancels. |
| **O**pen/Closed | New fare strategies can be added without modifying `BookingService` (just implement `FareCalculator`). |
| **L**iskov Substitution | `User` and `Admin` are interchangeable wherever a `Person` is expected. |
| **I**nterface Segregation | Repository interfaces are small and focused (e.g., `TrainRepository` doesn't know about tickets). |
| **D**ependency Inversion | Services depend on repository *interfaces*, not on JDBC implementations. |

---

*Auto-generated concept map for the Railway Reservation System project.*
