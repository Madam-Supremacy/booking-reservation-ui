package jabulile.persistence;

public class ResourceEntity {
    private int id;
    private String name;
    private String type;
    private int capacity;
    private String location;
    
    public ResourceEntity() {
    }
    
    public ResourceEntity(String name, String type, int capacity, String location) {
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.location = location;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    @Override
    public String toString() {
        return "ResourceEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", capacity=" + capacity +
                ", location='" + location + '\'' +
                '}';
    }
}