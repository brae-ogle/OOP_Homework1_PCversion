package com.example.hogwarts.view;

import com.example.hogwarts.controller.WizardController;
import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.Wizard;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class WizardView extends VBox{
    private final WizardController controller;
    private final TableView<Wizard> wizardTable;
    private final ObservableList<Wizard> wizardData;

    public WizardView() {
        this.controller = new WizardController();
        this.wizardTable = new TableView<>();
        this.wizardData = FXCollections.observableArrayList(controller.findAllWizards());

        setSpacing(10);
        setPadding(new Insets(10));
        getChildren().addAll(createTable(), createButtons());
    }

    private TableView<Wizard> createTable() {
        TableColumn<Wizard, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));

        TableColumn<Wizard, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Wizard, Void> actionCol = new TableColumn<>("Actions");

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button assignButton = new Button("Assign");
            private final HBox buttons = new HBox(5);

            {
                viewButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showViewWizardDialog(wizard);
                });

                editButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showEditWizardDialog(wizard);
                });

                deleteButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete Wizard");
                    confirm.setContentText("Are you sure you want to delete \"" + wizard.getName() + "\" and unassign their artifacts?");
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            controller.deleteWizard(wizard.getId());
                            wizardData.setAll(controller.findAllWizards());
                        }
                    });
                });

                assignButton.setOnAction(e -> {
                    Wizard wizard = getTableView().getItems().get(getIndex());
                    showAssignArtifactDialogFor(wizard);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= wizardData.size()) {
                    setGraphic(null);
                } else {
                    buttons.getChildren().clear();
                    buttons.getChildren().add(viewButton);
                    if (DataStore.getInstance().getCurrentUser().isAdmin()) {
                        buttons.getChildren().addAll(editButton, deleteButton, assignButton);
                    }
                    setGraphic(buttons);
                }
            }
        });

        wizardTable.getColumns().setAll(idCol, nameCol, actionCol);
        wizardTable.setItems(wizardData);
        wizardTable.setPrefHeight(300);
        return wizardTable;
    }

    private HBox createButtons() {
        Button addBtn = new Button("Add");
        HBox buttonBox = new HBox(10);
        if (DataStore.getInstance().getCurrentUser().isAdmin()) {
            addBtn.setOnAction(e -> showAddWizardDialog());
            buttonBox.getChildren().add(addBtn);
        }
        return buttonBox;
    }

    private void showAddWizardDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Wizard");
        dialog.setHeaderText("Enter wizard name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Wizard wizard = controller.addWizard(name);
                wizardData.setAll(controller.findAllWizards());
                wizardTable.getSelectionModel().select(wizard);
            }
        });
    }

    private void showEditWizardDialog(Wizard wizard) {
        if (wizard == null) return;

        TextInputDialog dialog = new TextInputDialog(wizard.getName());
        dialog.setTitle("Edit Wizard");
        dialog.setHeaderText("Edit wizard name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                controller.updateWizard(wizard.getId(), name);
                wizardData.setAll(controller.findAllWizards());
            }
        });
    }

    private void showAssignArtifactDialogFor(Wizard wizard) {
        var unowned = FXCollections.observableArrayList(controller.getUnassignedArtifacts());

        if (unowned.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No unassigned artifacts available.");
            alert.setHeaderText("Nothing to assign");
            alert.showAndWait();
            return;
        }

        ChoiceDialog<Artifact> dialog = new ChoiceDialog<>(unowned.get(0), unowned);
        dialog.setTitle("Assign Artifact");
        dialog.setHeaderText("Assign to " + wizard.getName());

        dialog.showAndWait().ifPresent(artifact -> {
            if (artifact.getCondition() < 10) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Condition < 10. Repair item first.",
                        ButtonType.OK);
                alert.setHeaderText("Cannot Assign Artifact");
                alert.showAndWait();
            } else {
                controller.assignArtifactToWizard(wizard, artifact);
                wizardData.setAll(controller.findAllWizards());
                wizardTable.getSelectionModel().select(wizard);
            }
        });
    }


    private void showViewWizardDialog(Wizard wizard) {
        if (wizard == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Wizard Details");
        dialog.setHeaderText("Viewing: " + wizard.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(wizard.getId()).append("\n");
        sb.append("Name: ").append(wizard.getName()).append("\n");
        sb.append("Artifacts:\n");
        for (Artifact a : wizard.getArtifacts()) {
            sb.append("  - ").append(a.getName()).append(" (ID: ").append(a.getId()).append(")\n");
        }

        TextArea details = new TextArea(sb.toString());
        details.setEditable(false);
        details.setWrapText(true);

        VBox content = new VBox(details);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}

