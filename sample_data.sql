-- ============================================================================
-- MindCare Events - Sample Data & Testing Queries
-- ============================================================================
-- Use this file to test the database and populate it with sample data
-- ============================================================================

USE mindcare;

-- ============================================================================
-- 1. SAMPLE USERS
-- ============================================================================

-- Insert additional student users
INSERT INTO users (username, email, password_hash, role, is_admin, full_name, phone, created_at)
VALUES 
('alice', 'alice@student.local', '$2a$12$...hash...', 'STUDENT', FALSE, 'Alice Johnson', '555-0101', NOW()),
('bob', 'bob@student.local', '$2a$12$...hash...', 'STUDENT', FALSE, 'Bob Smith', '555-0102', NOW()),
('charlie', 'charlie@student.local', '$2a$12$...hash...', 'STUDENT', FALSE, 'Charlie Brown', '555-0103', NOW()),
('diana', 'diana@student.local', '$2a$12$...hash...', 'STUDENT', FALSE, 'Diana Prince', '555-0104', NOW()),
('eve', 'eve@student.local', '$2a$12$...hash...', 'STUDENT', FALSE, 'Eve Wilson', '555-0105', NOW());

-- ============================================================================
-- 2. SAMPLE EVENTS (Created by admin, user_id=1)
-- ============================================================================

INSERT INTO events (title, description, location, category, event_date, duration_minutes, 
                   capacity, overbooking_percentage, image_path, status, created_by, created_at)
VALUES 
(
    'Advanced Java Programming Workshop',
    'Learn advanced Java concepts including concurrency, streams, and functional programming.',
    'Room 101 - Building A',
    'Workshop',
    '2026-05-15 10:00:00',
    180,
    50,
    10,
    '/uploads/java-workshop.png',
    'ACTIVE',
    1,
    NOW()
),
(
    'Web Development with Spring Boot',
    'Master Spring Boot and build scalable web applications from scratch.',
    'Room 205 - Building B',
    'Workshop',
    '2026-05-20 14:00:00',
    120,
    40,
    15,
    '/uploads/spring-boot.png',
    'ACTIVE',
    1,
    NOW()
),
(
    'Database Design & Optimization',
    'Deep dive into SQL optimization, indexing strategies, and database architecture.',
    'Lab 301 - Building C',
    'Workshop',
    '2026-05-25 09:00:00',
    150,
    30,
    0,
    '/uploads/database.png',
    'ACTIVE',
    1,
    NOW()
),
(
    'Networking Event - Tech Careers',
    'Meet industry professionals and explore career opportunities in technology.',
    'Main Hall - Student Center',
    'Networking',
    '2026-06-01 18:00:00',
    120,
    200,
    20,
    '/uploads/networking.png',
    'ACTIVE',
    1,
    NOW()
),
(
    'UI/UX Design Masterclass',
    'Learn modern design principles and create beautiful user interfaces.',
    'Design Lab - Building D',
    'Workshop',
    '2026-06-05 15:00:00',
    90,
    25,
    10,
    '/uploads/ui-ux.png',
    'ACTIVE',
    1,
    NOW()
);

-- ============================================================================
-- 3. SMART CAPACITY MANAGEMENT
-- ============================================================================

INSERT INTO smart_capacity (event_id, base_capacity, max_capacity_with_overbooking, 
                           overbooking_enabled, overbooking_percent, current_confirmed, 
                           current_waitlisted, occupancy_percent)
VALUES 
(1, 50, 55, TRUE, 10, 0, 0, 0),
(2, 40, 46, TRUE, 15, 0, 0, 0),
(3, 30, 30, FALSE, 0, 0, 0, 0),
(4, 200, 240, TRUE, 20, 0, 0, 0),
(5, 25, 27, TRUE, 10, 0, 0, 0);

-- ============================================================================
-- 4. RESERVATIONS (Students reserve events)
-- ============================================================================

-- Alice reserves Java Workshop (CONFIRMED - spots available)
INSERT INTO reservations (user_id, event_id, status, reserved_at, qr_token)
VALUES (2, 1, 'CONFIRMED', NOW(), 'token_alice_java_1');

-- Bob reserves Java Workshop (CONFIRMED)
INSERT INTO reservations (user_id, event_id, status, reserved_at, qr_token)
VALUES (3, 1, 'CONFIRMED', NOW(), 'token_bob_java_1');

-- Charlie reserves Java Workshop (CONFIRMED)
INSERT INTO reservations (user_id, event_id, status, reserved_at, qr_token)
VALUES (4, 1, 'CONFIRMED', NOW(), 'token_charlie_java_1');

