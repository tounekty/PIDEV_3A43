package com.mindcare.legacy.psychologue;

import com.mindcare.components.BadgeLabel;
import com.mindcare.model.Appointment;
import com.mindcare.model.PatientFile;
import com.mindcare.model.User;
import com.mindcare.services.AppointmentService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GestionRendezVousLegacyContent implements NavigationManager.Buildable {

    private enum Mode {
        PENDING,
        OTHERS
    }

    private final AppointmentService appointmentService = new AppointmentService();
    private final ObservableList<Appointment> appointments = FXCollections.observableArrayList();
    private Mode currentMode = Mode.PENDING;
    private int currentPsychologistId = -1;

    @Override
    public Node build() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        currentPsychologistId = currentUser != null ? currentUser.getId() : -1;
        loadAppointments();
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.getStyleClass().add("module-content");
        content.setPadding(new Insets(28, 32, 28, 32));
        content.getChildren().addAll(buildHeader(), buildActionsRow(content), buildTable());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private HBox buildActionsRow(VBox content) {
        Button pendingBtn = new Button("Rendez-vous en attente");
        pendingBtn.getStyleClass().addAll("btn", currentMode == Mode.PENDING ? "btn-primary" : "btn-secondary");
        pendingBtn.setOnAction(e -> {
            currentMode = Mode.PENDING;
            refreshContent(content);
        });

        Button othersBtn = new Button("Autres rendez-vous");
        othersBtn.getStyleClass().addAll("btn", currentMode == Mode.OTHERS ? "btn-primary" : "btn-secondary");
        othersBtn.setOnAction(e -> {
            currentMode = Mode.OTHERS;
            refreshContent(content);
        });

        Button refreshBtn = new Button("Rafraichir");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> refreshContent(content));

        Button statsBtn = new Button("Statistique");
        statsBtn.getStyleClass().addAll("btn", "btn-secondary");
        statsBtn.setOnAction(e -> NavigationManager.getInstance()
            .navigateContent("Statistiques rendez-vous", new GestionRendezVousStatsLegacyContent()));

        HBox actions = new HBox(10, pendingBtn, othersBtn, refreshBtn, statsBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private void refreshContent(VBox content) {
        loadAppointments();
        content.getChildren().setAll(buildHeader(), buildActionsRow(content), buildTable());
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        Label currentTitle = new Label(currentMode == Mode.PENDING ? "Rendez-vous en attente" : "Rendez-vous traites");
        currentTitle.getStyleClass().add("page-title");
        Label currentSubtitle = new Label(currentMode == Mode.PENDING
            ? "Valider ou refuser les rendez-vous en attente pour vous."
            : "Consulter et modifier les rendez-vous non en attente, puis ouvrir le dossier medical.");
        currentSubtitle.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(currentTitle, currentSubtitle);
        return header;
    }

    private VBox buildTable() {
        TextField searchField = new TextField();
        searchField.setPromptText("Recherche...");
        searchField.getStyleClass().add("module-search-field");

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Date (plus recente)", "Date (plus ancienne)", "Etudiant (A-Z)", "Statut (A-Z)");
        sortBox.setValue("Date (plus recente)");
        sortBox.getStyleClass().add("module-sort-box");

        HBox filtersRow = new HBox(10, searchField, sortBox);
        filtersRow.getStyleClass().add("module-filters-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox cardsContainer = new VBox(10);
        cardsContainer.getStyleClass().add("appointment-cards-container");
        ScrollPane cardsScroll = new ScrollPane(cardsContainer);
        cardsScroll.setFitToWidth(true);
        cardsScroll.setPrefHeight(500);
        cardsScroll.getStyleClass().add("scroll-pane");

        FilteredList<Appointment> filtered = new FilteredList<>(appointments, a -> matchesMode(a));
        SortedList<Appointment> sorted = new SortedList<>(filtered);
        sorted.setComparator((a, b) -> compareByOption(a, b, sortBox.getValue()));

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filtered.setPredicate(appointment -> matchesMode(appointment) && (query.isEmpty()
                || contains(appointment.getLocation(), query)
                || contains(appointment.getStatus(), query)
                || contains(appointment.getStudentName(), query)
                || contains(appointment.getDossierReference(), query)
                || contains(appointment.getDescription(), query)
                || contains(appointment.getDateTimeDisplay(), query)));
        });

        sortBox.valueProperty().addListener((obs, oldValue, newValue) -> sorted.setComparator((a, b) -> compareByOption(a, b, newValue)));

        Runnable refreshCards = () -> {
            cardsContainer.getChildren().clear();
            for (Appointment appointment : sorted) {
                cardsContainer.getChildren().add(buildPsychologueAppointmentCard(appointment));
            }
            if (cardsContainer.getChildren().isEmpty()) {
                Label empty = new Label("Aucun rendez-vous trouve.");
                empty.getStyleClass().add("label-muted");
                cardsContainer.getChildren().add(empty);
            }
        };

        sorted.addListener((ListChangeListener<Appointment>) change -> refreshCards.run());
        refreshCards.run();

        VBox card = new VBox(10, filtersRow, cardsScroll);
        card.getStyleClass().addAll("card", "appointments-board");
        card.setPadding(new Insets(10));
        return card;
    }

    private VBox buildPsychologueAppointmentCard(Appointment current) {
        Label dateLabel = new Label("Date/Heure: " + emptySafe(current.getDateTimeDisplay()));
        Label studentLabel = new Label("Etudiant: " + displayUser(current.getStudentName(), current.getStudentId()));
        Label dossierLabel = new Label("Dossier: " + current.getDossierReference());
        Label locationLabel = new Label("Lieu: " + emptySafe(current.getLocation()));
        Label descriptionLabel = new Label("Description: " + emptySafe(current.getDescription()));

        BadgeLabel statusBadge = BadgeLabel.forStatus(emptySafe(current.getStatus()).toUpperCase());
        HBox statusRow = new HBox(8, new Label("Statut:"), statusBadge);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button dossierBtn = new Button("Voir Dossier");
        dossierBtn.getStyleClass().addAll("btn", "btn-info", "btn-sm");
        dossierBtn.setOnAction(e -> openPatientFile(current));

        Button detailsBtn = new Button("Détails");
        detailsBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        detailsBtn.setOnAction(e -> showAppointmentDetails(current));

        Button acceptBtn = new Button("Accepter");
        acceptBtn.getStyleClass().addAll("btn", "btn-success", "btn-sm");
        acceptBtn.setOnAction(e -> changeStatus(current, "accepted"));

        Button rejectBtn = new Button("Refuser");
        rejectBtn.getStyleClass().addAll("btn", "btn-danger", "btn-sm");
        rejectBtn.setOnAction(e -> changeStatus(current, "rejected"));

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        editBtn.setOnAction(e -> openEditor(current));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger", "btn-sm");
        deleteBtn.setOnAction(e -> deleteAppointment(current));

        Button reportBtn = new Button("Ajouter rapport");
        reportBtn.getStyleClass().addAll("btn", "btn-info", "btn-sm");
        reportBtn.setOnAction(e -> openReportUploadDialog(current));

        Button viewReportBtn = new Button("Voir rapport");
        viewReportBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        viewReportBtn.setDisable(current.getReportName() == null || current.getReportName().isBlank());
        viewReportBtn.setOnAction(e -> openReportFile(current));

        HBox topRow = new HBox(6, dossierBtn, detailsBtn);
        HBox bottomRow = new HBox(6);
        if (currentMode == Mode.PENDING) {
            bottomRow.getChildren().setAll(acceptBtn, rejectBtn);
        } else {
            String status = normalizeStatus(current.getStatus());
            if ("cancelled".equals(status)) {
                bottomRow.getChildren().setAll(deleteBtn);
            } else if ("completed".equals(status)) {
                bottomRow.getChildren().setAll(reportBtn, viewReportBtn, deleteBtn);
            } else {
                bottomRow.getChildren().setAll(editBtn, deleteBtn);
            }
        }

        VBox card = new VBox(8, dateLabel, studentLabel, dossierLabel, locationLabel, descriptionLabel, statusRow, topRow, bottomRow);
        card.getStyleClass().addAll("card", "appointment-card");
        card.setPadding(new Insets(12));
        return card;
    }

    private boolean matchesMode(Appointment appointment) {
        String status = emptySafe(appointment.getStatus()).toLowerCase();
        boolean pending = "pending".equals(status);
        return currentMode == Mode.PENDING ? pending : !pending;
    }

    private void loadAppointments() {
        if (currentPsychologistId <= 0) {
            appointments.clear();
            return;
        }

        List<Appointment> loaded = appointmentService.findForPsychologist(currentPsychologistId);
        appointments.setAll(loaded);
    }

    private void openEditor(Appointment existing) {
        if ("cancelled".equals(normalizeStatus(existing == null ? null : existing.getStatus()))) {
            showError("Action non autorisee", "Un rendez-vous annule ne peut pas etre modifie. Vous pouvez seulement le supprimer.");
            return;
        }

        Dialog<Appointment> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Modifier rendez-vous" : "Modifier rendez-vous");
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ObservableList<User> students = FXCollections.observableArrayList(appointmentService.getStudents());
        ObservableList<User> psychologists = FXCollections.observableArrayList(appointmentService.getPsychologists());

        DatePicker datePicker = new DatePicker(existing != null && existing.getDateTime() != null
            ? existing.getDateTime().toLocalDate()
            : LocalDate.now());

        ComboBox<String> timeBox = new ComboBox<>();
        String preferredTime = existing != null && existing.getDateTime() != null
            ? existing.getDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            : "09:00";

        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("online", "in office");
        String existingLocation = existing == null ? "in office" : emptySafe(existing.getLocation()).toLowerCase();
        locationBox.setValue("online".equals(existingLocation) ? "online" : "in office");

        ComboBox<User> studentCombo = new ComboBox<>(students);
        studentCombo.setConverter(userToStringConverter());
        studentCombo.setCellFactory(param -> userListCell());
        if (existing != null && existing.getStudentId() != null) {
            students.stream().filter(u -> u.getId() == existing.getStudentId()).findFirst().ifPresent(studentCombo::setValue);
        }
        // Psychologue cannot reassign appointment to another student.
        studentCombo.setDisable(true);

        ComboBox<User> psyCombo = new ComboBox<>(psychologists);
        psyCombo.setConverter(userToStringConverter());
        psyCombo.setCellFactory(param -> userListCell());
        if (existing != null && existing.getPsyId() != null) {
            psychologists.stream().filter(u -> u.getId() == existing.getPsyId()).findFirst().ifPresent(psyCombo::setValue);
        }
        if (currentPsychologistId > 0) {
            psychologists.stream().filter(u -> u.getId() == currentPsychologistId).findFirst().ifPresent(psyCombo::setValue);
        }
        // Psychologue cannot reassign appointment to another psychologist.
        psyCombo.setDisable(true);

        Integer selectedPsyId = psyCombo.getValue() == null ? null : psyCombo.getValue().getId();
        refreshAvailableSlots(timeBox, selectedPsyId, datePicker.getValue(), existing == null ? null : existing.getId(), preferredTime);
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            Integer psyId = psyCombo.getValue() == null ? null : psyCombo.getValue().getId();
            refreshAvailableSlots(timeBox, psyId, newDate, existing == null ? null : existing.getId(), timeBox.getValue());
        });
        psyCombo.valueProperty().addListener((obs, oldUser, newUser) -> {
            Integer psyId = newUser == null ? null : newUser.getId();
            refreshAvailableSlots(timeBox, psyId, datePicker.getValue(), existing == null ? null : existing.getId(), timeBox.getValue());
        });

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("pending", "accepted", "rejected", "cancelled", "completed");
        statusBox.setValue(existing != null ? emptySafe(existing.getStatus()) : "pending");

        TextArea descriptionArea = new TextArea(existing == null ? "" : emptySafe(existing.getDescription()));
        descriptionArea.setPrefRowCount(3);

        Label inlineError = new Label();
        inlineError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12; -fx-font-weight: bold;");
        inlineError.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Date"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Heure"), 0, 1);
        grid.add(timeBox, 1, 1);
        grid.add(new Label("Lieu"), 0, 2);
        grid.add(locationBox, 1, 2);
        grid.add(new Label("Etudiant"), 0, 3);
        grid.add(studentCombo, 1, 3);
        grid.add(new Label("Psychologue"), 0, 4);
        grid.add(psyCombo, 1, 4);
        grid.add(new Label("Statut"), 0, 5);
        grid.add(statusBox, 1, 5);
        grid.add(new Label("Description"), 0, 6);
        grid.add(descriptionArea, 1, 6);
        grid.add(inlineError, 1, 7);

        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String validationError = validateAppointmentInput(datePicker.getValue(), timeBox.getValue(), studentCombo.getValue(), psyCombo.getValue(), descriptionArea.getText());
            if (validationError != null) {
                inlineError.setText(validationError);
                event.consume();
            } else {
                inlineError.setText("");
            }
        });

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }

            Appointment target = existing == null ? new Appointment() : existing;
            LocalDateTime selectedDateTime = LocalDateTime.of(datePicker.getValue(), parseTime(timeBox.getValue()));
            target.setDateTime(selectedDateTime);
            target.setLocation(locationBox.getValue());
            target.setDescription(normalizeDescription(descriptionArea.getText()));
            // Keep identity fields immutable in psychologue edit flow.
            target.setStudentId(existing != null ? existing.getStudentId() : (studentCombo.getValue() == null ? null : studentCombo.getValue().getId()));
            target.setPsyId(existing != null ? existing.getPsyId() : (psyCombo.getValue() == null ? null : psyCombo.getValue().getId()));
            target.setStatus(statusBox.getValue());
            return target;
        });

        try {
            Optional<Appointment> result = dialog.showAndWait();
            if (result.isPresent()) {
                appointmentService.updateByPsychologist(result.get());
                loadAppointments();
            }
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private void changeStatus(Appointment appointment, String status) {
        try {
            appointmentService.updateStatus(appointment.getId(), status);
            loadAppointments();
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private void deleteAppointment(Appointment appointment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le rendez-vous #" + appointment.getId() + " ?",
            ButtonType.YES, ButtonType.NO);
        confirm.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        confirm.setHeaderText("Confirmation");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.YES) {
            return;
        }

        try {
            appointmentService.deleteById(appointment.getId());
            loadAppointments();
        } catch (Exception exception) {
            showError("Delete failed", rootMessage(exception));
        }
    }

    private void openPatientFile(Appointment appointment) {
        try {
            Integer studentId = appointment.getStudentId();
            PatientFile patientFile = studentId != null ? appointmentService.getPatientFileByStudentId(studentId) : null;
            String studentName = appointment.getStudentName() != null ? appointment.getStudentName() : "Etudiant";
            com.mindcare.legacy.admin.DossierMedicalLegacyContent.setPatientFile(patientFile, studentName, studentId);
            NavigationManager.getInstance().navigateContent("Dossier medical - " + studentName, new com.mindcare.legacy.admin.DossierMedicalLegacyContent());
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private String validateAppointmentInput(LocalDate date, String time, User student, User psychologist, String description) {
        if (date == null) {
            return "La date est obligatoire.";
        }
        if (student == null) {
            return "L'etudiant est obligatoire.";
        }
        if (psychologist == null) {
            return "Le psychologue est obligatoire.";
        }
        LocalTime parsedTime;
        try {
            parsedTime = parseTime(time == null ? "" : time.trim());
        } catch (Exception ex) {
            return "Heure invalide. Format attendu: HH:mm.";
        }
        LocalDateTime selectedDateTime = LocalDateTime.of(date, parsedTime);
        if (selectedDateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            return "Le rendez-vous doit etre au moins 1 heure apres l'heure actuelle.";
        }
        String trimmedDescription = description == null ? "" : description.trim();
        if (!trimmedDescription.isEmpty() && trimmedDescription.length() < 6) {
            return "La description doit contenir au moins 6 caracteres si elle est renseignee.";
        }
        return null;
    }

    private List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        while (!time.isAfter(LocalTime.of(17, 0))) {
            slots.add(time.format(formatter));
            time = time.plusMinutes(15);
        }
        return slots;
    }

    private void refreshAvailableSlots(ComboBox<String> timeBox, Integer psyId, LocalDate selectedDate, Integer excludedAppointmentId, String preferredTime) {
        timeBox.getItems().clear();
        if (psyId == null || selectedDate == null) {
            return;
        }

        List<String> allSlots = buildTimeSlots();
        List<String> occupiedStartTimes = appointmentService.getOccupiedSlotsForPsychologist(psyId, selectedDate, excludedAppointmentId);
        LocalDateTime minAllowed = LocalDateTime.now().plusHours(1);

        List<LocalTime> occupiedTimes = new ArrayList<>();
        for (String startTime : occupiedStartTimes) {
            try {
                occupiedTimes.add(LocalTime.parse(startTime));
            } catch (Exception ignored) {
            }
        }

        for (String slot : allSlots) {
            LocalTime slotTime = LocalTime.parse(slot);

            boolean isBlocked = false;
            for (LocalTime occupiedStart : occupiedTimes) {
                LocalTime occupiedEnd = occupiedStart.plusHours(1);
                if (!slotTime.isBefore(occupiedStart) && slotTime.isBefore(occupiedEnd)) {
                    isBlocked = true;
                    break;
                }
            }

            if (isBlocked) {
                continue;
            }

            LocalDateTime slotDateTime = LocalDateTime.of(selectedDate, slotTime);
            if (slotDateTime.isBefore(minAllowed)) {
                continue;
            }
            timeBox.getItems().add(slot);
        }

        if (preferredTime != null && timeBox.getItems().contains(preferredTime)) {
            timeBox.setValue(preferredTime);
        } else if (!timeBox.getItems().isEmpty()) {
            timeBox.setValue(timeBox.getItems().get(0));
        } else {
            timeBox.setPromptText("Aucun creneau disponible");
        }
    }

    private javafx.util.StringConverter<User> userToStringConverter() {
        return new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFirstName() + " " + user.getLastName() + " (ID: " + user.getId() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        };
    }

    private ListCell<User> userListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getFirstName() + " " + user.getLastName() + " (ID: " + user.getId() + ")");
            }
        };
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalTime.of(9, 0);
        }
        return LocalTime.parse(value.length() == 5 ? value : value.substring(0, 5));
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int compareByOption(Appointment a, Appointment b, String option) {
        String selected = option == null ? "Date (plus recente)" : option;
        switch (selected) {
            case "Date (plus ancienne)":
                return Comparator.nullsLast(LocalDateTime::compareTo).compare(a.getDateTime(), b.getDateTime());
            case "Etudiant (A-Z)":
                return safeCompare(a.getStudentName(), b.getStudentName());
            case "Statut (A-Z)":
                return safeCompare(a.getStatus(), b.getStatus());
            case "Date (plus recente)":
            default:
                return Comparator.nullsLast(LocalDateTime::compareTo).compare(b.getDateTime(), a.getDateTime());
        }
    }

    private int safeCompare(String left, String right) {
        String l = left == null ? "" : left.toLowerCase();
        String r = right == null ? "" : right.toLowerCase();
        return l.compareTo(r);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
        );
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Unexpected error" : message);
        alert.showAndWait();
    }

    private String displayUser(String name, Integer id) {
        String safeName = emptySafe(name);
        if (safeName.isBlank()) {
            return id == null ? "N/A" : "ID " + id;
        }
        return id == null ? safeName : safeName + " (ID " + id + ")";
    }

    private String emptySafe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private void showAppointmentDetails(Appointment appointment) {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails du rendez-vous");
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/mindcare/styles/orion-theme.css").toExternalForm()
            );
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            String dateTimeStr = appointment.getDateTime() != null
                ? appointment.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";
            String location = emptySafe(appointment.getLocation());
            String studentName = emptySafe(appointment.getStudentName());
            String psychologistName = emptySafe(appointment.getPsyName());
            String status = emptySafe(appointment.getStatus());
            String description = emptySafe(appointment.getDescription());

            Label dateLabel = new Label("Date et Heure:");
            dateLabel.getStyleClass().add("label-bold");
            Label dateValue = new Label(dateTimeStr);

            Label locationLabel = new Label("Lieu:");
            locationLabel.getStyleClass().add("label-bold");
            Label locationValue = new Label(location);

            Label studentLabel = new Label("Étudiant:");
            studentLabel.getStyleClass().add("label-bold");
            Label studentValue = new Label(studentName);

            Label psychoLabel = new Label("Psychologue:");
            psychoLabel.getStyleClass().add("label-bold");
            Label psychoValue = new Label(psychologistName);

            Label statusLabel = new Label("Statut:");
            statusLabel.getStyleClass().add("label-bold");
            Label statusValue = new Label(status);

            Label descriptionLabel = new Label("Description:");
            descriptionLabel.getStyleClass().add("label-bold");
            TextArea descriptionArea = new TextArea(description);
            descriptionArea.setWrapText(true);
            descriptionArea.setEditable(false);
            descriptionArea.setPrefRowCount(4);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(15));
            grid.add(dateLabel, 0, 0);
            grid.add(dateValue, 1, 0);
            grid.add(locationLabel, 0, 1);
            grid.add(locationValue, 1, 1);
            grid.add(studentLabel, 0, 2);
            grid.add(studentValue, 1, 2);
            grid.add(psychoLabel, 0, 3);
            grid.add(psychoValue, 1, 3);
            grid.add(statusLabel, 0, 4);
            grid.add(statusValue, 1, 4);
            grid.add(descriptionLabel, 0, 5);
            grid.add(descriptionArea, 0, 6, 2, 1);
            GridPane.setHgrow(descriptionArea, Priority.ALWAYS);

            dialog.getDialogPane().setContent(grid);
            dialog.showAndWait();
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private void openReportUploadDialog(Appointment appointment) {
        if (appointment == null) {
            return;
        }
        if (!"completed".equals(normalizeStatus(appointment.getStatus()))) {
            showError("Action non autorisee", "Le rapport est autorise uniquement pour les rendez-vous termines.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selectionner un rapport");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt")
        );

        File selected = chooser.showOpenDialog(null);
        if (selected == null) {
            return;
        }

        try {
            appointmentService.uploadPsychologistReport(appointment.getId(), currentPsychologistId, selected.toPath());
            loadAppointments();
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }

    private void openReportFile(Appointment appointment) {
        try {
            Path reportPath = appointmentService.resolveAppointmentReportPath(appointment.getReportName());
            if (reportPath == null || !Files.exists(reportPath)) {
                showError("Fichier introuvable", "Le rapport est introuvable sur le disque.");
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                showError("Action non supportee", "Ouverture de fichier non supportee sur cette machine.");
                return;
            }
            Desktop.getDesktop().open(reportPath.toFile());
        } catch (Exception exception) {
            showError("Erreur", rootMessage(exception));
        }
    }
}
