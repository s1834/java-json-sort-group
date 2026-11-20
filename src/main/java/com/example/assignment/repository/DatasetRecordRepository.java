package com.example.assignment.repository;

import com.example.assignment.entity.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, Long> {
    List<DatasetRecord> findAllByDatasetNameOrderByIdAsc(String datasetName);
}
