package com.example.assignment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

@Entity
@Table(name = "dataset_records", indexes = @Index(name = "idx_dataset_records_dataset_name", columnList = "dataset_name"))
public class DatasetRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dataset_name", nullable = false, length = 128)
    private String datasetName;
    @Column(name = "record_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode recordData;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DatasetRecord() { }
    public DatasetRecord(String datasetName, JsonNode recordData) {
        this.datasetName = datasetName; this.recordData = recordData;
    }
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getDatasetName() { return datasetName; }
    public JsonNode getRecordData() { return recordData; }
}
