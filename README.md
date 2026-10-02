# 🚂 Railway Reservation System

A console-based Railway Reservation System built in **Java** with **PostgreSQL** (Supabase) as the backend database. Features user registration/login, train search, ticket booking, cancellation with refund policy, and an admin panel.

---

## ✨ Features

- **User Registration & Login** — Secure password hashing with PBKDF2 + salt
- **Train Search** — Search trains by source, destination, and date
- **Seat Availability** — Real-time seat availability with atomic booking (no double-booking)
- **Ticket Booking** — Book up to 6 passengers per ticket with fare calculation
- **Ticket Cancellation** — Time-based refund policy (90% / 50% / 25% / 0%)
- **PNR Check** — Look up any booking by PNR number
- **Admin Panel** — View all trains, stations, look up/cancel any ticket
- **Beautiful Console UI** — Box-drawing characters, ANSI colors, loading animations
- **Connection Pooling** — Efficient reuse of database connections

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21+ |
| Database | PostgreSQL (Supabase) |
| JDBC Driver | PostgreSQL 42.7.13 |
| Build | NetBeans / Manual javac |
| Architecture | Layered (UI → Service → Repository → DB) |

---

## 📂 Project Structure

```
src/com/railway/
├── Main.java                    # Entry point, wires everything together
├── model/                       # POJOs: Person, User, Admin, Train, Ticket, etc.
├── repository/                  # Data access interfaces
│   └── jdbc/                    # JDBC implementations + connection pool
├── service/                     # Business logic (Auth, Booking, Cancellation, Fare)
├── ui/                          # Console UI (ConsoleHelper, AdminMenu, UserMenu)
├── util/                        # Utilities (Validator, DateUtil, PasswordHasher, PNR)
└── exception/                   # Custom exceptions
sql/
├── schema.sql                   # Database schema (run first)
└── seed.sql                     # Sample data (run after schema)
```

---

## 🚀 Setup & Run

### Prerequisites
- Java 21 or higher
- A Supabase project (free tier works)

### Steps

1. **Set up the database:**
   - Open the Supabase SQL Editor
   - Run `sql/schema.sql` to create all tables
   - Run `sql/seed.sql` to insert sample trains and stations

2. **Configure database credentials:**
   - Copy `db.properties.example` to `db.properties`
   - Fill in your Supabase connection URL, username, and password

3. **Compile:**
   ```bash
   # On Windows (PowerShell)
   $files = Get-ChildItem -Path src -Filter *.java -Recurse | Select-Object -ExpandProperty FullName
   javac -cp "lib/*" -d out $files
   ```

4. **Run:**
   ```bash
   java -cp "out;lib/*" com.railway.Main
   ```

---

## 🔐 Security

- Passwords are **never stored in plain text**
- PBKDF2WithHmacSHA256 with 600,000 iterations + random salt
- Constant-time hash comparison to prevent timing attacks
- Login uses a dummy hash for unknown emails to prevent user enumeration
- Row Level Security enabled on all Supabase tables

---

## 🎯 OOP Concepts Used

See [`docs/oop-concept-map.md`](docs/oop-concept-map.md) for a detailed mapping of every OOP concept to its implementation.

- **Encapsulation** — Private fields, public getters/setters with validation
- **Inheritance** — `Person` → `User` / `Admin`
- **Polymorphism** — Interface-based dispatch (`FareCalculator`), method overriding (`getRole()`)
- **Abstraction** — Repository interfaces hide JDBC details from services
- **Composition** — `Train` has `Route` has `RouteStop` has `Station`
- **Design Patterns** — Strategy, Repository, Dependency Injection, Object Pool

---

## 📝 License

This project is for educational purposes.
