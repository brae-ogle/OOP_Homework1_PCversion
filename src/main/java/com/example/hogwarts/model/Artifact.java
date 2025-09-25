package com.example.hogwarts.model;

import java.util.Objects;

public class Artifact {
    private int id;
    private String name;
    private String description;
    private Wizard owner; // can be null
    private int condition; // 0 (worst) to 100 (best)

    public Artifact(String name, String description) {
        this.name = Objects.requireNonNullElse(name, "name must not be null");
        this.description = Objects.requireNonNullElse(description, "description must not be null");
        this.owner = null;
        this.condition = 100; // default condition
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Wizard getOwner() {
        if (owner == null) {
            return new Wizard("--");
        } else {
            return owner;
        }
    }

    public void setId(int id) { this.id = id; }
    public void setName(String name) {
        this.name = Objects.requireNonNullElse(name, "name must not be null");
    }
    public void setDescription(String description) {
        this.description = Objects.requireNonNullElse(description, "description must not be null");
    }
    public void unassignOwner() {
        this.owner = null;
    }
    public int getCondition() { return condition; }
    public void setCondition(int condition) {
        //Used for repairing artifacts
        //Bound condition between 0 and 100
        if (condition < 0) {
            this.condition = 0;
        } else if (condition > 100) {
            this.condition = 100;
        } else {
            this.condition = condition;
        }
    }
    public void reduceConditionByFive() {
        //Used when artifact is assigned to a wizard or unassigned
        int newCondition = this.getCondition() - 5;
        this.setCondition(newCondition);
    }

    void setOwner(Wizard owner) {
        this.owner = owner; } // package-private to restrict access

    @Override
    public String toString() {
        return name + " (ID: " + id + ")";
    }

}