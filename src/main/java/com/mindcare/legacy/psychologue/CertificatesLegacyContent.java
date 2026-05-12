package com.mindcare.legacy.psychologue;

import com.mindcare.view.psychologue.*;

import com.mindcare.components.*;
import com.mindcare.model.Certificate;
import com.mindcare.service.MockDataService;
import com.mindcare.utils.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * CertificatesLegacyContent â€“ worker certificate management and upload.
 */
public class CertificatesLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(20);

        Label title = new Label("My Certificates");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Upload and manage your professional certifications");
        sub.getStyleClass().add("page-subtitle");

        content.getChildren().addAll(new VBox(6, title, sub), buildUploadCard(), buildCertsList());
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox buildUploadCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("Upload New Certificate");
        header.getStyleClass().add("section-title");

        // Upload zone
        VBox uploadZone = new VBox(10);
        uploadZone.setAlignment(Pos.CENTER);
        uploadZone.setStyle("-fx-border-color: #334155; -fx-border-style: dashed; -fx-border-radius: 10; " +
            "-fx-background-radius: 10; -fx-padding: 32; -fx-background-color: rgba(15,175,122,0.04);");

        FontIcon uploadIcon = FontIcon.of(Feather.UPLOAD_CLOUD, 36);
        uploadIcon.setStyle("-fx-icon-color: #0FAF7A;");

        Label uploadText = new Label("Drag & drop your certificate file here");
        uploadText.getStyleClass().add("label-secondary");

        Label uploadSub = new Label("PDF, JPG, PNG supported â€” max 10MB");
        uploadSub.getStyleClass().add("label-muted");

        Button browseBtn = new Button("Browse File");
        browseBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");

        uploadZone.getChildren().addAll(uploadIcon, uploadText, uploadSub, browseBtn);

        HBox formRow = new HBox(16,
            formField("Certificate Name", "e.g. AWS Solutions Architect"),
            formField("Issuing Authority", "e.g. Amazon, Google, Adobe")
        );
        HBox.setHgrow(formRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(formRow.getChildren().get(1), Priority.ALWAYS);

        HBox dateRow = new HBox(16,
            formField("Issue Date", "YYYY-MM-DD"),
            formField("Expiry Date (optional)", "YYYY-MM-DD")
        );
        HBox.setHgrow(dateRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(dateRow.getChildren().get(1), Priority.ALWAYS);

        Button submitBtn = new Button("Submit for Validation");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");

        card.getChildren().addAll(header, uploadZone, formRow, dateRow, submitBtn);
        return card;
    }

    private VBox buildCertsList() {
        List<Certificate> certs = data.getCertificates().stream()
            .filter(c -> c.getWorkerName().equals("Bob Chen")).toList();

        Label header = new Label("My Certificates");
        header.getStyleClass().add("section-title");

        VBox list = new VBox(12);
        for (Certificate cert : certs) {
            list.getChildren().add(buildCertCard(cert));
        }

        VBox section = new VBox(14, header, list);
        return section;
    }

    private VBox buildCertCard(Certificate cert) {
        Label name    = new Label(cert.getName()); name.getStyleClass().add("card-title");
        Label issuer  = new Label("Issued by " + cert.getIssuer()); issuer.getStyleClass().add("label-secondary");
        Label date    = new Label("Issued: " + cert.getIssuedDate()); date.getStyleClass().add("label-muted");
        BadgeLabel badge = BadgeLabel.forStatus(cert.getStatus().name());

        // AI Analysis block
        VBox aiBlock = new VBox(6);
        aiBlock.getStyleClass().add("ai-block");
        Label aiTitle = new Label("AI Analysis");
        aiTitle.getStyleClass().add("ai-block-title");
        Label aiText = new Label(cert.getAiAnalysis());
        aiText.getStyleClass().add("label-muted");
        aiText.setWrapText(true);
        aiBlock.getChildren().addAll(aiTitle, aiText);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(16, new VBox(4, name, issuer, date), spacer, badge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, topRow, aiBlock);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox formField(String label, String prompt) {
        Label lbl = new Label(label); lbl.getStyleClass().add("form-label");
        TextField tf = new TextField(); tf.setPromptText(prompt); tf.getStyleClass().add("text-field"); tf.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, lbl, tf);
    }
}

