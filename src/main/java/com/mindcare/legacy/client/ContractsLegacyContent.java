package com.mindcare.legacy.client;

import com.mindcare.components.BadgeLabel;
import com.mindcare.model.Appointment;
import com.mindcare.model.PatientFile;
import com.mindcare.model.User;
import com.mindcare.services.AppointmentService;
import com.mindcare.utils.NavigationManager;
import com.mindcare.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ContractsLegacyContent implements NavigationManager.Buildable {

    private enum Mode {
        BOOK,
        MY_APPOINTMENTS,
        MY_DOSSIER
    }

    private final AppointmentService appointmentService = new AppointmentService();
    private final VBox content = new VBox(16);
    private User selectedPsychologue;
    private Mode mode = Mode.BOOK;

    @Override
    public Node build() {
        content.setPadding(new Insets(0));
        render();

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private void render() {
        content.getChildren().clear();
        content.getChildren().addAll(buildMainHeader(), buildModeButtons());

        if (mode == Mode.MY_APPOINTMENTS) {
            content.getChildren().add(buildMyAppointments());
            return;
        }

        if (mode == Mode.MY_DOSSIER) {
            content.getChildren().add(buildMyDossier());
            return;
        }

        if (selectedPsychologue == null) {
            content.getChildren().addAll(buildBookHeader(), buildPsychologueCards());
            return;
        }

        content.getChildren().addAll(buildCreateHeader(), buildCreateForm(selectedPsychologue));
    }

    private VBox buildMainHeader() {
        Label title = new Label("Prenez un rendez-vous");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Choisissez une action: nouveau rendez-vous, suivi de vos rendez-vous, ou dossier médical.");
        sub.getStyleClass().add("page-subtitle");
        return new VBox(6, title, sub);
    }

    private HBox buildModeButtons() {
        Button bookBtn = new Button("Prendre un rendez-vous");
        bookBtn.getStyleClass().addAll("btn", mode == Mode.BOOK ? "btn-primary" : "btn-outline", "btn-sm");
        bookBtn.setOnAction(e -> {
            mode = Mode.BOOK;
            selectedPsychologue = null;
            render();
        });

        Button myAppointmentsBtn = new Button("Mes rendez-vous");
        myAppointmentsBtn.getStyleClass().addAll("btn", mode == Mode.MY_APPOINTMENTS ? "btn-primary" : "btn-outline", "btn-sm");
        myAppointmentsBtn.setOnAction(e -> {
            mode = Mode.MY_APPOINTMENTS;
            selectedPsychologue = null;
            render();
        });

        Button myDossierBtn = new Button("Mon dossier médical");
        myDossierBtn.getStyleClass().addAll("btn", mode == Mode.MY_DOSSIER ? "btn-primary" : "btn-outline", "btn-sm");
        myDossierBtn.setOnAction(e -> {
            mode = Mode.MY_DOSSIER;
            selectedPsychologue = null;
            render();
        });

        return new HBox(8, bookBtn, myAppointmentsBtn, myDossierBtn);
    }

    private VBox buildBookHeader() {
        Label sub = new Label("Choisissez un psychologue pour commencer la demande de rendez-vous.");
        sub.getStyleClass().add("page-subtitle");
        return new VBox(6, sub);
    }

    private VBox buildPsychologueCards() {
        List<User> psychologues = appointmentService.getPsychologists();
        VBox cards = new VBox(12);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un psychologue...");

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Alphabetique (A-Z)", "Alphabetique (Z-A)");
        sortBox.setValue("Alphabetique (A-Z)");

        HBox tools = new HBox(10, searchField, sortBox);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox cardsContainer = new VBox(12);

        Runnable refreshCards = () -> {
            String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            List<User> filtered = psychologues.stream()
                .filter(u -> {
                    if (query.isEmpty()) {
                        return true;
                    }
                    String full = fullName(u).toLowerCase();
                    String email = u.getEmail() == null ? "" : u.getEmail().toLowerCase();
                    return full.contains(query) || email.contains(query);
                })
                .sorted((a, b) -> {
                    int cmp = fullName(a).compareToIgnoreCase(fullName(b));
                    return "Alphabetique (Z-A)".equals(sortBox.getValue()) ? -cmp : cmp;
                })
                .toList();

            cardsContainer.getChildren().clear();
            if (filtered.isEmpty()) {
                Label empty = new Label("Aucun psychologue trouvé.");
                empty.getStyleClass().add("label-muted");
                cardsContainer.getChildren().add(empty);
                return;
            }

            for (User psychologue : filtered) {
                cardsContainer.getChildren().add(buildPsychologueCard(psychologue));
            }
        };

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshCards.run());
        sortBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshCards.run());
        refreshCards.run();

        cards.getChildren().addAll(tools, cardsContainer);
        return cards;
    }

    private HBox buildPsychologueCard(User psychologue) {
        Label name = new Label(fullName(psychologue));
        name.getStyleClass().add("card-title");

        Label email = new Label(psychologue.getEmail() == null ? "" : psychologue.getEmail());
        email.getStyleClass().add("label-secondary");

        VBox info = new VBox(4, name, email);

        Button chooseBtn = new Button("Choisir");
        chooseBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        chooseBtn.setOnAction(e -> {
            selectedPsychologue = psychologue;
            render();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox card = new HBox(12, info, spacer, chooseBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("card");
        card.setOnMouseClicked(e -> {
            selectedPsychologue = psychologue;
            render();
        });
        return card;
    }

    private VBox buildCreateHeader() {
        Label title = new Label("Nouveau rendez-vous");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Psychologue sélectionné: " + fullName(selectedPsychologue));
        sub.getStyleClass().add("page-subtitle");
        return new VBox(6, title, sub);
    }

    private VBox buildCreateForm(User psychologue) {
        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<String> timeBox = new ComboBox<>();
        refreshAvailableSlots(timeBox, psychologue, datePicker.getValue());
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshAvailableSlots(timeBox, psychologue, newDate));

        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("online", "in office");
        locationBox.setValue("in office");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description (optionnel, min 6 caractères si rempli)");
        descriptionArea.setPrefRowCount(4);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("text-danger");
        errorLabel.setVisible(false);

        Label successLabel = new Label();
        successLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
        successLabel.setVisible(false);

        Button backBtn = new Button("Retour");
        backBtn.getStyleClass().addAll("btn", "btn-outline", "btn-sm");
        backBtn.setOnAction(e -> {
            selectedPsychologue = null;
            render();
        });

        Button createBtn = new Button("Confirmer le rendez-vous");
        createBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        createBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            errorLabel.setText("");
            successLabel.setVisible(false);
            successLabel.setText("");

            String validation = validateInput(datePicker.getValue(), timeBox.getValue(), locationBox.getValue(), descriptionArea.getText());
            if (validation != null) {
                errorLabel.setText(validation);
                errorLabel.setVisible(true);
                return;
            }

            User currentStudent = SessionManager.getInstance().getCurrentUser();
            if (currentStudent == null) {
                errorLabel.setText("Session invalide. Veuillez vous reconnecter.");
                errorLabel.setVisible(true);
                return;
            }

            Appointment appointment = new Appointment();
            appointment.setDateTime(LocalDateTime.of(datePicker.getValue(), LocalTime.parse(timeBox.getValue())));
            appointment.setLocation(locationBox.getValue());
            appointment.setDescription(cleanDescription(descriptionArea.getText()));
            appointment.setStatus("pending");
            appointment.setStudentId(currentStudent.getId());
            appointment.setPsyId(psychologue.getId());

            try {
                appointmentService.create(appointment);
                successLabel.setText("Votre demande de rendez-vous a été envoyée avec succès.");
                successLabel.setVisible(true);
                datePicker.setValue(LocalDate.now());
                refreshAvailableSlots(timeBox, psychologue, datePicker.getValue());
                locationBox.setValue("in office");
                descriptionArea.clear();
            } catch (Exception exception) {
                errorLabel.setText(rootMessage(exception));
                errorLabel.setVisible(true);
            }
        });

        HBox buttons = new HBox(8, backBtn, createBtn);
        VBox card = new VBox(
            10,
            field("Date", datePicker),
            field("Heure", timeBox),
            field("Lieu", locationBox),
            field("Description", descriptionArea),
            errorLabel,
            successLabel,
            buttons
        );
        card.setPadding(new Insets(14));
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildMyAppointments() {
        User currentStudent = SessionManager.getInstance().getCurrentUser();
        int studentId = currentStudent == null ? -1 : currentStudent.getId();

        VBox wrapper = new VBox(10);
        Label title = new Label("Mes rendez-vous");
        title.getStyleClass().add("page-title");

        if (studentId <= 0) {
            Label error = new Label("Session invalide. Veuillez vous reconnecter.");
            error.getStyleClass().add("text-danger");
            wrapper.getChildren().addAll(title, error);
            return wrapper;
        }

        ObservableList<Appointment> data = FXCollections.observableArrayList(appointmentService.findForStudent(studentId));
        FilteredList<Appointment> filtered = new FilteredList<>(data, a -> true);
        SortedList<Appointment> sorted = new SortedList<>(filtered);

        Label messageLabel = new Label();
        messageLabel.setVisible(false);

        TextField searchField = new TextField();
        searchField.setPromptText("Recherche...");

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll(
            "Date (plus recente)",
            "Date (plus ancienne)",
            "Psychologue (A-Z)",
            "Psychologue (Z-A)",
            "Statut (A-Z)"
        );
        sortBox.setValue("Date (plus recente)");

        HBox filters = new HBox(10, searchField, sortBox);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        TableView<Appointment> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(56);
        table.setPrefHeight(460);

        TableColumn<Appointment, String> dateCol = new TableColumn<>("Date/Heure");
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDateTimeDisplay()));

        TableColumn<Appointment, String> psychologueCol = new TableColumn<>("Psychologue");
        psychologueCol.setCellValueFactory(cd -> new SimpleStringProperty(emptySafe(cd.getValue().getPsyName())));

        TableColumn<Appointment, String> locationCol = new TableColumn<>("Lieu");
        locationCol.setCellValueFactory(cd -> new SimpleStringProperty(emptySafe(cd.getValue().getLocation())));

        TableColumn<Appointment, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(cd -> new SimpleStringProperty(emptySafe(cd.getValue().getDescription())));

        TableColumn<Appointment, Void> statusCol = new TableColumn<>("Statut");
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(BadgeLabel.forStatus(emptySafe(getTableRow().getItem().getStatus()).toUpperCase()));
            }
        });

        TableColumn<Appointment, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setMinWidth(290);
        actionCol.setPrefWidth(290);
        actionCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                Appointment appointment = empty ? null : getTableRow().getItem();
                if (appointment == null) {
                    setGraphic(null);
                    return;
                }

                String status = normalizeStatus(appointment.getStatus());

                Button editBtn = new Button("Modifier");
                editBtn.getStyleClass().addAll("btn", "btn-outline", "btn-sm");
                editBtn.setMinWidth(84);
                editBtn.setDisable(!("pending".equals(status) || "accepted".equals(status)));
                editBtn.setOnAction(e -> openEditAppointmentDialog(appointment, studentId, data, messageLabel));

                Button deleteBtn = new Button("Supprimer");
                deleteBtn.getStyleClass().addAll("btn", "btn-danger", "btn-sm");
                deleteBtn.setMinWidth(92);
                deleteBtn.setDisable(!"pending".equals(status));
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmer la suppression");
                    confirm.setHeaderText("Supprimer ce rendez-vous en attente ?");
                    confirm.setContentText("Cette action est irreversible.");

                    ButtonType yes = new ButtonType("Oui", ButtonBar.ButtonData.OK_DONE);
                    ButtonType no = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
                    confirm.getButtonTypes().setAll(yes, no);

                    if (confirm.showAndWait().orElse(no) != yes) {
                        return;
                    }

                    try {
                        appointmentService.deletePendingByStudent(appointment.getId(), studentId);
                        reloadStudentAppointments(data, studentId);
                        messageLabel.setText("Rendez-vous supprime avec succes.");
                        messageLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
                        messageLabel.setVisible(true);
                    } catch (Exception exception) {
                        messageLabel.setText(rootMessage(exception));
                        messageLabel.getStyleClass().setAll("text-danger");
                        messageLabel.setVisible(true);
                    }
                });

                Button cancelBtn = new Button("Annuler");
                cancelBtn.getStyleClass().addAll("btn", "btn-outline", "btn-sm");
                cancelBtn.setMinWidth(84);
                cancelBtn.setDisable(!"accepted".equals(status));
                cancelBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmer l'annulation");
                    confirm.setHeaderText("Annuler ce rendez-vous accepte ?");
                    confirm.setContentText("Le statut passera a cancelled.");

                    ButtonType yes = new ButtonType("Oui", ButtonBar.ButtonData.OK_DONE);
                    ButtonType no = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
                    confirm.getButtonTypes().setAll(yes, no);

                    if (confirm.showAndWait().orElse(no) != yes) {
                        return;
                    }

                    try {
                        appointmentService.cancelAcceptedByStudent(appointment.getId(), studentId);
                        reloadStudentAppointments(data, studentId);
                        messageLabel.setText("Rendez-vous annule avec succes.");
                        messageLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
                        messageLabel.setVisible(true);
                    } catch (Exception exception) {
                        messageLabel.setText(rootMessage(exception));
                        messageLabel.getStyleClass().setAll("text-danger");
                        messageLabel.setVisible(true);
                    }
                });

                HBox actions = new HBox(6, editBtn, deleteBtn, cancelBtn);
                actions.setAlignment(Pos.CENTER_LEFT);
                setGraphic(actions);
            }
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filtered.setPredicate(appointment -> query.isEmpty()
                || contains(appointment.getDateTimeDisplay(), query)
                || contains(appointment.getPsyName(), query)
                || contains(appointment.getLocation(), query)
                || contains(appointment.getDescription(), query)
                || contains(appointment.getStatus(), query));
        });

        sortBox.valueProperty().addListener((obs, oldValue, newValue) ->
            sorted.setComparator((a, b) -> compareAppointments(a, b, newValue))
        );
        sorted.setComparator((a, b) -> compareAppointments(a, b, sortBox.getValue()));

        table.getColumns().addAll(dateCol, psychologueCol, locationCol, descriptionCol, statusCol, actionCol);
        table.setItems(sorted);

        VBox card = new VBox(10, filters, messageLabel, table);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("card");

        wrapper.getChildren().addAll(title, card);
        return wrapper;
    }

    private void openEditAppointmentDialog(Appointment appointment, int studentId, ObservableList<Appointment> data, Label messageLabel) {
        if (appointment == null || appointment.getPsyId() == null) {
            messageLabel.setText("Rendez-vous invalide.");
            messageLabel.getStyleClass().setAll("text-danger");
            messageLabel.setVisible(true);
            return;
        }

        Alert dialog = new Alert(Alert.AlertType.NONE);
        dialog.setTitle("Modifier le rendez-vous");
        dialog.setHeaderText("Modification autorisee (statut remis a pending)");

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getButtonTypes().setAll(saveType, ButtonType.CANCEL);

        LocalDate initialDate = appointment.getDateTime() == null ? LocalDate.now() : appointment.getDateTime().toLocalDate();
        String initialTime = appointment.getDateTime() == null ? "09:00" : appointment.getDateTime().toLocalTime().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm"));

        DatePicker datePicker = new DatePicker(initialDate);

        ComboBox<String> timeBox = new ComboBox<>();
        refreshAvailableSlots(timeBox, appointment.getPsyId(), initialDate, appointment.getId());
        if (timeBox.getItems().contains(initialTime)) {
            timeBox.setValue(initialTime);
        }
        datePicker.valueProperty().addListener((obs, oldDate, newDate) ->
            refreshAvailableSlots(timeBox, appointment.getPsyId(), newDate, appointment.getId())
        );

        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("online", "in office");
        locationBox.setValue(appointment.getLocation() == null || appointment.getLocation().isBlank() ? "in office" : appointment.getLocation());

        TextArea descriptionArea = new TextArea(valueOrEmpty(appointment.getDescription()));
        descriptionArea.setPromptText("Description (optionnel, min 6 caracteres si rempli)");
        descriptionArea.setPrefRowCount(4);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("text-danger");
        errorLabel.setVisible(false);

        VBox form = new VBox(
            10,
            field("Date", datePicker),
            field("Heure", timeBox),
            field("Lieu", locationBox),
            field("Description", descriptionArea),
            errorLabel
        );
        form.setPadding(new Insets(6, 0, 0, 0));
        dialog.getDialogPane().setContent(form);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        final boolean[] saved = {false};
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            errorLabel.setVisible(false);

            String validation = validateInput(datePicker.getValue(), timeBox.getValue(), locationBox.getValue(), descriptionArea.getText());
            if (validation != null) {
                errorLabel.setText(validation);
                errorLabel.setVisible(true);
                event.consume();
                return;
            }

            Appointment edited = new Appointment();
            edited.setId(appointment.getId());
            edited.setStudentId(studentId);
            edited.setPsyId(appointment.getPsyId());
            edited.setPatientFileId(appointment.getPatientFileId());
            edited.setDateTime(LocalDateTime.of(datePicker.getValue(), LocalTime.parse(timeBox.getValue())));
            edited.setLocation(locationBox.getValue());
            edited.setDescription(cleanDescription(descriptionArea.getText()));

            try {
                appointmentService.updateByStudentResetPending(edited, studentId);
                saved[0] = true;
            } catch (Exception exception) {
                errorLabel.setText(rootMessage(exception));
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        dialog.showAndWait();
        if (saved[0]) {
            reloadStudentAppointments(data, studentId);
            messageLabel.setText("Rendez-vous modifie avec succes. Statut remis a pending.");
            messageLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
            messageLabel.setVisible(true);
        }
    }

    private void reloadStudentAppointments(ObservableList<Appointment> data, int studentId) {
        data.setAll(appointmentService.findForStudent(studentId));
    }

    private VBox buildMyDossier() {
        User currentStudent = SessionManager.getInstance().getCurrentUser();
        int studentId = currentStudent == null ? -1 : currentStudent.getId();

        VBox wrapper = new VBox(10);
        Label title = new Label("Mon dossier médical");
        title.getStyleClass().add("page-title");

        Label info = new Label("Vous pouvez modifier uniquement les informations non cliniques (contact d'urgence).");
        info.getStyleClass().add("page-subtitle");

        if (studentId <= 0) {
            Label error = new Label("Session invalide. Veuillez vous reconnecter.");
            error.getStyleClass().add("text-danger");
            wrapper.getChildren().addAll(title, info, error);
            return wrapper;
        }

        PatientFile existing = appointmentService.getPatientFileByStudentId(studentId);

        TextField emergencyNameField = new TextField(valueOrEmpty(existing == null ? null : existing.getContactUrgenceNom()));
        emergencyNameField.setPromptText("Nom du contact d'urgence");

        TextField emergencyPhoneField = new TextField(valueOrEmpty(existing == null ? null : existing.getContactUrgenceTel()));
        emergencyPhoneField.setPromptText("Téléphone du contact d'urgence");

        Label clinicalLocked = new Label("Informations cliniques verrouillées pour le profil étudiant.");
        clinicalLocked.getStyleClass().add("label-muted");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("text-danger");
        errorLabel.setVisible(false);

        Label successLabel = new Label();
        successLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 12px;");
        successLabel.setVisible(false);

        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        saveBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            successLabel.setVisible(false);

            String nameValidation = validateEmergencyContactName(emergencyNameField.getText());
            if (nameValidation != null) {
                errorLabel.setText(nameValidation);
                errorLabel.setVisible(true);
                return;
            }

            String phoneValidation = validateEmergencyContactPhone(emergencyPhoneField.getText());
            if (phoneValidation != null) {
                errorLabel.setText(phoneValidation);
                errorLabel.setVisible(true);
                return;
            }

            PatientFile toSave = existing != null ? existing : new PatientFile();
            toSave.setStudentId(studentId);
            toSave.setContactUrgenceNom(cleanOptional(emergencyNameField.getText()));
            toSave.setContactUrgenceTel(cleanOptional(emergencyPhoneField.getText()));

            try {
                PatientFile saved = appointmentService.savePatientFileForStudent(toSave);
                successLabel.setText("Informations enregistrées avec succès.");
                successLabel.setVisible(true);
                emergencyNameField.setText(valueOrEmpty(saved.getContactUrgenceNom()));
                emergencyPhoneField.setText(valueOrEmpty(saved.getContactUrgenceTel()));
            } catch (Exception exception) {
                errorLabel.setText(rootMessage(exception));
                errorLabel.setVisible(true);
            }
        });

        VBox card = new VBox(
            10,
            field("Contact d'urgence - Nom", emergencyNameField),
            field("Contact d'urgence - Téléphone", emergencyPhoneField),
            clinicalLocked,
            errorLabel,
            successLabel,
            new HBox(saveBtn)
        );
        card.setPadding(new Insets(12));
        card.getStyleClass().add("card");

        wrapper.getChildren().addAll(title, info, card);
        return wrapper;
    }

    private VBox field(String labelText, Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("label-secondary");
        return new VBox(4, label, input);
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

    private void refreshAvailableSlots(ComboBox<String> timeBox, User psychologue, LocalDate selectedDate) {
        Integer psyId = psychologue == null ? null : psychologue.getId();
        refreshAvailableSlots(timeBox, psyId, selectedDate, null);
    }

    private void refreshAvailableSlots(ComboBox<String> timeBox, Integer psyId, LocalDate selectedDate, Integer excludedAppointmentId) {
        timeBox.getItems().clear();
        if (psyId == null || selectedDate == null) {
            return;
        }

        List<String> allSlots = buildTimeSlots();
        List<String> occupiedStartTimes = appointmentService.getOccupiedSlotsForPsychologist(psyId, selectedDate, excludedAppointmentId);
        LocalDateTime minAllowed = LocalDateTime.now().plusHours(1);

        // Convert occupied start times to LocalTime objects for 1-hour window checking
        List<LocalTime> occupiedTimes = new ArrayList<>();
        for (String startTime : occupiedStartTimes) {
            try {
                occupiedTimes.add(LocalTime.parse(startTime));
            } catch (Exception e) {
                // Skip invalid times
            }
        }

        for (String slot : allSlots) {
            LocalTime slotTime = LocalTime.parse(slot);
            
            // Check if this slot overlaps with any 1-hour occupied window
            boolean isBlocked = false;
            for (LocalTime occupiedStart : occupiedTimes) {
                LocalTime occupiedEnd = occupiedStart.plusHours(1);
                // Slot overlaps if it starts before occupied end and starts at/after occupied start
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

        if (!timeBox.getItems().isEmpty()) {
            timeBox.setValue(timeBox.getItems().get(0));
        } else {
            timeBox.setPromptText("Aucun créneau disponible");
        }
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private int compareAppointments(Appointment a, Appointment b, String option) {
        String selected = option == null ? "Date (plus recente)" : option;
        return switch (selected) {
            case "Date (plus ancienne)" -> Comparator.nullsLast(LocalDateTime::compareTo).compare(a.getDateTime(), b.getDateTime());
            case "Psychologue (A-Z)" -> safeCompare(a.getPsyName(), b.getPsyName());
            case "Psychologue (Z-A)" -> safeCompare(b.getPsyName(), a.getPsyName());
            case "Statut (A-Z)" -> safeCompare(a.getStatus(), b.getStatus());
            case "Date (plus recente)" -> Comparator.nullsLast(LocalDateTime::compareTo).compare(b.getDateTime(), a.getDateTime());
            default -> Comparator.nullsLast(LocalDateTime::compareTo).compare(b.getDateTime(), a.getDateTime());
        };
    }

    private int safeCompare(String left, String right) {
        String l = left == null ? "" : left.toLowerCase();
        String r = right == null ? "" : right.toLowerCase();
        return l.compareTo(r);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String validateInput(LocalDate date, String time, String location, String description) {
        if (date == null) {
            return "La date est obligatoire.";
        }
        if (time == null || time.isBlank()) {
            return "L'heure est obligatoire.";
        }
        if (location == null || location.isBlank()) {
            return "Le lieu est obligatoire.";
        }
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.parse(time));
        if (dateTime.isBefore(LocalDateTime.now().plusHours(1))) {
            return "Le rendez-vous doit être planifié au moins 1 heure à l'avance.";
        }
        String normalized = cleanDescription(description);
        if (normalized != null && normalized.length() < 6) {
            return "La description doit contenir au moins 6 caractères ou rester vide.";
        }
        return null;
    }

    private String validateNullableMin6(String value, String fieldName) {
        String clean = cleanOptional(value);
        if (clean == null) {
            return null;
        }
        if (clean.length() < 6) {
            return fieldName + " doit contenir au moins 6 caractères ou rester vide.";
        }
        return null;
    }

    private String validateEmergencyContactName(String value) {
        String clean = cleanOptional(value);
        if (clean == null) {
            return null; // Accepts null/empty
        }
        if (clean.length() < 3) {
            return "Le nom du contact d'urgence doit contenir au moins 3 caractères ou rester vide.";
        }
        if (clean.length() > 20) {
            return "Le nom du contact d'urgence ne doit pas dépasser 20 caractères.";
        }
        return null;
    }

    private String validateEmergencyContactPhone(String value) {
        String clean = cleanOptional(value);
        if (clean == null) {
            return null; // Accepts null/empty
        }
        if (!clean.matches("^\\d{8}$")) {
            return "Le téléphone du contact d'urgence doit contenir exactement 8 chiffres.";
        }
        return null;
    }

    private String cleanDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cleanOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "[Vide]".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String valueOrEmpty(String value) {
        return value == null || "[Vide]".equals(value) ? "" : value;
    }

    private String emptySafe(String value) {
        return value == null ? "" : value;
    }

    private String fullName(User user) {
        if (user == null) {
            return "N/A";
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? (user.getEmail() == null ? "N/A" : user.getEmail()) : full;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Erreur inattendue." : current.getMessage();
    }
}

