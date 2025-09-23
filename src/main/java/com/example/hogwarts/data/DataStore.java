package com.example.hogwarts.data;

import com.example.hogwarts.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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

    private DataStore() {
        // Hardcoded users
        this.users.add(new User("admin", "123", Role.ADMIN));
        this.users.add(new User("user", "123", Role.USER));

        // Sample data
        Wizard w1 = new Wizard("Harry Potter");
        Wizard w2 = new Wizard("Hermione Granger");
        this.addWizard(w1);
        this.addWizard(w2);

        Artifact a1 = new Artifact("Invisibility Cloak", "A magical cloak that makes the wearer invisible.");
        Artifact a2 = new Artifact("Time-Turner", "A device used for time travel.");
        this.addArtifact(a1);
        this.addArtifact(a2);

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
        History history = new History(artifact.getId(), artifact.getName(), "--", new Date());
        this.addHistoryEntry(artifact.getId(), history);
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
}
