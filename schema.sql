-- ============================================
-- Création des tables pour Ressources et Commentaires
-- ============================================

-- Table Resource
CREATE TABLE IF NOT EXISTS resource (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('article', 'video')),
    file_path VARCHAR(255),
    video_url VARCHAR(500),
    image_url VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_user INT NOT NULL,
    INDEX idx_user (id_user),
    INDEX idx_created (created_at)
);

-- Table Commentaire
CREATE TABLE IF NOT EXISTS commentaire (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_resource INT NOT NULL,
    id_user INT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    author_email VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_resource) REFERENCES resource(id) ON DELETE CASCADE,
    INDEX idx_resource (id_resource),
    INDEX idx_approved (approved),
    INDEX idx_created (created_at)
);

-- ============================================
-- Données d'exemple
-- ============================================

INSERT INTO resource (title, description, type, id_user) VALUES
('Introduction à JavaFX', 'Guide complet pour débuter avec JavaFX et créer des interfaces graphiques modernes en Java.', 'article', 1),
('Tutoriel MySQL', 'Apprenez à maîtriser les bases de données MySQL avec des exemples pratiques.', 'article', 1),
('Gestion CRUD en JavaFX', 'Tutoriel vidéo montrant comment implémenter un système CRUD complet avec JavaFX.', 'video', 1);

INSERT INTO commentaire (id_resource, id_user, author_name, author_email, content, rating, approved) VALUES
(1, 2, 'Jean Dupont', 'jean@example.com', 'Excellent tutoriel, très clair et bien structuré!', 5, TRUE),
(1, 3, 'Marie Martin', 'marie@example.com', 'Très utile pour débuter avec JavaFX', 4, TRUE),
(2, 2, 'Pierre Leonard', 'pierre@example.com', 'Bon contenu, quelques améliorations possibles', 3, TRUE);
