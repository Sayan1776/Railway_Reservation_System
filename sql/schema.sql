-- =====================================================================
-- Railway Reservation System: schema for Supabase (PostgreSQL)
-- Run in Supabase: SQL Editor > New query > paste > Run.
-- Keep this file in sql/ as the single source of truth for the schema.
-- =====================================================================

-- DEV RESET: uncomment ONLY to wipe everything. The database is shared by the team.
-- drop table if exists payments, passengers, tickets, seat_availability,
--                      train_classes, train_stops, trains, stations, users cascade;

-- ---------------------------------------------------------------------
-- users: both roles live here (Person -> User / Admin in Java)
-- password_hash and salt are produced in AuthService (SHA-256 + random salt).
-- Never store plain passwords.
-- ---------------------------------------------------------------------
create table users (
    user_id        bigint generated always as identity primary key,
    full_name      text        not null,
    email          text        not null unique,          -- lowercase it in Java before insert/lookup
    phone          text        not null check (phone ~ '^[0-9]{10}$'),
    password_hash  text        not null,
    salt           text        not null,
    role           text        not null default 'USER' check (role in ('USER', 'ADMIN')),
    created_at     timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- stations
-- ---------------------------------------------------------------------
create table stations (
    station_code   text primary key check (station_code = upper(station_code)),
    station_name   text not null,
    city           text not null
);

-- ---------------------------------------------------------------------
-- trains: identity only. Source/destination are the first/last stop.
-- is_active is a soft delete: trains with tickets can't be hard-deleted.
-- runs_on is 7 chars, Mon..Sun, e.g. 'YNYNYNY' = Mon, Wed, Fri, Sun.
-- ---------------------------------------------------------------------
create table trains (
    train_number   int  primary key,
    train_name     text not null,
    runs_on        text not null default 'YYYYYYY' check (runs_on ~ '^[YN]{7}$'),
    is_active      boolean not null default true
);

-- ---------------------------------------------------------------------
-- train_stops: the Route. One row per station on a train's path.
-- distance_km is measured from the origin; fare = distance * fare_per_km.
-- day_offset: 0 = same day as departure from origin, 1 = next day, etc.
-- ---------------------------------------------------------------------
create table train_stops (
    train_number    int      not null references trains(train_number) on delete cascade,
    stop_order      int      not null check (stop_order > 0),
    station_code    text     not null references stations(station_code),
    arrival_time    time,                                  -- null at the origin
    departure_time  time,                                  -- null at the terminus
    day_offset      smallint not null default 0 check (day_offset >= 0),
    distance_km     int      not null check (distance_km >= 0),
    primary key (train_number, stop_order),
    unique (train_number, station_code)
);

-- ---------------------------------------------------------------------
-- train_classes: which classes a train has, capacity, and price per km
-- ---------------------------------------------------------------------
create table train_classes (
    train_number   int           not null references trains(train_number) on delete cascade,
    seat_class     text          not null check (seat_class in ('SL', '3A', '2A', '1A')),
    total_seats    int           not null check (total_seats > 0),
    fare_per_km    numeric(6,2)  not null check (fare_per_km > 0),
    primary key (train_number, seat_class)
);

-- ---------------------------------------------------------------------
-- seat_availability: seats left PER TRAIN, CLASS and DATE.
-- Rows are created lazily the first time a date is searched or booked
-- (see the "insert ... on conflict do nothing" query in the notes).
-- The CHECK is what stops overbooking at the database level.
-- ---------------------------------------------------------------------
create table seat_availability (
    train_number     int  not null,
    seat_class       text not null,
    journey_date     date not null,
    available_seats  int  not null check (available_seats >= 0),
    primary key (train_number, seat_class, journey_date),
    foreign key (train_number, seat_class)
        references train_classes (train_number, seat_class) on delete cascade
);

-- ---------------------------------------------------------------------
-- tickets: one row per booking (PNR). Passengers hang off it.
-- ---------------------------------------------------------------------
create table tickets (
    pnr            text          primary key check (pnr ~ '^[0-9]{10}$'),
    user_id        bigint        not null references users(user_id),
    train_number   int           not null,
    seat_class     text          not null,
    journey_date   date          not null,
    from_station   text          not null references stations(station_code),
    to_station     text          not null references stations(station_code),
    status         text          not null default 'CONFIRMED'
                                 check (status in ('CONFIRMED', 'WAITING', 'CANCELLED')),
    total_fare     numeric(10,2) not null check (total_fare >= 0),
    refund_amount  numeric(10,2) check (refund_amount >= 0),
    booked_at      timestamptz   not null default now(),
    cancelled_at   timestamptz,
    check (from_station <> to_station),
    foreign key (train_number, seat_class)
        references train_classes (train_number, seat_class)
);

-- ---------------------------------------------------------------------
-- passengers: 1 to 6 per ticket
-- ---------------------------------------------------------------------
create table passengers (
    passenger_id      bigint generated always as identity primary key,
    pnr               text not null references tickets(pnr) on delete cascade,
    full_name         text not null,
    age               int  not null check (age between 1 and 120),
    gender            text not null check (gender in ('M', 'F', 'O')),
    berth_preference  text check (berth_preference in
                          ('LOWER', 'MIDDLE', 'UPPER', 'SIDE_LOWER', 'SIDE_UPPER', 'NONE')),
    seat_label        text                                   -- e.g. 'S4-23'; null while waiting
);

-- ---------------------------------------------------------------------
-- payments (simulated in Java)
-- ---------------------------------------------------------------------
create table payments (
    payment_id   bigint generated always as identity primary key,
    pnr          text          not null references tickets(pnr),
    amount       numeric(10,2) not null check (amount > 0),
    method       text          not null check (method in ('UPI', 'CARD', 'NETBANKING', 'WALLET')),
    status       text          not null default 'SUCCESS'
                               check (status in ('SUCCESS', 'FAILED', 'REFUNDED')),
    paid_at      timestamptz   not null default now()
);

-- ---------------------------------------------------------------------
-- indexes (primary keys and unique constraints already have one)
-- ---------------------------------------------------------------------
create index idx_stops_station   on train_stops (station_code);
create index idx_tickets_user    on tickets (user_id);
create index idx_tickets_train   on tickets (train_number, journey_date);
create index idx_passengers_pnr  on passengers (pnr);
create index idx_payments_pnr    on payments (pnr);

-- ---------------------------------------------------------------------
-- Row Level Security: Supabase exposes public tables through its REST API.
-- With RLS on and no policies, that API can read nothing. Your Java app
-- connects as the postgres role, which bypasses RLS, so it is unaffected.
-- ---------------------------------------------------------------------
alter table users              enable row level security;
alter table stations           enable row level security;
alter table trains             enable row level security;
alter table train_stops        enable row level security;
alter table train_classes      enable row level security;
alter table seat_availability  enable row level security;
alter table tickets            enable row level security;
alter table passengers         enable row level security;
alter table payments           enable row level security;