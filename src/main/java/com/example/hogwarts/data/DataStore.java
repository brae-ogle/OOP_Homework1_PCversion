package com.example.hogwarts.data;

import com.example.hogwarts.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Thread-safe Singleton DataStore
 * Uses atomic integers for ID generation
 */
public class DataStore {
    // Volatile ensures visibility across threads
    private static volatile DataStore instance;

    private final List<User> users = new CopyOnWriteArrayList<>();
    private final Map<Integer, Wizard> wizards = new ConcurrentHashMap<>();
    private final Map<Integer, Artifact> artifacts = new ConcurrentHashMap<>();
    private final Map<Integer, List<History>> assignmentLogs = new ConcurrentHashMap<>();

    private final AtomicInteger wizardIdCounter = new AtomicInteger(1);
    private final AtomicInteger artifactIdCounter = new AtomicInteger(1);

    private volatile User currentUser; // Guarded with volatile for visibility

    // File paths for persistence
    private static final String DATA_DIR = "data";
    private static final String WIZARDS_FILE = DATA_DIR + "/wizards.json";
    private static final String ARTIFACTS_FILE = DATA_DIR + "/artifacts.json";
    private static final String TRANSFERS_FILE = DATA_DIR + "/transfers.json";

//    private DataStore() {
//        // Hardcoded users
//        this.users.add(new User("admin", "123", Role.ADMIN));
//        this.users.add(new User("user", "123", Role.USER));
//
//        // Sample data
//        Wizard w1 = new Wizard("Harry Potter");
//        Wizard w2 = new Wizard("Hermione Granger");
//        this.addWizard(w1);
//        this.addWizard(w2);
//
//        Artifact a1 = new Artifact("Invisibility Cloak", "A magical cloak that makes the wearer invisible.");
//        Artifact a2 = new Artifact("Time-Turner", "A device used for time travel.");
//        this.addArtifact(a1);
//        this.addArtifact(a2);
//
//        this.assignArtifactToWizard(a1.getId(), w1.getId());
//        this.assignArtifactToWizard(a2.getId(), w2.getId());
//    }
private DataStore() {
    this.users.add(new User("admin", "123", Role.ADMIN));
    this.users.add(new User("user", "123", Role.USER));
    File dir = new File(DATA_DIR);
    if (!dir.exists()) dir.mkdirs();

    // Try loading from JSON; if none exists, seed defaults
    try {
        if (new File(WIZARDS_FILE).exists() && new File(ARTIFACTS_FILE).exists()) {
            loadWizards();
            loadArtifacts();
            loadTransfers();
        } else {
            //System.out.println("Here 1");
            seedDefaults();
        }
    } catch (IOException e) {
        e.printStackTrace();
        //System.out.println("Here 2");
        //seedDefaults(); // fallback if something breaks

    }
}

    private void seedDefaults() {
//        this.users.add(new User("admin", "123", Role.ADMIN));
//        this.users.add(new User("user", "123", Role.USER));

        Wizard w1 = this.addWizard(new Wizard("Harry Potter"));
        Wizard w2 = this.addWizard(new Wizard("Hermione Granger"));

        Artifact a1 = this.addArtifact(new Artifact("Invisibility Cloak", "A magical cloak that makes the wearer invisible."));
        Artifact a2 = this.addArtifact(new Artifact("Time-Turner", "A device used for time travel."));

        this.assignArtifactToWizard(a1.getId(), w1.getId());
        this.assignArtifactToWizard(a2.getId(), w2.getId());
    }

    // Double-checked locking for singleton
    public static DataStore getInstance() {
        if (instance == null) {
            synchronized (DataStore.class) {
                if (instance == null) {
                    instance = new DataStore();
                }
            }
        }
        return instance;
    }

    // User authentication
    public User authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    // Wizards
    public Wizard addWizard(Wizard wizard) {
        wizard.setId(wizardIdCounter.getAndIncrement());
        this.wizards.put(wizard.getId(), wizard);
        return wizard;
    }

    public void deleteWizardById(int id) {
        Wizard wizard = this.wizards.remove(id);
        if (wizard != null) {
            wizard.removeAllArtifacts();
        }
    }

    public Collection<Wizard> findAllWizards() {
        return this.wizards.values();
    }

    public Wizard findWizardById(int id) {
        return this.wizards.get(id);
    }

    // Artifacts
    public Artifact addArtifact(Artifact artifact) {
        artifact.setId(artifactIdCounter.getAndIncrement());
        this.artifacts.put(artifact.getId(), artifact);
        return artifact;
    }

    public void deleteArtifactById(int id) {
        Artifact artifact = this.artifacts.remove(id);
        if (artifact != null && artifact.getOwner() != null) {
            artifact.getOwner().removeArtifact(artifact);
        }
    }

    public Collection<Artifact> findAllArtifacts() {
        return this.artifacts.values();
    }

    public Artifact findArtifactById(int id) {
        return this.artifacts.get(id);
    }

    public boolean assignArtifactToWizard(int artifactId, int wizardId) {
        Artifact artifact = this.artifacts.get(artifactId);
        Wizard wizard = this.wizards.get(wizardId);
        if (artifact == null || wizard == null) return false;

        wizard.addArtifact(artifact);

        // Log the assignment
        History history = new History(artifact.getId(), artifact.getName(), wizard.getName(), new Date());
        this.addHistoryEntry(artifactId, history);
        return true;
    }

