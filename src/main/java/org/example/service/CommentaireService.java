package org.example.service;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.example.dao.CommentaireDAO;
import org.example.model.Commentaire;

public class CommentaireService {
    private CommentaireDAO commentaireDAO = new CommentaireDAO();
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    private void validateCommentaire(Commentaire commentaire) throws IllegalArgumentException {
        // Validation authorName
        if (commentaire.getAuthorName() == null || commentaire.getAuthorName().trim().length() < 2 || 
            commentaire.getAuthorName().trim().length() > 100) {
            throw new IllegalArgumentException("Nom d'auteur invalide (2-100 caractères)");
        }
        
        // Validation email
        if (commentaire.getAuthorEmail() == null || !EMAIL_PATTERN.matcher(commentaire.getAuthorEmail()).matches()) {
            throw new IllegalArgumentException("Email invalide");
        }
        
        // Validation content
        if (commentaire.getContent() == null || commentaire.getContent().trim().length() < 5 || 
            commentaire.getContent().trim().length() > 2000) {
            throw new IllegalArgumentException("Commentaire invalide (5-2000 caractères)");
        }
        
        // Validation rating
        if (commentaire.getRating() < 1 || commentaire.getRating() > 5) {
            throw new IllegalArgumentException("Note invalide (1-5 étoiles)");
        }
    }
    
    public void createCommentaire(Commentaire commentaire) throws SQLException, IllegalArgumentException {
        validateCommentaire(commentaire);
        // Publication directe: pas de validation admin requise
        commentaire.setApproved(true);
        commentaireDAO.create(commentaire);
    }
    
    public List<Commentaire> getCommentairesByResource(int resourceId) throws SQLException {
        return commentaireDAO.findByResourceId(resourceId);
    }
    
    public List<Commentaire> getCommentairesByResourceAll(int resourceId) throws SQLException {
        return commentaireDAO.findByResourceIdAll(resourceId);
    }
    
    public Commentaire getCommentaireById(int id) throws SQLException {
        return commentaireDAO.findById(id);
    }
    
    public void updateCommentaire(Commentaire commentaire) throws SQLException, IllegalArgumentException {
        validateCommentaire(commentaire);
        commentaireDAO.update(commentaire);
    }
    
    public void deleteCommentaire(int id) throws SQLException {
        commentaireDAO.delete(id);
    }
    
    public void approveCommentaire(int id) throws SQLException {
        commentaireDAO.approve(id);
    }
    
    public List<Commentaire> getUnapprovedCommentaires() throws SQLException {
        return commentaireDAO.findUnapproved();
    }
    
    public void addLike(int commentId) throws SQLException {
        commentaireDAO.addLike(commentId);
    }
    
    public void removeLike(int commentId) throws SQLException {
        commentaireDAO.removeLike(commentId);
    }
    
    public void addDislike(int commentId) throws SQLException {
        commentaireDAO.addDislike(commentId);
    }
    
    public void removeDislike(int commentId) throws SQLException {
        commentaireDAO.removeDislike(commentId);
    }
}
