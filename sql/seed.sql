-- =====================================================================
-- Sample data for testing. NOT a real timetable: trains, times,
-- distances and fares are made up. Run AFTER schema.sql.
-- No admin user here: AuthService creates the default admin on first
-- run so the password hash matches your Java hashing code.
-- =====================================================================

insert into stations (station_code, station_name, city) values
    ('HWH',  'Howrah Junction',          'Kolkata'),
    ('ASN',  'Asansol Junction',         'Asansol'),
    ('DHN',  'Dhanbad Junction',         'Dhanbad'),
    ('GAYA', 'Gaya Junction',            'Gaya'),
    ('DDU',  'Pt. Deen Dayal Upadhyaya Junction', 'Mughalsarai'),
    ('CNB',  'Kanpur Central',           'Kanpur'),
    ('NDLS', 'New Delhi',                'Delhi'),
    ('PNBE', 'Patna Junction',           'Patna');

insert into trains (train_number, train_name, runs_on) values
    (10001, 'Kolkata-Delhi Express (sample)', 'YYYYYYY'),
    (10002, 'Howrah-Patna Express (sample)',  'YNYNYNY');

-- Train 10001: Howrah -> New Delhi, daily
insert into train_stops (train_number, stop_order, station_code, arrival_time, departure_time, day_offset, distance_km) values
    (10001, 1, 'HWH',  null,    '16:50', 0,    0),
    (10001, 2, 'ASN',  '19:15', '19:17', 0,  200),
    (10001, 3, 'DHN',  '20:20', '20:25', 0,  259),
    (10001, 4, 'GAYA', '23:10', '23:15', 0,  458),
    (10001, 5, 'DDU',  '01:30', '01:35', 1,  679),
    (10001, 6, 'CNB',  '06:15', '06:20', 1, 1000),
    (10001, 7, 'NDLS', '10:30', null,    1, 1450);

-- Train 10002: Howrah -> Patna, Mon/Wed/Fri/Sun
insert into train_stops (train_number, stop_order, station_code, arrival_time, departure_time, day_offset, distance_km) values
    (10002, 1, 'HWH',  null,    '21:00', 0,   0),
    (10002, 2, 'ASN',  '23:30', '23:32', 0, 200),
    (10002, 3, 'DHN',  '00:35', '00:40', 1, 259),
    (10002, 4, 'GAYA', '03:10', '03:15', 1, 458),
    (10002, 5, 'PNBE', '05:30', null,    1, 560);

insert into train_classes (train_number, seat_class, total_seats, fare_per_km) values
    (10001, 'SL', 180, 0.55),
    (10001, '3A', 120, 1.40),
    (10001, '2A',  48, 2.00),
    (10001, '1A',  18, 3.50),
    (10002, 'SL', 180, 0.55),
    (10002, '3A', 120, 1.40),
    (10002, '2A',  48, 2.00);