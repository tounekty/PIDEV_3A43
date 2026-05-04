package com.mindcare.services;

import com.mindcare.db.DBConnection;
import com.mindcare.model.Appointment;
import com.mindcare.model.PatientFile;
import com.mindcare.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class AppointmentService {

    private static final int APPOINTMENT_DURATION_MINUTES = 60;
    private final AppointmentNotificationService notificationService = new AppointmentNotificationService(this);

    public List<Appointment> findAll() {
        refreshTimeBasedStatuses();
        String sql = "SELECT " +
            "a.id, a.date, a.location, a.description, a.status, a.idetudiant, a.idpsy, a.report_name, a.report_updated_at, " +
            "COALESCE(a.patient_file_id, pf.id) AS resolved_patient_file_id, " +
            "CONCAT_WS(' ', su.first_name, su.last_name) AS student_name, " +
            "CONCAT_WS(' ', pu.first_name, pu.last_name) AS psy_name " +
            "FROM appointment a " +
            "LEFT JOIN users su ON su.id = a.idetudiant " +
            "LEFT JOIN users pu ON pu.id = a.idpsy " +
            "LEFT JOIN patient_file pf ON pf.student_id = a.idetudiant " +
            "WHERE LOWER(COALESCE(a.status, '')) <> 'archived' " +
            "ORDER BY a.date DESC";

        List<Appointment> results = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load appointments from database", exception);
        }

        return results;
    }

    public Appointment findById(int appointmentId) {
        refreshTimeBasedStatuses();
        String sql = "SELECT " +
            "a.id, a.date, a.location, a.description, a.status, a.idetudiant, a.idpsy, a.report_name, a.report_updated_at, " +
            "COALESCE(a.patient_file_id, pf.id) AS resolved_patient_file_id, " +
            "CONCAT_WS(' ', su.first_name, su.last_name) AS student_name, " +
            "CONCAT_WS(' ', pu.first_name, pu.last_name) AS psy_name " +
            "FROM appointment a " +
            "LEFT JOIN users su ON su.id = a.idetudiant " +
            "LEFT JOIN users pu ON pu.id = a.idpsy " +
            "LEFT JOIN patient_file pf ON pf.student_id = a.idetudiant " +
            "WHERE a.id = ? LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load appointment", exception);
        }

        return null;
    }

    public Appointment create(Appointment appointment) {
        validateAppointmentDateTimeForScheduling(appointment);
        validateStudentWeeklyPsychologistLimit(appointment, null);

        int patientFileId = resolvePatientFileId(appointment.getPatientFileId(), appointment.getStudentId());

        String sql = "INSERT INTO appointment (date, location, description, status, idetudiant, idpsy, patient_file_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindCommon(statement, appointment, patientFileId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    appointment.setId(keys.getInt(1));
                }
            }
            appointment.setPatientFileId(patientFileId > 0 ? patientFileId : null);
            notificationService.notifyNewAppointment(appointment);
            return appointment;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create appointment", exception);
        }
    }

    public void update(Appointment appointment) {
        updateInternal(appointment, false);
    }

    public void updateByPsychologist(Appointment appointment) {
        updateInternal(appointment, true);
    }

    private void updateInternal(Appointment appointment, boolean editedByPsychologist) {
        validateAppointmentDateTimeForScheduling(appointment);
        Appointment previous = appointment == null ? null : findById(appointment.getId());

        int patientFileId = resolvePatientFileId(appointment.getPatientFileId(), appointment.getStudentId());

        String sql = "UPDATE appointment SET date = ?, location = ?, description = ?, status = ?, " +
            "idetudiant = ?, idpsy = ?, patient_file_id = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindCommon(statement, appointment, patientFileId);
            statement.setInt(8, appointment.getId());
            statement.executeUpdate();
            appointment.setPatientFileId(patientFileId > 0 ? patientFileId : null);
            notificationService.notifyEditedAppointment(previous, appointment);
            if (editedByPsychologist) {
                notificationService.notifyAppointmentTimeChangedByPsychologist(previous, appointment);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update appointment", exception);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM appointment WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete appointment", exception);
        }
    }

    public void updateStatus(int appointmentId, String status) {
        Appointment previous = findById(appointmentId);
        if (previous == null) {
            throw new IllegalStateException("Appointment not found");
        }
        String normalizedStatus = status == null ? "" : status.trim().toLowerCase();
        String sql = "UPDATE appointment SET status = ? WHERE id = ?";
        if ("accepted".equals(normalizedStatus)) {
            sql += " AND date >= ?";
        }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, appointmentId);
            if ("accepted".equals(normalizedStatus)) {
                statement.setObject(3, LocalDateTime.now());
            }
            int affected = statement.executeUpdate();
            if (affected == 0) {
                if ("accepted".equals(normalizedStatus)) {
                    throw new IllegalStateException("Impossible d'accepter un rendez-vous dont l'heure est deja passee.");
                }
                throw new IllegalStateException("Unable to update appointment status.");
            }
            Appointment current = findById(appointmentId);
            notificationService.notifyEditedAppointment(previous, current == null ? previous : current);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update appointment status", exception);
        }
    }

    public void deletePendingByStudent(int appointmentId, int studentId) {
        String sql = "DELETE FROM appointment WHERE id = ? AND idetudiant = ? AND LOWER(COALESCE(status, '')) = 'pending'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            statement.setInt(2, studentId);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Seuls les rendez-vous en attente peuvent etre supprimes.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete pending appointment", exception);
        }
    }

    public void cancelAcceptedByStudent(int appointmentId, int studentId) {
        Appointment previous = findById(appointmentId);
        String sql = "UPDATE appointment SET status = 'cancelled' " +
            "WHERE id = ? AND idetudiant = ? AND LOWER(COALESCE(status, '')) = 'accepted'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, appointmentId);
            statement.setInt(2, studentId);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Seuls les rendez-vous acceptes peuvent etre annules.");
            }
            if (previous != null) {
                notificationService.notifyAppointmentCancelledByStudent(previous);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to cancel accepted appointment", exception);
        }
    }

    public void updateByStudentResetPending(Appointment appointment, int studentId) {
        if (appointment == null || appointment.getId() <= 0) {
            throw new IllegalArgumentException("Appointment id is required");
        }

        validateAppointmentDateTimeForScheduling(appointment);
        validateStudentWeeklyPsychologistLimit(appointment, appointment.getId());
        int patientFileId = resolvePatientFileId(appointment.getPatientFileId(), studentId);

        String sql = "UPDATE appointment SET date = ?, location = ?, description = ?, status = 'pending', " +
            "idpsy = ?, patient_file_id = ? WHERE id = ? AND idetudiant = ? " +
            "AND LOWER(COALESCE(status, '')) IN ('pending', 'accepted')";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDateTime dateTime = appointment.getDateTime();
            if (dateTime == null) {
                throw new IllegalArgumentException("Appointment date is required");
            }

            statement.setObject(1, dateTime);
            statement.setString(2, safe(appointment.getLocation()));

            String description = appointment.getDescription();
            if (description != null) {
                description = description.trim();
                if (description.isEmpty()) {
                    description = null;
                } else if (description.length() < 6) {
                    throw new IllegalArgumentException("Description must be at least 6 characters when provided");
                }
            }
            statement.setString(3, description);

            if (appointment.getPsyId() != null) {
                statement.setInt(4, appointment.getPsyId());
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
            }

            if (patientFileId > 0) {
                statement.setInt(5, patientFileId);
            } else {
                statement.setNull(5, java.sql.Types.INTEGER);
            }

            statement.setInt(6, appointment.getId());
            statement.setInt(7, studentId);

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Seuls les rendez-vous en attente ou acceptes peuvent etre modifies.");
            }
            appointment.setStatus("pending");
            appointment.setPatientFileId(patientFileId > 0 ? patientFileId : null);
            notificationService.notifyAppointmentReturnedToPending(appointment);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update appointment for student", exception);
        }
    }

    public List<User> getStudents() {
        String sql = "SELECT id, first_name, last_name, email FROM users WHERE role = 'etudiant' ORDER BY first_name, last_name";
        List<User> students = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                students.add(user);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load students", exception);
        }
        return students;
    }

    public List<User> getPsychologists() {
        String sql = "SELECT id, first_name, last_name, email FROM users WHERE role = 'psychologue' ORDER BY first_name, last_name";
        List<User> psychologists = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                psychologists.add(user);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load psychologists", exception);
        }
        return psychologists;
    }

    public User findUserById(int userId) {
        String sql = "SELECT id, first_name, last_name, email FROM users WHERE id = ? LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setEmail(rs.getString("email"));
                    return user;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load user by id", exception);
        }
        return null;
    }

    public List<Appointment> findForPsychologist(int psyId) {
        refreshTimeBasedStatuses();
        String sql = "SELECT " +
            "a.id, a.date, a.location, a.description, a.status, a.idetudiant, a.idpsy, a.report_name, a.report_updated_at, " +
            "COALESCE(a.patient_file_id, pf.id) AS resolved_patient_file_id, " +
            "CONCAT_WS(' ', su.first_name, su.last_name) AS student_name, " +
            "CONCAT_WS(' ', pu.first_name, pu.last_name) AS psy_name " +
            "FROM appointment a " +
            "LEFT JOIN users su ON su.id = a.idetudiant " +
            "LEFT JOIN users pu ON pu.id = a.idpsy " +
            "LEFT JOIN patient_file pf ON pf.student_id = a.idetudiant " +
            "WHERE a.idpsy = ? AND LOWER(COALESCE(a.status, '')) <> 'archived' " +
            "ORDER BY a.date DESC";

        List<Appointment> results = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, psyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load psychologist appointments", exception);
        }

        return results;
    }

    public List<Appointment> findForStudent(int studentId) {
        refreshTimeBasedStatuses();
        String sql = "SELECT " +
            "a.id, a.date, a.location, a.description, a.status, a.idetudiant, a.idpsy, a.report_name, a.report_updated_at, " +
            "COALESCE(a.patient_file_id, pf.id) AS resolved_patient_file_id, " +
            "CONCAT_WS(' ', su.first_name, su.last_name) AS student_name, " +
            "CONCAT_WS(' ', pu.first_name, pu.last_name) AS psy_name " +
            "FROM appointment a " +
            "LEFT JOIN users su ON su.id = a.idetudiant " +
            "LEFT JOIN users pu ON pu.id = a.idpsy " +
            "LEFT JOIN patient_file pf ON pf.student_id = a.idetudiant " +
            "WHERE a.idetudiant = ? " +
            "ORDER BY a.date DESC";

        List<Appointment> results = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load student appointments", exception);
        }

        return results;
    }

    public void refreshTimeBasedStatuses() {
        String promoteToInProgressSql = "UPDATE appointment SET status = 'en cours' " +
            "WHERE LOWER(COALESCE(status, '')) = 'accepted' " +
            "AND date <= ? " +
            "AND DATE_ADD(date, INTERVAL ? MINUTE) > ?";

        String promoteToCompletedSql = "UPDATE appointment SET status = 'completed' " +
            "WHERE LOWER(COALESCE(status, '')) IN ('accepted', 'en cours') " +
            "AND DATE_ADD(date, INTERVAL ? MINUTE) <= ?";
        String promoteToArchivedSql = "UPDATE appointment SET status = 'archived' " +
            "WHERE LOWER(COALESCE(status, '')) = 'completed' " +
            "AND DATE_ADD(date, INTERVAL 1 MONTH) <= ?";

        LocalDateTime referenceNow = LocalDateTime.now();

        try (Connection connection = DBConnection.getConnection()) {
            try (PreparedStatement inProgress = connection.prepareStatement(promoteToInProgressSql)) {
                inProgress.setObject(1, referenceNow);
                inProgress.setInt(2, APPOINTMENT_DURATION_MINUTES);
                inProgress.setObject(3, referenceNow);
                inProgress.executeUpdate();
            }

            try (PreparedStatement completed = connection.prepareStatement(promoteToCompletedSql)) {
                completed.setInt(1, APPOINTMENT_DURATION_MINUTES);
                completed.setObject(2, referenceNow);
                completed.executeUpdate();
            }

            try (PreparedStatement archived = connection.prepareStatement(promoteToArchivedSql)) {
                archived.setObject(1, referenceNow);
                archived.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to refresh appointment statuses", exception);
        }
    }

    public List<String> getOccupiedSlotsForPsychologist(int psyId, LocalDate date) {
        return getOccupiedSlotsForPsychologist(psyId, date, null);
    }

    public List<String> getOccupiedSlotsForPsychologist(int psyId, LocalDate date, Integer excludedAppointmentId) {
        String sql = "SELECT date FROM appointment " +
            "WHERE idpsy = ? AND DATE(date) = ? " +
            "AND LOWER(COALESCE(status, '')) NOT IN ('rejected', 'cancelled')";

        if (excludedAppointmentId != null && excludedAppointmentId > 0) {
            sql += " AND id <> ?";
        }

        List<String> occupied = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, psyId);
            statement.setDate(2, java.sql.Date.valueOf(date));

            if (excludedAppointmentId != null && excludedAppointmentId > 0) {
                statement.setInt(3, excludedAppointmentId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime occupiedDateTime = rs.getObject("date", LocalDateTime.class);
                    if (occupiedDateTime != null) {
                        LocalTime time = occupiedDateTime.toLocalTime().withSecond(0).withNano(0);
                        occupied.add(time.format(formatter));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load occupied slots", exception);
        }

        return occupied;
    }

    public PatientFile getPatientFileById(int patientFileId) {
        String sql = "SELECT id, traitements_en_cours, allergies, contact_urgence_nom, contact_urgence_tel, " +
            "antecedents_personnels, antecedents_familiaux, motif_consultation, objectifs_therapeutiques, " +
            "notes_generales, niveau_risque, created_at, updated_at, student_id FROM patient_file WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientFileId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    PatientFile file = new PatientFile();
                    file.setId(rs.getInt("id"));
                    file.setTraitementsEnCours(rs.getString("traitements_en_cours"));
                    file.setAllergies(rs.getString("allergies"));
                    file.setContactUrgenceNom(rs.getString("contact_urgence_nom"));
                    file.setContactUrgenceTel(rs.getString("contact_urgence_tel"));
                    file.setAntecedentsPersonnels(rs.getString("antecedents_personnels"));
                    file.setAntecedentsFamiliaux(rs.getString("antecedents_familiaux"));
                    file.setMotifConsultation(rs.getString("motif_consultation"));
                    file.setObjectifsTherapeutiques(rs.getString("objectifs_therapeutiques"));
                    file.setNotesGenerales(rs.getString("notes_generales"));
                    file.setNiveauRisque(rs.getString("niveau_risque"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        file.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        file.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    file.setStudentId(rs.getInt("student_id"));
                    return file;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load patient file", exception);
        }
        return null;
    }

    public PatientFile getPatientFileByStudentId(int studentId) {
        String sql = "SELECT id FROM patient_file WHERE student_id = ? LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return getPatientFileById(rs.getInt("id"));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load patient file by student", exception);
        }
        return null;
    }

    public PatientFile savePatientFile(PatientFile file) {
        if (file == null || file.getStudentId() == null || file.getStudentId() <= 0) {
            throw new IllegalArgumentException("student_id is required to save patient file");
        }

        PatientFile existing = getPatientFileByStudentId(file.getStudentId());
        if (existing == null) {
            return insertPatientFile(file);
        }

        file.setId(existing.getId());
        updatePatientFile(file);
        return getPatientFileById(existing.getId());
    }

    public PatientFile savePatientFileForStudent(PatientFile file) {
        if (file == null || file.getStudentId() == null || file.getStudentId() <= 0) {
            throw new IllegalArgumentException("student_id is required to save patient file");
        }

        PatientFile existing = getPatientFileByStudentId(file.getStudentId());
        if (existing == null) {
            // Create new with only contact d'urgence fields
            return savePatientFileOnlyContactInfo(file);
        }

        // Update only contact d'urgence fields, preserve clinical data
        updatePatientFileOnlyContactInfo(file, existing);
        return getPatientFileById(existing.getId());
    }

    private PatientFile savePatientFileOnlyContactInfo(PatientFile file) {
        String sql = "INSERT INTO patient_file (contact_urgence_nom, contact_urgence_tel, created_at, updated_at, student_id) " +
            "VALUES (?, ?, NOW(), NOW(), ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, validateContactName(file.getContactUrgenceNom()));
            statement.setString(2, validateContactPhone(file.getContactUrgenceTel()));
            statement.setInt(3, file.getStudentId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    file.setId(keys.getInt(1));
                }
            }
            return getPatientFileById(file.getId());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create patient file", exception);
        }
    }

    private void updatePatientFileOnlyContactInfo(PatientFile file, PatientFile existing) {
        String sql = "UPDATE patient_file SET contact_urgence_nom = ?, contact_urgence_tel = ?, updated_at = NOW() WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, validateContactName(file.getContactUrgenceNom()));
            statement.setString(2, validateContactPhone(file.getContactUrgenceTel()));
            statement.setInt(3, file.getStudentId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update patient file contact info", exception);
        }
    }

    private PatientFile insertPatientFile(PatientFile file) {
        String sql = "INSERT INTO patient_file (traitements_en_cours, allergies, contact_urgence_nom, contact_urgence_tel, " +
            "antecedents_personnels, antecedents_familiaux, motif_consultation, objectifs_therapeutiques, notes_generales, " +
            "niveau_risque, created_at, updated_at, student_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindPatientFile(statement, file);
            statement.setInt(11, file.getStudentId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    file.setId(keys.getInt(1));
                }
            }
            return getPatientFileById(file.getId());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create patient file", exception);
        }
    }

    private void updatePatientFile(PatientFile file) {
        String sql = "UPDATE patient_file SET traitements_en_cours = ?, allergies = ?, contact_urgence_nom = ?, " +
            "contact_urgence_tel = ?, antecedents_personnels = ?, antecedents_familiaux = ?, motif_consultation = ?, " +
            "objectifs_therapeutiques = ?, notes_generales = ?, niveau_risque = ?, updated_at = NOW() WHERE student_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPatientFile(statement, file);
            statement.setInt(11, file.getStudentId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update patient file", exception);
        }
    }

    private void bindPatientFile(PreparedStatement statement, PatientFile file) throws SQLException {
        statement.setString(1, nullableMin6(file.getTraitementsEnCours(), "traitements_en_cours"));
        statement.setString(2, nullableMin6(file.getAllergies(), "allergies"));
        statement.setString(3, nullableMin6(file.getContactUrgenceNom(), "contact_urgence_nom"));
        statement.setString(4, validateContactPhone(file.getContactUrgenceTel()));
        statement.setString(5, nullableMin6(file.getAntecedentsPersonnels(), "antecedents_personnels"));
        statement.setString(6, nullableMin6(file.getAntecedentsFamiliaux(), "antecedents_familiaux"));
        statement.setString(7, nullableMin6(file.getMotifConsultation(), "motif_consultation"));
        statement.setString(8, nullableMin6(file.getObjectifsTherapeutiques(), "objectifs_therapeutiques"));
        statement.setString(9, nullableMin6(file.getNotesGenerales(), "notes_generales"));
        statement.setString(10, nullableMin6(file.getNiveauRisque(), "niveau_risque"));
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getInt("id"));

        LocalDateTime appointmentDateTime = rs.getObject("date", LocalDateTime.class);
        if (appointmentDateTime != null) {
            appointment.setDateTime(appointmentDateTime);
        }

        appointment.setLocation(rs.getString("location"));
        appointment.setDescription(rs.getString("description"));
        appointment.setStatus(rs.getString("status"));
        appointment.setReportName(rs.getString("report_name"));
        appointment.setReportUpdatedAt(rs.getObject("report_updated_at", LocalDateTime.class));

        int studentId = rs.getInt("idetudiant");
        appointment.setStudentId(rs.wasNull() ? null : studentId);

        int psyId = rs.getInt("idpsy");
        appointment.setPsyId(rs.wasNull() ? null : psyId);

        int patientFileId = rs.getInt("resolved_patient_file_id");
        appointment.setPatientFileId(rs.wasNull() ? null : patientFileId);

        appointment.setStudentName(rs.getString("student_name"));
        appointment.setPsyName(rs.getString("psy_name"));
        
        return appointment;
    }

    public void uploadPsychologistReport(int appointmentId, int psychologistId, Path sourceFile) {
        if (sourceFile == null) {
            throw new IllegalArgumentException("Report file is required");
        }
        if (!Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("Selected report file does not exist");
        }

        Appointment appointment = findById(appointmentId);
        if (appointment == null) {
            throw new IllegalStateException("Appointment not found");
        }
        if (appointment.getPsyId() == null || appointment.getPsyId() != psychologistId) {
            throw new IllegalStateException("Only the assigned psychologist can upload the report");
        }
        String normalizedStatus = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toLowerCase();
        if (!"completed".equals(normalizedStatus)) {
            throw new IllegalStateException("Report can only be uploaded for completed appointments");
        }

        try {
            Path reportsDir = Path.of("uploads", "appointment-reports");
            Files.createDirectories(reportsDir);

            String originalName = sourceFile.getFileName().toString();
            int dotIndex = originalName.lastIndexOf('.');
            String extension = dotIndex >= 0 ? originalName.substring(dotIndex) : "";
            String storedFileName = "appointment-" + appointmentId + "-" + System.currentTimeMillis() + extension;
            Path destination = reportsDir.resolve(storedFileName);
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);

            String sql = "UPDATE appointment SET report_name = ?, report_updated_at = ? WHERE id = ?";
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, storedFileName);
                statement.setObject(2, LocalDateTime.now());
                statement.setInt(3, appointmentId);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save report metadata", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to upload report file", exception);
        }
    }

    public Path resolveAppointmentReportPath(String reportName) {
        if (reportName == null || reportName.trim().isEmpty()) {
            return null;
        }
        return Path.of("uploads", "appointment-reports", reportName);
    }

    public String buildPsychologistAcceptanceStatsSummary(int psyId) {
        String sql = "SELECT TIME_FORMAT(date, '%H:%i') AS slot, " +
            "SUM(CASE WHEN LOWER(COALESCE(status, '')) IN ('accepted', 'en cours', 'completed') THEN 1 ELSE 0 END) AS accepted_count, " +
            "COUNT(*) AS total_count, " +
            "SUM(CASE WHEN LOWER(COALESCE(status, '')) IN ('accepted', 'en cours', 'completed') THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS acceptance_rate " +
            "FROM appointment " +
            "WHERE idpsy = ? " +
            "AND LOWER(COALESCE(status, '')) IN ('accepted', 'en cours', 'completed', 'rejected', 'cancelled') " +
            "GROUP BY TIME_FORMAT(date, '%H:%i') " +
            "HAVING COUNT(*) >= 1 " +
            "ORDER BY acceptance_rate DESC, total_count DESC " +
            "LIMIT 10";

        StringBuilder builder = new StringBuilder();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, psyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String slot = rs.getString("slot");
                    int acceptedCount = rs.getInt("accepted_count");
                    int totalCount = rs.getInt("total_count");
                    double rate = rs.getDouble("acceptance_rate");
                    builder.append("- ").append(slot)
                        .append(" -> ").append(acceptedCount).append("/").append(totalCount)
                        .append(" (").append(String.format(Locale.ROOT, "%.1f", rate)).append("%)")
                        .append("\n");
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load acceptance stats for psychologist", exception);
        }

        if (builder.length() == 0) {
            return "Aucune donnée historique disponible pour ce psychologue.";
        }
        return builder.toString().trim();
    }

    private void bindCommon(PreparedStatement statement, Appointment appointment, int patientFileId) throws SQLException {
        LocalDateTime dateTime = appointment.getDateTime();
        if (dateTime == null) {
            throw new IllegalArgumentException("Appointment date is required");
        }

        statement.setObject(1, dateTime);
        statement.setString(2, safe(appointment.getLocation()));
        String description = appointment.getDescription();
        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            } else if (description.length() < 6) {
                throw new IllegalArgumentException("Description must be at least 6 characters when provided");
            }
        }
        statement.setString(3, description);
        statement.setString(4, safe(appointment.getStatus()));

        if (appointment.getStudentId() != null) {
            statement.setInt(5, appointment.getStudentId());
        } else {
            statement.setNull(5, java.sql.Types.INTEGER);
        }

        if (appointment.getPsyId() != null) {
            statement.setInt(6, appointment.getPsyId());
        } else {
            statement.setNull(6, java.sql.Types.INTEGER);
        }

        if (patientFileId > 0) {
            statement.setInt(7, patientFileId);
        } else {
            statement.setNull(7, java.sql.Types.INTEGER);
        }
    }

    private int resolvePatientFileId(Integer explicitPatientFileId, Integer studentId) {
        if (explicitPatientFileId != null && explicitPatientFileId > 0) {
            return explicitPatientFileId;
        }
        if (studentId == null || studentId <= 0) {
            return 0;
        }

        String sql = "SELECT id FROM patient_file WHERE student_id = ? LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to resolve patient_file for student", exception);
        }
        return 0;
    }

    public boolean hasStudentAppointmentWithPsychologistInSameWeek(int studentId, int psyId, LocalDateTime targetDateTime, Integer excludedAppointmentId) {
        if (studentId <= 0 || psyId <= 0 || targetDateTime == null) {
            return false;
        }

        // Query: count active (non-cancelled/non-rejected) appointments for the same student, psychologist, and week
        // This explicitly checks for appointments with status 'pending' or 'accepted' only
        String sql = "SELECT COUNT(*) FROM appointment " +
            "WHERE idetudiant = ? AND idpsy = ? " +
            "AND YEARWEEK(date, 1) = YEARWEEK(?, 1) " +
            "AND YEAR(date) = YEAR(?) " +
            "AND LOWER(COALESCE(status, 'pending')) IN ('pending', 'accepted')";

        if (excludedAppointmentId != null && excludedAppointmentId > 0) {
            sql += " AND id <> ?";
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, psyId);
            statement.setObject(3, targetDateTime);
            statement.setObject(4, targetDateTime);
            if (excludedAppointmentId != null && excludedAppointmentId > 0) {
                statement.setInt(5, excludedAppointmentId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                boolean hasAppointment = rs.next() && rs.getInt(1) > 0;
                if (hasAppointment) {
                    System.out.println("[AppointmentService] Student " + studentId + " already has active appointment with psychologist " + psyId + " this week");
                }
                return hasAppointment;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to validate weekly appointment limit", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "[Vide]".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private void validateAppointmentDateTimeForScheduling(Appointment appointment) {
        if (appointment == null || appointment.getDateTime() == null) {
            throw new IllegalArgumentException("Appointment date is required");
        }
        if (appointment.getDateTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Appointment must be at least 1 hour from now");
        }
    }

    private void validateStudentWeeklyPsychologistLimit(Appointment appointment, Integer excludedAppointmentId) {
        if (appointment == null
            || appointment.getStudentId() == null
            || appointment.getPsyId() == null
            || appointment.getDateTime() == null) {
            return;
        }
        boolean alreadyBooked = hasStudentAppointmentWithPsychologistInSameWeek(
            appointment.getStudentId(),
            appointment.getPsyId(),
            appointment.getDateTime(),
            excludedAppointmentId
        );
        if (alreadyBooked) {
            throw new IllegalArgumentException(
                "Un seul rendez-vous par semaine par psychologue et par étudiant."
            );
        }
    }

    private String nullableMin6(String value, String fieldName) {
        String normalized = nullable(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() < 6) {
            throw new IllegalArgumentException(fieldName + " must be at least 6 characters when provided");
        }
        return normalized;
    }

    private String validateContactName(String value) {
        String normalized = nullable(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() < 3) {
            throw new IllegalArgumentException("contact_urgence_nom must be at least 3 characters when provided");
        }
        if (normalized.length() > 20) {
            throw new IllegalArgumentException("contact_urgence_nom cannot exceed 20 characters");
        }
        return normalized;
    }

    private String validateContactPhone(String value) {
        String normalized = nullable(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("contact_urgence_tel must be exactly 8 digits");
        }
        return normalized;
    }
}