-- Diana reserves Spring Boot Workshop (CONFIRMED)
INSERT INTO reservations (user_id, event_id, status, reserved_at, qr_token)
VALUES (5, 2, 'CONFIRMED', NOW(), 'token_diana_spring_2');

-- Eve reserves Spring Boot Workshop (CONFIRMED)
INSERT INTO reservations (user_id, event_id, status, reserved_at, qr_token)
VALUES (6, 2, 'CONFIRMED', NOW(), 'token_eve_spring_2');

-- Alice reserves Networking Event (CONFIRMED)
INSERT INTO reservations (user_id, event_id, status, reserved_at, qr_token)
VALUES (2, 4, 'CONFIRMED', NOW(), 'token_alice_network_4');

-- Update events to reflect current reservations
UPDATE events SET current_reservations = 3 WHERE id = 1;
UPDATE events SET current_reservations = 2 WHERE id = 2;
UPDATE events SET current_reservations = 1 WHERE id = 4;

UPDATE smart_capacity SET current_confirmed = 3 WHERE event_id = 1;
UPDATE smart_capacity SET occupancy_percent = (3.0 / 50) * 100 WHERE event_id = 1;

UPDATE smart_capacity SET current_confirmed = 2 WHERE event_id = 2;
UPDATE smart_capacity SET occupancy_percent = (2.0 / 40) * 100 WHERE event_id = 2;

-- ============================================================================
-- 5. EVENT ENGAGEMENT (Tracking user interest)
-- ============================================================================

-- Alice engages with multiple events
INSERT INTO event_engagement (user_id, event_id, liked, engagement_score, viewed, clicked)
VALUES 
(2, 1, TRUE, 0.9, TRUE, TRUE),   -- Liked Java workshop
(2, 2, TRUE, 0.8, TRUE, TRUE),   -- Liked Spring Boot
(2, 5, FALSE, 0.4, TRUE, FALSE); -- Viewed UI/UX

-- Bob's engagement
INSERT INTO event_engagement (user_id, event_id, liked, engagement_score, viewed, clicked)
VALUES 
(3, 1, TRUE, 0.95, TRUE, TRUE),   -- Loved Java workshop
(3, 3, FALSE, 0.3, TRUE, FALSE);  -- Just viewed Database

-- Charlie's engagement
INSERT INTO event_engagement (user_id, event_id, liked, engagement_score, viewed, clicked)
VALUES 
(4, 1, TRUE, 0.85, TRUE, TRUE),
(4, 4, TRUE, 0.7, TRUE, TRUE);

-- Diana & Eve engagement
INSERT INTO event_engagement (user_id, event_id, liked, engagement_score, viewed, clicked)
VALUES 
(5, 2, TRUE, 0.9, TRUE, TRUE),
(6, 2, TRUE, 0.85, TRUE, TRUE);

-- ============================================================================
-- 6. EVENT REVIEWS & RATINGS
-- ============================================================================

-- Alice reviews Java Workshop (attended, gave 5 stars)
INSERT INTO event_reviews (user_id, event_id, rating, title, comment, created_at)
VALUES 
(2, 1, 5, 'Excellent Workshop!', 'Very informative and well-organized. The instructor was knowledgeable and approachable.', NOW()),
(3, 1, 4, 'Great Content', 'Good content but could use more hands-on exercises.', NOW()),
(4, 1, 5, 'Outstanding!', 'One of the best workshops I attended. Highly recommended!', NOW());

-- Reviews for Networking Event
INSERT INTO event_reviews (user_id, event_id, rating, title, comment, created_at)
VALUES 
(2, 4, 4, 'Good Networking Opportunity', 'Met some interesting people. Would like more structured time slots.', NOW());

-- ============================================================================
-- 7. EMAIL REMINDERS (Scheduled for each reservation)
-- ============================================================================

-- 24-hour reminder for Java Workshop (May 14, 10:00 AM)
INSERT INTO reminders (user_id, event_id, reminder_type, scheduled_at, email_address, is_sent)
VALUES 
(2, 1, 'BEFORE_24H', DATE_ADD('2026-05-15 10:00:00', INTERVAL -24 HOUR), 'alice@student.local', FALSE),
(3, 1, 'BEFORE_24H', DATE_ADD('2026-05-15 10:00:00', INTERVAL -24 HOUR), 'bob@student.local', FALSE),
(4, 1, 'BEFORE_24H', DATE_ADD('2026-05-15 10:00:00', INTERVAL -24 HOUR), 'charlie@student.local', FALSE);

