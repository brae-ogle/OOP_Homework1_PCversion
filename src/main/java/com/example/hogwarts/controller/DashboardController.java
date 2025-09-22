package com.example.hogwarts.controller;

import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.view.DashboardView;
import com.example.hogwarts.view.LoginView;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

public class DashboardController {
    private final DashboardView dashboardView;
    private final LoginView loginView;

    public DashboardController(DashboardView dashboardView, LoginView loginView) {
        this.dashboardView = dashboardView;
        this.loginView = loginView;
        this.dashboardView.setController(this);
    }

    public void handleLogout() {
        StackPane rootPane = (StackPane) this.dashboardView.getParent();

        // Clear the current user session
        DataStore.getInstance().setCurrentUser(null);
        // Clear the form fields in the login view
        this.loginView.getUserField().clear();
        this.loginView.getPassField().clear();

        Scene scene = this.dashboardView.getScene();
        if (scene != null) {
            scene.setRoot(this.loginView); // Replace the dashboard view with login view
        }
        this.loginView.getMessageLabel().setText("You have been logged out.");
    }
}
