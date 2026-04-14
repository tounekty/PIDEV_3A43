package com.mindcare.legacy.admin;

import com.mindcare.model.PatientFile;
import com.mindcare.services.AppointmentService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class DossierMedicalLegacyContent implements com.mindcare.utils.NavigationManager.Buildable {

    private static PatientFile currentPatientFile;
    private static String currentStudentName;
    private static Integer currentStudentId;
    private final AppointmentService appointmentService = new AppointmentService();

    public static void setPatientFile(PatientFile file, String studentName, Integer studentId) {
        currentPatientFile = file;
        currentStudentName = studentName;
        currentStudentId = studentId;
    }

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        VBox header = buildHeader();
        VBox form = buildEditableForm();
        content.getChildren().addAll(header, form);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildEditableForm() {
        if (currentStudentId == null || currentStudentId <= 0) {
            VBox invalid = new VBox(10);
            invalid.getStyleClass().add("card");
            invalid.setPadding(new Insets(16));
            invalid.getChildren().add(new Label("Étudiant invalide. Impossible de modifier le dossier médical."));
            return invalid;
        }

        if (currentPatientFile == null) {
            currentPatientFile = new PatientFile();
            currentPatientFile.setStudentId(currentStudentId);
        }

        VBox content = new VBox(10);
        content.getStyleClass().add("card");
        content.setPadding(new Insets(15));

        TextArea traitementsField = area(currentPatientFile.getTraitementsEnCours());
        TextArea allergiesField = area(currentPatientFile.getAllergies());
        TextField contactNomField = input(currentPatientFile.getContactUrgenceNom());
        TextField contactTelField = input(currentPatientFile.getContactUrgenceTel());
        TextArea antecedentsPersField = area(currentPatientFile.getAntecedentsPersonnels());
        TextArea antecedentsFamField = area(currentPatientFile.getAntecedentsFamiliaux());
        TextArea motifField = area(currentPatientFile.getMotifConsultation());
        TextArea objectifsField = area(currentPatientFile.getObjectifsTherapeutiques());
        TextArea notesField = area(currentPatientFile.getNotesGenerales());
        TextField risqueField = input(currentPatientFile.getNiveauRisque());

        addField(content, "Traitements en cours", traitementsField);
        addField(content, "Allergies", allergiesField);
        addField(content, "Contact d'urgence (Nom)", contactNomField);
        addField(content, "Contact d'urgence (Téléphone)", contactTelField);
        addField(content, "Antécédents personnels", antecedentsPersField);
        addField(content, "Antécédents familiaux", antecedentsFamField);
        addField(content, "Motif de consultation", motifField);
        addField(content, "Objectifs thérapeutiques", objectifsField);
        addField(content, "Notes générales", notesField);
        addField(content, "Niveau de risque", risqueField);

        HBox actions = new HBox(10);
        Button saveBtn = new Button("Enregistrer");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");

        Label inlineError = new Label();
        inlineError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12; -fx-font-weight: bold;");
        inlineError.setWrapText(true);

        Label inlineSuccess = new Label();
        inlineSuccess.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 12; -fx-font-weight: bold;");
        inlineSuccess.setWrapText(true);

        saveBtn.setOnAction(e -> {
            try {
            inlineError.setText("");
                inlineSuccess.setText("");
                currentPatientFile.setStudentId(currentStudentId);
                currentPatientFile.setTraitementsEnCours(clean(traitementsField.getText()));
                currentPatientFile.setAllergies(clean(allergiesField.getText()));
                currentPatientFile.setContactUrgenceNom(clean(contactNomField.getText()));
                currentPatientFile.setContactUrgenceTel(clean(contactTelField.getText()));
                currentPatientFile.setAntecedentsPersonnels(clean(antecedentsPersField.getText()));
                currentPatientFile.setAntecedentsFamiliaux(clean(antecedentsFamField.getText()));
                currentPatientFile.setMotifConsultation(clean(motifField.getText()));
                currentPatientFile.setObjectifsTherapeutiques(clean(objectifsField.getText()));
                currentPatientFile.setNotesGenerales(clean(notesField.getText()));
                currentPatientFile.setNiveauRisque(clean(risqueField.getText()));

                currentPatientFile = appointmentService.savePatientFile(currentPatientFile);
                inlineSuccess.setText("Dossier medical enregistre avec succes.");
            } catch (IllegalArgumentException validationException) {
                inlineSuccess.setText("");
                inlineError.setText(rootMessage(validationException));
            } catch (Exception exception) {
                inlineSuccess.setText("");
                showError("Erreur", rootMessage(exception));
            }
        });

        actions.getChildren().add(saveBtn);
        content.getChildren().add(inlineError);
        content.getChildren().add(inlineSuccess);
        content.getChildren().add(actions);
        return content;
    }

    private TextField input(String value) {
        TextField field = new TextField();
        field.setText(cleanForUi(value));
        return field;
    }

    private TextArea area(String value) {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefRowCount(3);
        area.setText(cleanForUi(value));
        return area;
    }

    private String cleanForUi(String value) {
        if (value == null || "[Vide]".equals(value)) {
            return "";
        }
        return value;
    }

    private void addField(VBox container, String label, Node input) {
        VBox block = new VBox(4);
        Label labelControl = new Label(label);
        labelControl.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #333;");
        block.getChildren().addAll(labelControl, input);
        container.getChildren().add(block);
    }

    private VBox buildHeader() {
        VBox header = new VBox(8);

        Label title = new Label("Dossier Médical");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");

        String studentLabel = currentStudentName != null ? currentStudentName : "Étudiant";
        Label subtitle = new Label("Patient: " + studentLabel);
        subtitle.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Retour");
        backBtn.getStyleClass().addAll("btn", "btn-secondary");
        backBtn.setOnAction(e -> navigateBack());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonRow.getChildren().addAll(backBtn, spacer);

        header.getChildren().addAll(title, subtitle, buttonRow);
        return header;
    }

    private void navigateBack() {
        com.mindcare.utils.NavigationManager navigationManager = com.mindcare.utils.NavigationManager.getInstance();
        if (com.mindcare.utils.SessionManager.getInstance().hasRole(com.mindcare.model.User.Role.PSYCHOLOGUE)) {
            navigationManager.navigateContent("Gestion rendez-vous", new com.mindcare.view.psychologue.GestionRendezVousView());
        } else {
            navigationManager.navigateContent("Gestion rendez-vous", new GestionReservationsLegacyContent());
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() < 6) {
            throw new IllegalArgumentException("Chaque champ du dossier doit contenir au moins 6 caracteres s'il est renseigne.");
        }
        return trimmed;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Unexpected error" : message);
        alert.showAndWait();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
