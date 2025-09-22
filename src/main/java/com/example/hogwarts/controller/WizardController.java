package com.example.hogwarts.controller;

import com.example.hogwarts.data.DataStore;
import com.example.hogwarts.model.Artifact;
import com.example.hogwarts.model.Wizard;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WizardController {
    private final DataStore store = DataStore.getInstance();

    public Collection<Wizard> findAllWizards() {
        return this.store.findAllWizards();
    }

    public Wizard addWizard(String name) {
        Wizard wizard = new Wizard(name);
        return this.store.addWizard(wizard);
    }

    public void updateWizard(int id, String newName) {
        Wizard wizard = this.store.findWizardById(id);
        if(wizard == null) {
            throw new IllegalArgumentException("Wizard with ID " + id + " not found.");
        }
        wizard.setName(newName);
    }

    public void deleteWizard(int id) {
        this.store.deleteWizardById(id);
    }

    public boolean assignArtifactToWizard(Wizard wizard, Artifact artifact) {
        return this.store.assignArtifactToWizard(artifact.getId(), wizard.getId());
    }

    public List<Artifact> getUnassignedArtifacts() {
        return this.store.findAllArtifacts().stream()
                .filter(a -> a.getOwner() == null)
                .collect(Collectors.toList());
    }
}
