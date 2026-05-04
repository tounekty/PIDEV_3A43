-- ============================================
-- Création des tables pour Appointments et Patient Files
-- ============================================

-- Table appointment
CREATE TABLE IF NOT EXISTS appointment (
    id INT PRIMARY KEY AUTO_INCREMENT,
    date_time DATETIME NOT NULL,
    location VARCHAR(255),
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    student_id INT NOT NULL,
    psy_id INT NOT NULL,
    patient_file_id INT,
    report_name VARCHAR(255),
    report_updated_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (psy_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_file_id) REFERENCES patient_file(id) ON DELETE SET NULL,
    INDEX idx_student (student_id),
    INDEX idx_psy (psy_id),
    INDEX idx_status (status),
    INDEX idx_date_time (date_time),
    INDEX idx_patient_file (patient_file_id)
);

-- Table patient_file
CREATE TABLE IF NOT EXISTS patient_file (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL UNIQUE,
    traitements_en_cours TEXT,
    allergies TEXT,
    contact_urgence_nom VARCHAR(255),
    contact_urgence_tel VARCHAR(20),
    antecedents_personnels TEXT,
    antecedents_familiaux TEXT,
    motif_consultation TEXT,
    objectifs_therapeutiques TEXT,
    notes_generales TEXT,
    niveau_risque VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_niveau_risque (niveau_risque)
);

-- Table appointment_notification (for tracking sent notifications)
CREATE TABLE IF NOT EXISTS appointment_notification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    appointment_id INT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointment(id) ON DELETE CASCADE,
    INDEX idx_appointment (appointment_id),
    INDEX idx_sent_at (sent_at)
);

-- ============================================
-- Exemple de données (optionnel)
-- ============================================

-- Vous pouvez ajouter des données de test ici une fois que les tables users existent
-- INSERT INTO appointment (date_time, location, description, status, student_id, psy_id) VALUES
-- (DATE_ADD(NOW(), INTERVAL 7 DAY), 'Bureau 101', 'Consultation initiale', 'PENDING', 1, 2);

-- INSERT INTO patient_file (student_id, traitements_en_cours, allergies, niveau_risque) VALUES
-- (1, 'Aucun', 'Pénicilline', 'BAS');
