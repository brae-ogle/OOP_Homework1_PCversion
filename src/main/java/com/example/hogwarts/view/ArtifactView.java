package com.example.hogwarts.view;

import com.example.hogwarts.controller.ArtifactController;
import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.History;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.transformation.FilteredList;

public class ArtifactView extends VBox{
    private final ArtifactController controller;
    private final TableView<Artifact> artifactTable;
    private final ObservableList<Artifact> artifactData;
    FilteredList<Artifact> filteredData;
    TextField searchBar;

    public ArtifactView() {
        this.controller = new ArtifactController();
        this.artifactTable = new TableView<>();
        this.artifactData = FXCollections.observableArrayList(controller.findAllArtifacts());
        this.filteredData = new FilteredList<>(artifactData, p -> true); //Allow filtering (via search bar)
        SortedList<Artifact> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(artifactTable.comparatorProperty()); //Allows sorting by clicking column headers
        artifactTable.setItems(sortedData);
        this.searchBar = createSearchBar(filteredData);

        setSpacing(10);
        setPadding(new Insets(10));
        getChildren().addAll(searchBar, createTable(), createButtons());
    }

    private TableView<Artifact> createTable() {
        TableColumn<Artifact, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));

        TableColumn<Artifact, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Artifact, Number> conditionCol = new TableColumn<>("Condition");
        conditionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper(cell.getValue().getCondition()));

        TableColumn<Artifact, String> ownerCol = createOwnerColumn();

        TableColumn<Artifact, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button unassignButton = new Button("Unassign");
            private final Button historyButton = new Button("History");
            private final Button repairButton = new Button("Repair");
            private final HBox buttons = new HBox(5);

            {
                viewButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showViewArtifactDialog(artifact);
                });

                editButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showEditArtifactDialog(artifact);
                });

                deleteButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete Artifact");
                    confirm.setContentText("Are you sure you want to delete \"" + artifact.getName() + "\"?");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            controller.deleteArtifact(artifact.getId());
                            artifactData.setAll(controller.findAllArtifacts());
                        }
                    });
                });
                unassignButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    //Confirmation unassignment
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Unassignment");
                    confirm.setHeaderText("Unassign Artifact Owner");
                    confirm.setContentText("Are you sure you want to unassign the owner of \"" + artifact.getName() + "\"?");
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) { //Proceed only if confirmed
                            if (artifact.getOwner() != null) {
                                controller.unassignArtifactOwner(artifact.getId());
                                refreshArtifactView();
                            }
                        }
                    });
                });

                historyButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showArtifactHistoryDialog(artifact);
                });

                repairButton.setOnAction(e -> {
                    Artifact artifact = getTableView().getItems().get(getIndex());
                    showArtifactRepairDialog(artifact);
                });
            }


            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= artifactData.size()) {
                    setGraphic(null);
                } else {
                    buttons.getChildren().clear();
                    //buttons.getChildren().add(viewButton);
                    buttons.getChildren().addAll(viewButton, unassignButton, historyButton, repairButton);
                    if (DataStore.getInstance().getCurrentUser().isAdmin()) {
                        buttons.getChildren().addAll(editButton, deleteButton);
                    }
                    setGraphic(buttons);
                }
            }
        });

        artifactTable.getColumns().setAll(idCol, nameCol, ownerCol, conditionCol, actionCol);
        //No longer needed as we use filtered list
        //artifactTable.setItems(artifactData);
        artifactTable.setPrefHeight(300);
        return artifactTable;
    }

    private TextField createSearchBar(FilteredList<Artifact> filteredData) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name...");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            filteredData.setPredicate(artifact -> {
                if (filter.isEmpty()) return true;
                return artifact.getName().toLowerCase().contains(filter);
            });
        });

        return searchField;
    }


    private HBox createButtons() {
        Button addBtn = new Button("Add");
        HBox box = new HBox(10);
        if (DataStore.getInstance().getCurrentUser().isAdmin()) {
            addBtn.setOnAction(e -> showAddArtifactDialog());
            box.getChildren().add(addBtn);
        }
        return box;
    }

    private TableColumn<Artifact, String> createOwnerColumn() {
        TableColumn<Artifact, String> ownerCol = new TableColumn<>("Owner");

        // Set cell value (handle null owners)
        ownerCol.setCellValueFactory(cell -> {
            String ownerName = cell.getValue().getOwner() != null ? cell.getValue().getOwner().getName() : "";
            return new ReadOnlyStringWrapper(ownerName);
        });
        ownerCol.setSortable(true);
        ownerCol.setComparator((o1, o2) -> {
            boolean o1IsDummy = o1 == null || o1.equals("--");
            boolean o2IsDummy = o2 == null || o2.equals("--");
            if (o1IsDummy && o2IsDummy) return 0;
            if (o1IsDummy) return 1; // put o1 after o2
            if (o2IsDummy) return -1; // put o2 after o1
            return o1.compareToIgnoreCase(o2);
        });
        return ownerCol;
    }

    private void showAddArtifactDialog() {
        Dialog<Artifact> dialog = new Dialog<>();
        dialog.setTitle("Add Artifact");
        dialog.setHeaderText("Enter artifact details:");

        TextField nameField = new TextField();
        TextArea descField = new TextArea();

        VBox content = new VBox(10, new Label("Name:"), nameField, new Label("Description:"), descField);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return controller.addArtifact(nameField.getText(), descField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(artifact -> {
            artifactData.setAll(controller.findAllArtifacts());
            artifactTable.getSelectionModel().select(artifact);
        });
        refreshArtifactView();
    }

    private void showEditArtifactDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Artifact");
        dialog.setHeaderText("Edit artifact details:");

        TextField nameField = new TextField(artifact.getName());
        TextArea descField = new TextArea(artifact.getDescription());

        VBox content = new VBox(10, new Label("Name:"), nameField, new Label("Description:"), descField);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                controller.updateArtifact(artifact.getId(), nameField.getText(), descField.getText());
                artifactData.setAll(controller.findAllArtifacts());
            }
            return null;
        });
        refreshArtifactView();
        dialog.showAndWait();
    }

    private void showViewArtifactDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Artifact Details");
        dialog.setHeaderText("Viewing: " + artifact.getName());

        String ownerName = artifact.getOwner() != null ? artifact.getOwner().getName() : "Unassigned";
        TextArea details = new TextArea(
                "ID: " + artifact.getId() + "\n" +
                        "Name: " + artifact.getName() + "\n" +
                        "Description: " + artifact.getDescription() + "\n" +
                        "Owner: " + ownerName
        );
        details.setEditable(false);
        details.setWrapText(true);

        VBox content = new VBox(details);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        refreshArtifactView();
        dialog.showAndWait();
    }

    private void showArtifactHistoryDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Artifact Assignment History");
        dialog.setHeaderText("History for: " + artifact.getName());

        // Pull *all* history entries for this artifact
        ObservableList<History> historyList = FXCollections.observableArrayList(
                DataStore.getInstance().getHistoryByArtifactId(artifact.getId())
        );

        TableView<History> historyTable = new TableView<>(historyList);

