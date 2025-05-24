package org.example.annotation.Repository;

import org.example.annotation.Entity.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.example.annotation.Entity.TextPair;

import java.util.List;

@Repository
public interface TextPairRepository extends JpaRepository<TextPair, Long> {
    List<TextPair> findByDataset(Dataset dataset);
    List<TextPair> findByDatasetId(Long Id);
    Page<TextPair> findByDataset(Dataset dataset, Pageable pageable);
    @Query("SELECT COUNT(tp) FROM TextPair tp WHERE tp.dataset = :dataset")
    long countByDataset(@Param("dataset") Dataset dataset);
}
