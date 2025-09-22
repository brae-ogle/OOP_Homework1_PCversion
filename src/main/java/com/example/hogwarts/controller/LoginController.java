package com.example.hogwarts.controller;

import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.User;
import com.example.hogwarts.view.DashboardView;
import com.example.hogwarts.view.LoginView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    private final LoginView loginView;

    public LoginController(LoginView loginView) {
        this.loginView = loginView;
        this.loginView.setController(this);
    }

    public void handleLogin(String username, String password) {
        User user = DataStore.getInstance().authenticate(username, password);
        if (user != null) { // User authenticated successfully
            // Save the authenticated user
            DataStore.getInstance().setCurrentUser(user);
            DashboardView dashboardView = new DashboardView(); // Create the dashboard view ONLY after successful login
            DashboardController dashboardController = new DashboardController(dashboardView, this.loginView); // Create the controller for the dashboard view

            Scene scene = this.loginView.getScene();
            if (scene != null) {
                scene.setRoot(dashboardView); // Replace the login view with the dashboard view
//                Stage stage = (Stage) scene.getWindow();
//                stage.setWidth(800);
//                stage.setHeight(600);
            }
        } else { // Authentication failed
            this.loginView.getMessageLabel().setText("Invalid username or password.");
        }
    }
}
