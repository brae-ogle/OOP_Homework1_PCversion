package com.example.hogwarts.model;
import java.util.Date;

public class History {
    private final int artifactId;
    private final String artifactName;
    private final String wizardName;
    private final Date timestamp;

    public History(int artifactId, String artifactName, String wizardName, Date timestamp) {
        this.artifactId = artifactId;
        this.artifactName = artifactName;
        this.wizardName = wizardName;
        this.timestamp = timestamp;
    }

    public int getArtifactId() {
        return artifactId;
    }
    public String getArtifactName() {
        return artifactName;
    }

    public String getWizardName() {
        return wizardName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Artifact '%s' assigned to Wizard '%s' at %s", artifactName, wizardName, timestamp);
    }
}
