package org.example.annotation.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.annotation.Entity.Dataset;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {
}
