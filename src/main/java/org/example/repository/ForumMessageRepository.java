package org.example.repository;

import org.example.model.ForumMessage;

import java.sql.SQLException;
import java.util.List;

public interface ForumMessageRepository {
    void createTableIfNotExists() throws SQLException;
    
    void save(ForumMessage message) throws SQLException;
    
    void delete(int id) throws SQLException;
    
    void deleteBySubjectId(int subjectId) throws SQLException;
    
    List<ForumMessage> findBySubjectId(int subjectId) throws SQLException;
}
