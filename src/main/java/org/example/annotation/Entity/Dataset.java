package org.example.annotation.Entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "datasets")
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    private String classes;

    private String filePath;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TextPair> textPairs;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    public Dataset() {}

    public Dataset(Long id, String name, String description, String classes, String filePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.classes = classes;
        this.filePath = filePath;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<TextPair> getTextPairs() {
        return textPairs;
    }

    public void setTextPairs(List<TextPair> textPairs) {
        this.textPairs = textPairs;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }


}
