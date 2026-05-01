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
    private AiResourceGeneratorService aiResourceGeneratorService = new AiResourceGeneratorService();
    
    // Validation
    private void validateResource(Resource resource) throws IllegalArgumentException {
        if (resource.getTitle() == null || resource.getTitle().trim().length() < 3 || resource.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException("Titre invalide (3-255 caractères)");
        }
        if (resource.getDescription() == null || resource.getDescription().trim().length() < 10 || resource.getDescription().trim().length() > 10000) {
            throw new IllegalArgumentException("Description invalide (10-10000 caractères)");
        }
        if (!Resource.TYPE_ARTICLE.equals(resource.getType()) && 
            !Resource.TYPE_VIDEO.equals(resource.getType())) {
            throw new IllegalArgumentException("Type invalide (article ou video)");
        }
    }

    private void normalizeResource(Resource resource) {
        resource.setTitle(trimToEmpty(resource.getTitle()));
        resource.setDescription(trimToEmpty(resource.getDescription()));
        resource.setType(trimToEmpty(resource.getType()).toLowerCase());
        resource.setFilePath(blankToNull(resource.getFilePath()));
        resource.setVideoUrl(blankToNull(resource.getVideoUrl()));
        resource.setImageUrl(blankToNull(resource.getImageUrl()));
    }
    
    public void createResource(Resource resource) throws SQLException, IllegalArgumentException {
        normalizeResource(resource);
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
        normalizeResource(resource);
        validateResource(resource);
        resourceDAO.update(resource);
    }

    public Resource generateArticleDraft(String prompt, int userId) throws Exception {
        String cleanPrompt = trimToEmpty(prompt);
        if (cleanPrompt.length() < 10) {
            throw new IllegalArgumentException("Prompt invalide (minimum 10 caractères)");
        }
        if (cleanPrompt.length() > 2000) {
            throw new IllegalArgumentException("Prompt trop long (maximum 2000 caractères)");
        }

        Resource generated = aiResourceGeneratorService.generateArticleDraft(cleanPrompt, userId);
        normalizeResource(generated);
        generated.setType(Resource.TYPE_ARTICLE);
        validateResource(generated);
        return generated;
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

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
