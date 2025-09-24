package com.example.hogwarts.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Wizard {
    private int id;
    private String name;
    private final List<Artifact> artifacts = new ArrayList<>();

    public Wizard(String name) {
        this.name = Objects.requireNonNull(name, "name"); // name must not be null
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<Artifact> getArtifacts() {
        return Collections.unmodifiableList(artifacts); // good defensive programming: prevent external modification
    }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = Objects.requireNonNull(name, "name"); }

    public void addArtifact(Artifact artifact) {
        Objects.requireNonNull(artifact, "artifact"); // artifact must not be null

        if (this.artifacts.contains(artifact)) return; // already in the collection

        Wizard currentOwner = artifact.getOwner();

        if (currentOwner != null){
            currentOwner.removeArtifact(artifact); // detach from previous owner
        }

        // now attach to this owner
        artifacts.add(artifact);
        artifact.setOwner(this); // keep back-reference in sync
    }

    public boolean removeArtifact(Artifact artifact) {
        boolean removed = artifacts.remove(artifact);
        if (removed) {
            artifact.setOwner(null);
        }
        return removed;
    }

    public boolean removeAllArtifacts() {
        if (artifacts.isEmpty()) return false;

        for (Artifact a : artifacts) {
            a.setOwner(null);   // package-private
        }
        artifacts.clear();
        return true;
    }

    @Override
    public String toString() {
        return name + " (ID: " + id + ")";
    }
}
