package org.example.annotation.Repository;

import org.example.annotation.Entity.Dataset;
import org.example.annotation.Entity.TextPair;
import org.example.annotation.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.example.annotation.Entity.Task;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDataset(Dataset dataset);
    List<Task> findByAnnotatorIdAndDatasetId(Long annotatorId, Long datasetId);
    List<Task> findByAnnotatorId(Long annotatorId);
    List<Task> findByDatasetId(Long datasetId);
    @Query("SELECT t FROM Task t WHERE t.annotator.id = :userId ORDER BY t.id")
    List<Task> findByAnnotatorIdOrderById(@Param("userId") Long userId);
    int countByAnnotatorIdAndAnnotationIsNotNull(Long annotatorId);
    List<Task> findByAnnotationIsNotNull();
    Page<Task> findAll(Pageable pageable);
    Page<Task> findByAnnotatorId(Long annotatorId, Pageable pageable);
    Page<Task> findByDatasetId(Long datasetId, Pageable pageable);
    Page<Task> findByAnnotatorAndDataset(Long annotatorId, Long datasetId, Pageable pageable);


    List<Task> findByAnnotatorIdAndAnnotationIsNotNull(Long annotatorId);

    Task findByTextPairAndAnnotator(TextPair pair, User annotator);

    List<Task> findByAnnotatorIdAndDataset(Long annotatorId, Dataset dataset);

    List<Task> findByDatasetIdAndAnnotationDateIsNotNull(Long datasetId);
}