-- ============================================================================
-- MindCare Events Management System - Database Schema
-- ============================================================================
-- Created: April 27, 2026
-- Database: mindcare
-- Charset: utf8mb4
-- ============================================================================

-- Drop existing tables (if needed for clean reset)
-- DROP TABLE IF EXISTS waitlist;
-- DROP TABLE IF EXISTS reminders;
-- DROP TABLE IF EXISTS event_reviews;
-- DROP TABLE IF EXISTS event_engagement;
-- DROP TABLE IF EXISTS reservations;
-- DROP TABLE IF EXISTS events;
-- DROP TABLE IF EXISTS users;

-- ============================================================================
-- TABLE: users
-- Description: All application users (students and admins)
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    profile_image LONGBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: events
-- Description: All events created by admins
-- ============================================================================
CREATE TABLE IF NOT EXISTS events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    event_date DATETIME NOT NULL,
    duration_minutes INT DEFAULT 60,
    capacity INT NOT NULL,
    overbooking_percentage DECIMAL(5,2) DEFAULT 0,
    current_reservations INT DEFAULT 0,
    image LONGBLOB,
    image_path VARCHAR(255),
    status ENUM('ACTIVE', 'CANCELLED', 'COMPLETED', 'DRAFT') DEFAULT 'ACTIVE',
    created_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_event_date (event_date),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: reservations
