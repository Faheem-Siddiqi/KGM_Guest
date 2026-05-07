CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role_name VARCHAR(50) NOT NULL DEFAULT 'admin',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS guest_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accommodation_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accommodations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    status VARCHAR(60) NOT NULL,
    assigned_staff VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_accommodations_category
        FOREIGN KEY (category_id)
        REFERENCES accommodation_categories (id)
);

CREATE TABLE IF NOT EXISTS accommodation_amenities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    accommodation_id BIGINT NOT NULL,
    amenity VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accommodation_amenities_accommodation
        FOREIGN KEY (accommodation_id)
        REFERENCES accommodations (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_accommodation_amenity
        UNIQUE (accommodation_id, amenity)
);

CREATE TABLE IF NOT EXISTS guests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guest_name VARCHAR(150) NOT NULL,
    cnic VARCHAR(30) NOT NULL,
    nationality VARCHAR(80) NOT NULL,
    guest_category_id BIGINT NOT NULL,
    address VARCHAR(255) NOT NULL,
    requested_by VARCHAR(120) NOT NULL,
    requested_department VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120) NOT NULL,
    accommodated_by VARCHAR(120) NOT NULL,
    arrival_at DATETIME NOT NULL,
    departure_at DATETIME NOT NULL,
    accommodation_id BIGINT,
    room_name VARCHAR(120) NOT NULL,
    remarks TEXT,
    review TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_guests_category
        FOREIGN KEY (guest_category_id)
        REFERENCES guest_categories (id),
    CONSTRAINT fk_guests_accommodation
        FOREIGN KEY (accommodation_id)
        REFERENCES accommodations (id),
    INDEX idx_guests_cnic (cnic),
    INDEX idx_guests_arrival (arrival_at),
    INDEX idx_guests_departure (departure_at)
);

INSERT IGNORE INTO users (username, role_name)
VALUES ('admin', 'admin');

INSERT IGNORE INTO guest_categories (name)
VALUES ('Family'), ('Non-Family');

INSERT IGNORE INTO accommodation_categories (name)
VALUES ('Rooms'), ('Suites'), ('Guest House');

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room I', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room II', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room III', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room IV', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room V', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room VI', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

INSERT INTO accommodations (category_id, name, capacity, status, assigned_staff)
SELECT id, 'Room VII', 2, 'Ready for Assignment', 'Admin Office'
FROM accommodation_categories
WHERE name = 'Rooms'
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);
