package com.example.hogwarts.view;

import com.example.hogwarts.controller.LoginController;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView extends VBox{
    private LoginController controller;

    private Label userLabel;
    private TextField userField;
    private Label passLabel;
    private PasswordField passField;
    private Button loginButton;
    private Label messageLabel;

    public LoginView() {
        this.userLabel = new Label("Username:");
        this.userField = new TextField();
        this.passLabel = new Label("Password:");
        this.passField = new PasswordField();
        this.loginButton = new Button("Login");
        this.messageLabel = new Label();
        this.loginButton.setOnAction(e ->
                controller.handleLogin(this.userField.getText(), this.passField.getText())
        );

        setSpacing(10);
        setPrefWidth(300);
        setPadding(new Insets(20));
        getChildren().addAll(this.userLabel, this.userField, this.passLabel, this.passField, this.loginButton, this.messageLabel);
    }

    public void setController(LoginController loginController) {
        this.controller = loginController;
    }

    public TextField getUserField() {
        return userField;
    }

    public PasswordField getPassField() {
        return passField;
    }

    public Label getMessageLabel() {
        return messageLabel;
    }
}