-- Description: Student reservations for events (CONFIRMED, WAITLISTED, CANCELLED)
-- ============================================================================
CREATE TABLE IF NOT EXISTS reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    status ENUM('CONFIRMED', 'WAITLISTED', 'CANCELLED') DEFAULT 'CONFIRMED',
    waitlist_position INT,
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    qr_token VARCHAR(255),
    qr_token_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    notes VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE KEY unique_reservation (user_id, event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_status (status),
    INDEX idx_reserved_at (reserved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: event_engagement
-- Description: Track user engagement with events (likes, interest, scoring)
-- ============================================================================
CREATE TABLE IF NOT EXISTS event_engagement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    liked BOOLEAN DEFAULT FALSE,
    engagement_score DECIMAL(5,2) DEFAULT 0,
    clicked BOOLEAN DEFAULT FALSE,
    viewed BOOLEAN DEFAULT FALSE,
    shared BOOLEAN DEFAULT FALSE,
    engagement_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE KEY unique_engagement (user_id, event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_engagement_score (engagement_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: event_reviews
-- Description: Student reviews and ratings for attended events
-- ============================================================================
CREATE TABLE IF NOT EXISTS event_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    title VARCHAR(200),
    helpful_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE KEY unique_review (user_id, event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: waitlist
-- Description: Separate waitlist queue for events at capacity
-- ============================================================================
CREATE TABLE IF NOT EXISTS waitlist (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    position INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    promoted_at TIMESTAMP NULL,
    declined_at TIMESTAMP NULL,
    status ENUM('WAITING', 'PROMOTED', 'DECLINED', 'EXPIRED') DEFAULT 'WAITING',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE KEY unique_waitlist (user_id, event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_position (position),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: reminders
-- Description: Email reminders for upcoming events
-- ============================================================================
CREATE TABLE IF NOT EXISTS reminders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    reminder_type ENUM('BEFORE_24H', 'BEFORE_1H', 'CUSTOM') DEFAULT 'BEFORE_24H',
    scheduled_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    is_sent BOOLEAN DEFAULT FALSE,
    email_address VARCHAR(100),
    retry_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_is_sent (is_sent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: predictions
-- Description: AI predictions for event success and popularity
-- ============================================================================
CREATE TABLE IF NOT EXISTS predictions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL UNIQUE,
    predicted_popularity DECIMAL(5,2),
    predicted_attendance INT,
    predicted_occupancy_percent DECIMAL(5,2),
    success_score DECIMAL(5,2),
    confidence_level DECIMAL(5,2),
    prediction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: recommendations
-- Description: AI-generated personalized recommendations
-- ============================================================================
CREATE TABLE IF NOT EXISTS recommendations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    recommendation_score DECIMAL(5,2),
    reason VARCHAR(255),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    clicked BOOLEAN DEFAULT FALSE,
    clicked_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_recommendation_score (recommendation_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: smart_capacity
-- Description: Smart capacity management settings per event
-- ============================================================================
CREATE TABLE IF NOT EXISTS smart_capacity (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL UNIQUE,
    base_capacity INT NOT NULL,
    max_capacity_with_overbooking INT,
    overbooking_enabled BOOLEAN DEFAULT TRUE,
    overbooking_percent DECIMAL(5,2),
    current_confirmed INT DEFAULT 0,
    current_waitlisted INT DEFAULT 0,
    occupancy_percent DECIMAL(5,2),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: audit_log
-- Description: Track all important actions for security and debugging
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id INT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: api_keys
-- Description: API keys for external integrations
-- ============================================================================
CREATE TABLE IF NOT EXISTS api_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    api_key VARCHAR(255) UNIQUE NOT NULL,
    api_secret VARCHAR(255),
    service_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP NULL,
    INDEX idx_api_key (api_key),
    INDEX idx_service_name (service_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- SAMPLE DATA - Initial Users (Optional)
-- ============================================================================
-- Insert a default admin user (password: admin123 - should be hashed in production!)
INSERT IGNORE INTO users (username, email, password_hash, role, is_admin, full_name, created_at)
VALUES ('admin', 'admin@mindcare.local', 'admin123', 'ADMIN', TRUE, 'Administrator', CURRENT_TIMESTAMP);

-- Insert a sample student user
INSERT IGNORE INTO users (username, email, password_hash, role, is_admin, full_name, created_at)
VALUES ('student1', 'student1@mindcare.local', 'student123', 'STUDENT', FALSE, 'John Doe', CURRENT_TIMESTAMP);

-- ============================================================================
-- CREATE VIEWS for Common Queries
-- ============================================================================

-- View: Reservation Statistics per Event
CREATE OR REPLACE VIEW vw_event_statistics AS
SELECT 
    e.id,
    e.title,
    e.capacity,
    COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.id END) as confirmed_count,
    COUNT(DISTINCT CASE WHEN r.status = 'WAITLISTED' THEN r.id END) as waitlisted_count,
    COUNT(DISTINCT CASE WHEN r.status = 'CANCELLED' THEN r.id END) as cancelled_count,
    COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.id END) / e.capacity * 100 as occupancy_percent,
    AVG(er.rating) as avg_rating
FROM events e
LEFT JOIN reservations r ON e.id = r.event_id
LEFT JOIN event_reviews er ON e.id = er.event_id
GROUP BY e.id, e.title, e.capacity;

-- View: User Activity Summary
CREATE OR REPLACE VIEW vw_user_activity AS
SELECT 
    u.id,
    u.username,
    COUNT(DISTINCT r.id) as total_reservations,
    COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.id END) as confirmed_events,
    COUNT(DISTINCT er.id) as reviews_written,
    COUNT(DISTINCT ee.id) as events_engaged,
    SUM(ee.engagement_score) as total_engagement_score,
    u.last_login,
    u.created_at
FROM users u
LEFT JOIN reservations r ON u.id = r.user_id
LEFT JOIN event_reviews er ON u.id = er.user_id
LEFT JOIN event_engagement ee ON u.id = ee.user_id
GROUP BY u.id, u.username, u.last_login, u.created_at;

-- View: Upcoming Events
CREATE OR REPLACE VIEW vw_upcoming_events AS
SELECT 
    e.id,
    e.title,
    e.location,
    e.event_date,
    e.category,
    e.capacity,
    COUNT(DISTINCT CASE WHEN r.status = 'CONFIRMED' THEN r.id END) as confirmed_reservations,
    DATEDIFF(e.event_date, NOW()) as days_until_event
FROM events e
LEFT JOIN reservations r ON e.id = r.event_id
WHERE e.event_date > NOW() AND e.status = 'ACTIVE'
GROUP BY e.id
ORDER BY e.event_date ASC;

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
