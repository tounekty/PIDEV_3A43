package org.example.service;

import org.example.model.ForumSubject;
import org.example.repository.ForumRepository;
import org.example.repository.impl.ForumRepositoryImpl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ForumService {
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.90;
    private static final double DESCRIPTION_SIMILARITY_THRESHOLD = 0.82;

    private final ForumRepository forumRepository;
    private final ToxicityDetectionService toxicityDetectionService;

    public ForumService() {
        this.forumRepository = new ForumRepositoryImpl();
        this.toxicityDetectionService = new ToxicityDetectionService();
    }
    public List<ForumSubject> getAllSubjects() throws SQLException {
        return forumRepository.findAll();
    }
    public void createTableIfNotExists() throws SQLException {
        forumRepository.createTableIfNotExists();
    }

    public void addSubject(ForumSubject subject) throws SQLException {
        if (subject == null || subject.getTitre() == null || subject.getTitre().isBlank()) {
            throw new SQLException("Subject title is required.");
        }
        checkToxicity(subject.getTitre(), subject.getDescription());
        ensureSubjectIsUnique(subject);
        forumRepository.save(subject);
    }

    public void updateSubject(ForumSubject subject) throws SQLException {
        if (subject == null || subject.getId() <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        checkToxicity(subject.getTitre(), subject.getDescription());
        ensureSubjectIsUnique(subject);
        forumRepository.update(subject);
    }

    public void deleteSubject(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        forumRepository.delete(id);
    }

    public List<ForumSubject> getSubjects(String query, String sortBy) throws SQLException {
        return forumRepository.findByQuery(query, sortBy);
    }

    public List<ForumSubject> getSubjects(String query, String sortBy, Integer userId) throws SQLException {
        return forumRepository.findByQuery(query, sortBy, userId);
    }

    public void reactToSubject(int subjectId, int userId, boolean like) throws SQLException {
        if (subjectId <= 0) {
            throw new SQLException("Valid subject ID is required.");
        }
        if (userId <= 0) {
            throw new SQLException("Valid user ID is required.");
        }
        forumRepository.reactToSubject(subjectId, userId, like);
    }

    private void ensureSubjectIsUnique(ForumSubject subject) throws SQLException {
        String title = normalizeForComparison(subject.getTitre());
        String description = normalizeForComparison(subject.getDescription());
        Set<String> titleTokens = tokens(title);
        Set<String> descriptionTokens = tokens(description);

        for (ForumSubject existing : forumRepository.findAll()) {
            if (existing == null || existing.getId() == subject.getId()) {
                continue;
            }

            String existingTitle = normalizeForComparison(existing.getTitre());
            String existingDescription = normalizeForComparison(existing.getDescription());

            if (!title.isBlank() && title.equals(existingTitle)) {
                throw new SQLException("Un sujet avec le meme titre existe deja: " + existing.getTitre());
            }

            if (!description.isBlank() && description.equals(existingDescription)) {
                throw new SQLException("Un sujet avec la meme description existe deja: " + existing.getTitre());
            }

            if (!titleTokens.isEmpty()
                    && jaccardSimilarity(titleTokens, tokens(existingTitle)) >= TITLE_SIMILARITY_THRESHOLD) {
                throw new SQLException("Un sujet tres similaire existe deja: #" + existing.getId() + " - " + existing.getTitre());
            }

            if (descriptionTokens.size() >= 6
                    && jaccardSimilarity(descriptionTokens, tokens(existingDescription)) >= DESCRIPTION_SIMILARITY_THRESHOLD) {
                throw new SQLException("Un sujet avec une description tres similaire existe deja: #" + existing.getId() + " - " + existing.getTitre());
            }
        }
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split("\\s+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private double jaccardSimilarity(Set<String> first, Set<String> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);

        Set<String> union = new HashSet<>(first);
        union.addAll(second);
        return (double) intersection.size() / union.size();
    }

    private void checkToxicity(String... texts) throws SQLException {
        try {
            Set<String> toxicWords = toxicityDetectionService.detectToxicWords(texts);
            if (!toxicWords.isEmpty()) {
                throw new SQLException("Votre contenu contient des termes non autorisés: " + toxicWords);
            }
        } catch (IOException e) {
            System.err.println("Warning: Toxicity detection failed: " + e.getMessage());
            // Do not block if detection service fails
        }
    }
}
