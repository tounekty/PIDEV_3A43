-- Migration: Ajouter les colonnes like_count et dislike_count à la table commentaire
-- Exécutez ce script si vous avez des commentaires existants

ALTER TABLE commentaire ADD COLUMN like_count INT DEFAULT 0;
ALTER TABLE commentaire ADD COLUMN dislike_count INT DEFAULT 0;

-- Vérification
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'commentaire' AND TABLE_SCHEMA = DATABASE()
ORDER BY ORDINAL_POSITION;