//        // Wizard column (resolve wizard name from DataStore if available)
//        TableColumn<History, String> wizardCol = new TableColumn<>("Wizard");
//        wizardCol.setCellValueFactory(c -> {
//            int wizardId = c.getValue().getWizardId();
//            String name = "";
//            if (wizardId != 0) {
//                var w = DataStore.getInstance().findWizardById(wizardId);
//                if (w != null) name = w.getName();
//            }
//            return new ReadOnlyStringWrapper(name);
//        });
        TableColumn<History, String> wizardCol = new TableColumn<>("Wizard");
        wizardCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getWizardName())
        );

//        // Action column (ASSIGNED / UNASSIGNED)
//        TableColumn<History, String> actionCol = new TableColumn<>("Action");
//        actionCol.setCellValueFactory(c ->
//                new ReadOnlyStringWrapper(c.getValue().getActionType())
//        );

        // Timestamp column
        TableColumn<History, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getTimestamp().toString())
        );

        //historyTable.getColumns().addAll(wizardCol, actionCol, timeCol);
        historyTable.getColumns().addAll(wizardCol, timeCol);
        historyTable.setPrefHeight(300);

        VBox content = new VBox(historyTable);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showArtifactRepairDialog(Artifact artifact) {
        if (artifact == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Repair Artifact");
        dialog.setHeaderText("Repairing: " + artifact.getName());

        // Current condition display
        Label conditionLabel = new Label("Current Condition: " + artifact.getCondition() + " (0-100)");

        // Warning if condition < 10
        Label warningLabel = new Label();
        if (artifact.getCondition() < 10) {
            warningLabel.setText("⚠ Warning: Artifact condition is critically low!");
            warningLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }

        // New condition input
        TextField newConditionField = new TextField();
        newConditionField.setPromptText("Enter new condition (0-100)");

        // Restrict to integers only
        newConditionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                newConditionField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        VBox content = new VBox(10,
                conditionLabel,
                warningLabel,
                new Label("New Condition:"), newConditionField
        );
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String newConditionText = newConditionField.getText();
                if (!newConditionText.isEmpty()) {
                    int newCondition = Integer.parseInt(newConditionText);
                    if (newCondition < 0 || newCondition > 100) {
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "Condition must be between 0 and 100.",
                                ButtonType.OK);
                        alert.showAndWait();
                    } else {
                        controller.repairArtifactTo(artifact.getId(), newCondition - artifact.getCondition());

                    }
                }
            }
            return null;
        });
        dialog.showAndWait();
        refreshArtifactView();
    }



    public void refreshArtifactView() {
        artifactTable.refresh();
    }
}