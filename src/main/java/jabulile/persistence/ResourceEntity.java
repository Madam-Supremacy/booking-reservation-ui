package com.jabulile.booking.persistence;

public class ResourceEntity {
    private final int id;
    private final String name;
    private final String type;
    private final int capacity;

    public ResourceEntity(int id, String name, String type, int capacity) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }
}