    // Unassign artifact from its owner wizard
    public boolean unassignArtifactFromWizard(int artifactId) {
        Artifact artifact = this.artifacts.get(artifactId);
        if (artifact == null || artifact.getOwner() == null) return false;
        Wizard owner = artifact.getOwner();
        owner.removeArtifact(artifact);
        History history = new History(artifact.getId(), artifact.getName(), "--", new Date());
        this.addHistoryEntry(artifact.getId(), history);
        return true;
    }

    // Current user
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // History
    public void addHistoryEntry(int artifactID, History history) {
        this.assignmentLogs.computeIfAbsent(artifactID, k -> new CopyOnWriteArrayList<>()).add(history);
    }

    public List<History> getHistoryByArtifactId(int artifactId) {
        return assignmentLogs.getOrDefault(artifactId, new ArrayList<>());
    }

    //----------------------------------------------------------------------
    // Persistence (JSON)
    //----------------------------------------------------------------------
    /* ------------------ Persistence Methods ------------------ */

    // Call this on application exit
    public void saveAll() {
        try {
            saveWizards();
            saveArtifacts();
            saveTransfers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveWizards() throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator gen = factory.createGenerator(new FileOutputStream(WIZARDS_FILE))) {
            gen.writeStartArray();
            for (Wizard w : wizards.values()) {
                gen.writeStartObject();
                gen.writeNumberField("id", w.getId());
                gen.writeStringField("name", w.getName());
                gen.writeEndObject();
            }
            gen.writeEndArray();
        }
    }

    private void saveArtifacts() throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator gen = factory.createGenerator(new FileOutputStream(ARTIFACTS_FILE))) {
            gen.writeStartArray();
            for (Artifact a : artifacts.values()) {
                gen.writeStartObject();
                gen.writeNumberField("id", a.getId());
                gen.writeStringField("name", a.getName());
                gen.writeStringField("description", a.getDescription());
                gen.writeNumberField("condition", a.getCondition());
                if (a.getOwner() != null) {
                    gen.writeNumberField("ownerId", a.getOwner().getId());
                }
                gen.writeEndObject();
            }
            gen.writeEndArray();
        }
    }

    private void saveTransfers() throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator gen = factory.createGenerator(new FileOutputStream(TRANSFERS_FILE))) {
            gen.writeStartArray();
            for (List<History> historyList : assignmentLogs.values()) {
                for (History h : historyList) {
                    gen.writeStartObject();
                    gen.writeNumberField("artifactId", h.getArtifactId());
                    gen.writeStringField("artifactName", h.getArtifactName());
                    gen.writeStringField("wizardName", h.getWizardName());
                    gen.writeStringField("timestamp", h.getTimestamp().toString());
                    gen.writeEndObject();
                }
            }
            gen.writeEndArray();
        }
    }

    private void loadWizards() throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(new FileInputStream(WIZARDS_FILE))) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    int id = 0;
                    String name = null;
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        String field = parser.getCurrentName();
                        parser.nextToken();
                        switch (field) {
                            case "id": id = parser.getIntValue(); break;
                            case "name": name = parser.getText(); break;
                        }
                    }
                    Wizard w = new Wizard(name);
                    w.setId(id);
                    this.wizards.put(id, w);
                    wizardIdCounter.set(Math.max(wizardIdCounter.get(), id + 1));
                }
            }
        }
    }

    private void loadArtifacts() throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(new FileInputStream(ARTIFACTS_FILE))) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    int id = 0, ownerId = -1, condition = 100;
                    String name = null, description = null;
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        String field = parser.getCurrentName();
                        parser.nextToken();
                        switch (field) {
                            case "id": id = parser.getIntValue(); break;
                            case "name": name = parser.getText(); break;
                            case "description": description = parser.getText(); break;
                            case "condition": condition = parser.getIntValue(); break;
                            case "ownerId": ownerId = parser.getIntValue(); break;
                        }
                    }
                    Artifact a = new Artifact(name, description);
                    a.setId(id);
                    a.setCondition(condition);
                    this.artifacts.put(id, a);
                    artifactIdCounter.set(Math.max(artifactIdCounter.get(), id + 1));

                    if (ownerId != -1 && wizards.containsKey(ownerId)) {
                        wizards.get(ownerId).addArtifact(a);
                    }
                }
            }
        }
    }

    private void loadTransfers() throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(new FileInputStream(TRANSFERS_FILE))) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    int artifactId = 0;
                    String artifactName = null, wizardName = null;
                    long timestamp = 0;
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        String field = parser.getCurrentName();
                        parser.nextToken();
                        switch (field) {
                            case "artifactId": artifactId = parser.getIntValue(); break;
                            case "artifactName": artifactName = parser.getText(); break;
                            case "wizardName": wizardName = parser.getText(); break;
                            case "timestamp": timestamp = parser.getLongValue(); break;
                        }
                    }
                    History h = new History(artifactId, artifactName, wizardName, new Date(timestamp));
                    this.addHistoryEntry(artifactId, h);
                }
            }
        }
    }


}