-- 1-hour reminder for Spring Boot Workshop
INSERT INTO reminders (user_id, event_id, reminder_type, scheduled_at, email_address, is_sent)
VALUES 
(5, 2, 'BEFORE_1H', DATE_ADD('2026-05-20 14:00:00', INTERVAL -1 HOUR), 'diana@student.local', FALSE),
(6, 2, 'BEFORE_1H', DATE_ADD('2026-05-20 14:00:00', INTERVAL -1 HOUR), 'eve@student.local', FALSE);

-- ============================================================================
-- 8. AI PREDICTIONS (Event success predictions)
-- ============================================================================

INSERT INTO predictions (event_id, predicted_popularity, predicted_attendance, 
                        predicted_occupancy_percent, success_score, confidence_level)
VALUES 
(1, 9.2, 48, 96, 0.95, 0.92),  -- High success prediction for Java Workshop
(2, 8.8, 36, 90, 0.92, 0.88),  -- Spring Boot will be successful
(3, 7.5, 22, 73, 0.78, 0.75),  -- Database workshop moderate success
(4, 8.9, 180, 90, 0.93, 0.90), -- Networking event high success
(5, 7.2, 18, 72, 0.75, 0.70);  -- UI/UX moderate-low success

-- ============================================================================
-- 9. AI RECOMMENDATIONS (Personalized for each student)
-- ============================================================================

-- Recommendations for Alice (high engagement)
INSERT INTO recommendations (user_id, event_id, recommendation_score, reason, generated_at)
VALUES 
(2, 3, 0.88, 'You enjoyed advanced Java workshop. Database optimization is related.', NOW()),
(2, 5, 0.72, 'Design complements development skills.', NOW());

-- Recommendations for Bob
INSERT INTO recommendations (user_id, event_id, recommendation_score, reason, generated_at)
VALUES 
(3, 2, 0.85, 'Based on your interest in Java, Spring Boot is highly recommended.', NOW()),
(3, 4, 0.65, 'Meet companies hiring senior developers.', NOW());

-- Recommendations for Charlie
INSERT INTO recommendations (user_id, event_id, recommendation_score, reason, generated_at)
VALUES 
(4, 2, 0.82, 'Continuing your Java learning path.', NOW()),
(4, 3, 0.78, 'Database skills boost career prospects.', NOW());

-- ============================================================================
-- 10. AUDIT LOG (Track all actions)
-- ============================================================================

INSERT INTO audit_log (user_id, action, entity_type, entity_id, new_value, timestamp)
VALUES 
(1, 'CREATE_EVENT', 'EVENT', 1, 'Advanced Java Programming Workshop', NOW()),
(1, 'CREATE_EVENT', 'EVENT', 2, 'Web Development with Spring Boot', NOW()),
(2, 'CREATE_RESERVATION', 'RESERVATION', 1, 'CONFIRMED', NOW()),
(2, 'LIKE_EVENT', 'EVENT_ENGAGEMENT', 1, 'LIKED', NOW()),
(3, 'CREATE_RESERVATION', 'RESERVATION', 2, 'CONFIRMED', NOW());

-- ============================================================================
-- USEFUL QUERIES FOR TESTING
-- ============================================================================

-- View all events with occupancy
SELECT * FROM vw_event_statistics;

-- View user activity
SELECT * FROM vw_user_activity;

-- View upcoming events
SELECT * FROM vw_upcoming_events;

-- Find reservations by user
SELECT r.id, e.title, r.status, r.reserved_at 
FROM reservations r
JOIN events e ON r.event_id = e.id
WHERE r.user_id = 2
ORDER BY r.reserved_at DESC;

-- Check engagement scores
SELECT u.username, e.title, ee.engagement_score, ee.liked, ee.viewed, ee.clicked
FROM event_engagement ee
JOIN users u ON ee.user_id = u.id
JOIN events e ON ee.event_id = e.id
ORDER BY ee.engagement_score DESC;

-- Check recommendations
SELECT u.username, e.title, r.recommendation_score, r.reason
FROM recommendations r
JOIN users u ON r.user_id = u.id
JOIN events e ON r.event_id = e.id
ORDER BY r.recommendation_score DESC;

-- Event reviews summary
SELECT e.title, AVG(er.rating) as avg_rating, COUNT(er.id) as review_count
FROM events e
LEFT JOIN event_reviews er ON e.id = er.event_id
GROUP BY e.id, e.title;

-- Pending reminders
SELECT u.username, e.title, r.reminder_type, r.scheduled_at
FROM reminders r
JOIN users u ON r.user_id = u.id
JOIN events e ON r.event_id = e.id
WHERE r.is_sent = FALSE
ORDER BY r.scheduled_at ASC;

-- Database statistics
SELECT TABLE_NAME, TABLE_ROWS, ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'mindcare'
ORDER BY TABLE_ROWS DESC;

-- ============================================================================
-- END OF SAMPLE DATA
-- ============================================================================
