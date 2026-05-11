package org.example.repository;

import org.example.model.ForumSubject;

import java.sql.SQLException;
import java.util.List;

public interface ForumRepository {
    void createTableIfNotExists() throws SQLException;
    
    void save(ForumSubject subject) throws SQLException;
    
    void update(ForumSubject subject) throws SQLException;
    
    void delete(int id) throws SQLException;
    
    List<ForumSubject> findAll() throws SQLException;

    List<ForumSubject> findAll(Integer userId) throws SQLException;
    
    List<ForumSubject> findByQuery(String query, String sortBy) throws SQLException;

    List<ForumSubject> findByQuery(String query, String sortBy, Integer userId) throws SQLException;
    
    ForumSubject findById(int id) throws SQLException;

    ForumSubject findById(int id, Integer userId) throws SQLException;

    void reactToSubject(int subjectId, int userId, boolean like) throws SQLException;
}
