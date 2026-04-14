package org.example.service;

import org.example.dao.ResourceDAO;
import org.example.dao.CommentaireDAO;
import org.example.model.Resource;
import org.example.model.Commentaire;

import java.sql.SQLException;
import java.util.List;

public class ResourceService {
    private ResourceDAO resourceDAO = new ResourceDAO();
    private CommentaireDAO commentaireDAO = new CommentaireDAO();
    
    // Validation
    private void validateResource(Resource resource) throws IllegalArgumentException {
        if (resource.getTitle() == null || resource.getTitle().trim().length() < 3 || resource.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException("Titre invalide (3-255 caractères)");
        }
        if (resource.getDescription() == null || resource.getDescription().trim().length() < 10 || resource.getDescription().trim().length() > 5000) {
            throw new IllegalArgumentException("Description invalide (10-5000 caractères)");
        }
        if (!resource.getType().equals(Resource.TYPE_ARTICLE) && 
            !resource.getType().equals(Resource.TYPE_VIDEO)) {
            throw new IllegalArgumentException("Type invalide (article ou video)");
        }
    }
    
    public void createResource(Resource resource) throws SQLException, IllegalArgumentException {
        validateResource(resource);
        resourceDAO.create(resource);
    }
    
    public List<Resource> getAllResources() throws SQLException {
        return resourceDAO.findAll();
    }
    
    public Resource getResourceById(int id) throws SQLException {
        return resourceDAO.findById(id);
    }
    
    public void updateResource(Resource resource) throws SQLException, IllegalArgumentException {
        validateResource(resource);
        resourceDAO.update(resource);
    }
    
    public void deleteResource(int id) throws SQLException {
        // Supprime les commentaires associés
        List<Commentaire> comments = commentaireDAO.findByResourceIdAll(id);
        for (Commentaire c : comments) {
            commentaireDAO.delete(c.getId());
        }
        resourceDAO.delete(id);
    }
    
    public List<Resource> searchResources(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllResources();
        }
        return resourceDAO.search(query.trim());
    }
}
