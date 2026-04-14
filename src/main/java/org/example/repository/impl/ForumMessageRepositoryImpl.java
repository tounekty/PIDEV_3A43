package org.example.repository.impl;

import org.example.model.ForumMessage;
import org.example.repository.ForumMessageRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ForumMessageRepositoryImpl implements ForumMessageRepository {

    @Override
    public void createTableIfNotExists() throws SQLException {
        // TODO: Implement
    }

    @Override
    public void save(ForumMessage message) throws SQLException {
        // TODO: Implement
    }

    @Override
    public void delete(int id) throws SQLException {
        // TODO: Implement
    }

    @Override
    public void deleteBySubjectId(int subjectId) throws SQLException {
        // TODO: Implement
    }

    @Override
    public List<ForumMessage> findBySubjectId(int subjectId) throws SQLException {
        return new ArrayList<>();
    }
}
