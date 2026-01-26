package com.jabulile.booking.httpapi;

public class Resource {
    private int id;
    private String name;
    private String type;
    private int capacity;

    public Resource(int id, String name, String type, int capacity) {
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
