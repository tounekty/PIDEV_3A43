package com.mindcare.legacy.client;

import com.mindcare.view.client.*;

import com.mindcare.components.MainLayout;
import com.mindcare.model.Conversation;
import com.mindcare.model.Message;
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
 * MessagingLegacyContent â€“ chat interface for clients.
 * Left panel: conversation list. Right panel: chat messages.
 */
public class MessagingLegacyContent implements NavigationManager.Buildable {

    private final MockDataService data = MockDataService.getInstance();

    @Override
    public Node build() {
        return buildContent();
    }

    private Node buildContent() {
        SplitPane root = new SplitPane();
        root.setStyle("-fx-background-color: #0A0E1A;");

        VBox convoPanel = buildConversationPanel();
        convoPanel.setMinWidth(240);
        convoPanel.setPrefWidth(300);
        convoPanel.setMaxWidth(420);

        VBox chatPanel = buildChatPanel(1);
        HBox.setHgrow(chatPanel, Priority.ALWAYS);
        VBox.setVgrow(chatPanel, Priority.ALWAYS);

        root.getItems().addAll(convoPanel, chatPanel);
        root.setDividerPositions(0.24);
        return root;
    }

    private VBox buildConversationPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #0F172A; -fx-border-color: #334155; " +
            "-fx-border-width: 0 1 0 0;");

        Label header = new Label("Conversations");
        header.getStyleClass().add("section-title");
        header.setPadding(new Insets(18, 16, 12, 16));

        VBox items = new VBox();
        for (Conversation convo : data.getConversations()) {
            items.getChildren().add(buildConvoItem(convo));
        }

        ScrollPane listScroll = new ScrollPane(items);
        listScroll.setFitToWidth(true);
        listScroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, listScroll);
        return panel;
    }

    private HBox buildConvoItem(Conversation convo) {
        Label avatar = new Label(String.valueOf(convo.getParticipantName().charAt(0)));
        avatar.setStyle("-fx-background-color: rgba(15,175,122,0.2); -fx-background-radius: 20; " +
            "-fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; " +
            "-fx-alignment: center; -fx-text-fill: #0FAF7A; -fx-font-weight: bold;");

        Label nameLabel = new Label(convo.getParticipantName());
        nameLabel.getStyleClass().add("label-primary");
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label lastMsg = new Label(truncate(convo.getLastMessage(), 28));
        lastMsg.getStyleClass().add("label-muted");

        Label timeLabel = new Label(convo.getLastMessageTime());
        timeLabel.getStyleClass().add("label-muted");
        timeLabel.setStyle("-fx-font-size: 10px;");

        VBox nameBox = new VBox(2, nameLabel, lastMsg);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox(timeLabel);
        if (convo.getUnreadCount() > 0) {
            Label dot = new Label(String.valueOf(convo.getUnreadCount()));
            dot.getStyleClass().add("notification-dot");
            rightBox.getChildren().add(dot);
        }

        HBox row = new HBox(10, avatar, nameBox, spacer, rightBox);
        row.getStyleClass().add("convo-item");
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildChatPanel(int convId) {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #0A0E1A;");
        panel.setFillWidth(true);

        // Chat header
        Label chatTitle = new Label("Clara Dubois");
        chatTitle.getStyleClass().add("section-title");
        Label chatSub = new Label("Mobile App UI Design contract");
        chatSub.getStyleClass().add("label-muted");

        VBox chatHeader = new VBox(2, chatTitle, chatSub);
        chatHeader.setStyle("-fx-background-color: #0F172A; -fx-border-color: transparent transparent #334155 transparent; " +
            "-fx-border-width: 0 0 1 0;");
        chatHeader.setPadding(new Insets(14, 20, 14, 20));

        // Messages
        ScrollPane msgScroll = buildMessages(convId);
        VBox.setVgrow(msgScroll, Priority.ALWAYS);

        // Input bar
        HBox inputBar = buildInputBar();

        panel.getChildren().addAll(chatHeader, msgScroll, inputBar);
        return panel;
    }

    private ScrollPane buildMessages(int convId) {
        VBox messages = new VBox(12);
        messages.setPadding(new Insets(20));

        for (Message msg : data.getMessages(convId)) {
            messages.getChildren().add(buildMessageBubble(msg));
        }

        ScrollPane scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        scroll.setStyle("-fx-background-color: #0A0E1A;");
        return scroll;
    }

    private HBox buildMessageBubble(Message msg) {
        Label content = new Label(msg.getContent());
        content.setWrapText(true);

        Label time = new Label(msg.getSentAt());
        time.getStyleClass().add("chat-timestamp");

        VBox bubble = new VBox(4, content, time);
        bubble.getStyleClass().add(msg.isSentByMe() ? "chat-bubble-sent" : "chat-bubble-received");

        HBox row = new HBox(bubble);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(msg.isSentByMe() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.maxWidthProperty().bind(row.widthProperty().multiply(0.58));
        content.maxWidthProperty().bind(row.widthProperty().multiply(0.54));
        return row;
    }

    private HBox buildInputBar() {
        TextField input = new TextField();
        input.setPromptText("Type a message...");
        input.getStyleClass().add("chat-input");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button send = new Button();
        send.setGraphic(FontIcon.of(Feather.SEND, 16));
        send.getStyleClass().addAll("btn", "btn-primary");
        send.setStyle("-fx-background-radius: 50%; -fx-min-width: 40; -fx-min-height: 40;");

        HBox bar = new HBox(10, input, send);
        bar.getStyleClass().add("chat-input-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        return bar;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}

