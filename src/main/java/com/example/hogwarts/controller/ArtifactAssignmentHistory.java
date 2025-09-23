package com.example.hogwarts.controller;

import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.History;
import com.example.hogwarts.model.Wizard;

import java.util.Date;
import java.util.List;

public class ArtifactAssignmentHistory {
    private final DataStore store = DataStore.getInstance();

    public void recordAssignment(Artifact artifact, Wizard wizard) {
        String artifactName = artifact.getName();
        String wizardName = wizard.getName();
        Date timestamp = new Date();
        History entry = new History(artifact.getId(), artifactName, wizardName, timestamp);
        store.addHistoryEntry(artifact.getId(), entry);
    }

    public List<History> getHistory(int artifactID) {
        return store.getHistoryByArtifactId(artifactID);
    }

}
