package org.example.annotation.Service;

import org.example.annotation.Entity.Dataset;
import org.example.annotation.Entity.Task;
import org.example.annotation.Repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Transactional
@Service
public class DatasetService {
    @Autowired
    private DatasetRepository datasetRepository;

   // Create Database
    public Dataset createDataset(String name, String classes, String description) {
        Dataset dataset = new Dataset();
        dataset.setName(name);
        dataset.setClasses(classes);
        dataset.setDescription(description);
        return datasetRepository.save(dataset);
    }
    //Find Dataset by ID
    public Dataset getById(Long id) {
        return datasetRepository.findById(id).orElse(null);
    }
    //Find All Datasets
    public List<Dataset> findAll() {
        return datasetRepository.findAll();
    }
    //Count progress
    public int calculerAvancement(Dataset dataset) {
        List<Task> tasks = dataset.getTasks();
        if (tasks.isEmpty()) return 0;
        long annotées = tasks.stream().filter(t -> t.getAnnotation() != null).count();
        return (int) ((annotées * 100.0) / tasks.size());
    }
    public void save(Dataset created) {
        datasetRepository.save(created);
    }
}

