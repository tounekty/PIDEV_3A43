package com.mindcare.controller.admin;

import com.mindcare.dao.DataAccessException;
import com.mindcare.dao.UserDAO;
import com.mindcare.model.User;
import com.mindcare.service.MockDataService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Label totalUsersLabel;
    @FXML private Label activeContractsLabel;
    @FXML private Label openTicketsLabel;
    @FXML private Label serviceRequestsLabel;
    @FXML private Label pendingCertsLabel;
    @FXML private Canvas contractsChartCanvas;
    @FXML private Canvas usersChartCanvas;
    @FXML private TableView<User> recentUsersTable;
    @FXML private TableColumn<User, String> recentUserNameColumn;
    @FXML private TableColumn<User, String> recentUserRoleColumn;
    @FXML private TableColumn<User, String> recentUserEmailColumn;

    private final MockDataService mockDataService = MockDataService.getInstance();
    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void initialize() {
        bindStats();
        bindRecentUsers();
        drawContractsChart();
        drawUsersChart();
    }

    private void bindStats() {
        Map<String, Object> stats = mockDataService.getAdminStats();
        totalUsersLabel.setText(Integer.toString(loadUsersSafe().size()));
        activeContractsLabel.setText(String.valueOf(stats.get("activeContracts")));
        openTicketsLabel.setText(String.valueOf(stats.get("openTickets")));
        serviceRequestsLabel.setText(String.valueOf(stats.get("serviceRequests")));
        pendingCertsLabel.setText(String.valueOf(stats.get("pendingCerts")));
    }

    private void bindRecentUsers() {
        recentUserNameColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFullName()));
        recentUserRoleColumn.setCellValueFactory(cd -> new SimpleStringProperty(
            displayRole(cd.getValue().getRole())
        ));
        recentUserEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        recentUsersTable.setItems(FXCollections.observableArrayList(loadUsersSafe().stream().limit(5).toList()));
    }

    private String displayRole(User.Role role) {
        if (role == null) {
            return "ETUDIANT";
        }
        return role == User.Role.CLIENT ? "ETUDIANT" : role.name();
    }

    private void drawContractsChart() {
        GraphicsContext gc = contractsChartCanvas.getGraphicsContext2D();
        int[] vals = {24, 38, 29, 45, 51, 36, 60, 42, 55, 67, 72, 88};
        String[] months = {"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};

        double barW = 24;
        double gap = 12;
        double startX = 20;
        double maxH = 120;
        double maxVal = 100;

        for (int i = 0; i < vals.length; i++) {
            double x = startX + i * (barW + gap);
            double barH = (vals[i] / maxVal) * maxH;
            double y = 140 - barH;

            gc.setFill(Color.web("#0D6EFD", 0.7));
            gc.fillRoundRect(x, y, barW, barH, 4, 4);

            gc.setFill(Color.web("#7A8CA8"));
            gc.fillText(months[i], x + 7, 155);
        }
    }

    private void drawUsersChart() {
        GraphicsContext gc = usersChartCanvas.getGraphicsContext2D();
        int[] vals = {10, 18, 25, 32, 28, 45, 52, 60, 55, 70, 78, 88};
        double startX = 20;
        double stepX = 33;
        double maxH = 120;
        double maxVal = 90;

        gc.setStroke(Color.web("#D5E1F4", 0.4));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= 4; i++) {
            double y = 140 - (i * maxH / 4);
            gc.strokeLine(startX, y, startX + 11 * stepX, y);
        }

        gc.setStroke(Color.web("#0D6EFD"));
        gc.setLineWidth(2);
        double[] px = new double[vals.length];
        double[] py = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            px[i] = startX + i * stepX;
            py[i] = 140 - (vals[i] / maxVal) * maxH;
        }
        for (int i = 0; i < vals.length - 1; i++) {
            gc.strokeLine(px[i], py[i], px[i + 1], py[i + 1]);
        }

        gc.setFill(Color.web("#0D6EFD"));
        for (int i = 0; i < vals.length; i++) {
            gc.fillOval(px[i] - 4, py[i] - 4, 8, 8);
        }
    }

    private List<User> loadUsersSafe() {
        try {
            return userDAO.getAllUsers();
        } catch (DataAccessException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Unable to load dashboard users");
            String details = exception.getCause() != null ? exception.getCause().getMessage() : "";
            alert.setContentText(details.isBlank() ? "User data is temporarily unavailable." : details);
            alert.showAndWait();
            return List.of();
        }
    }
}

