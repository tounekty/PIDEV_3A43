-- Migration: un seul like/dislike par utilisateur et par commentaire

CREATE TABLE IF NOT EXISTS commentaire_vote (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_commentaire INT NOT NULL,
    id_user INT NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_commentaire_vote_user (id_commentaire, id_user),
    FOREIGN KEY (id_commentaire) REFERENCES commentaire(id) ON DELETE CASCADE,
    INDEX idx_commentaire_vote_commentaire (id_commentaire),
    INDEX idx_commentaire_vote_user (id_user)
);
