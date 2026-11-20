package com.example.assignment.service;

import com.example.assignment.dto.InsertRecordResponse;
import com.example.assignment.dto.QueryResponse;
import com.example.assignment.entity.DatasetRecord;
import com.example.assignment.exception.BadRequestException;
import com.example.assignment.exception.ResourceNotFoundException;
import com.example.assignment.repository.DatasetRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DatasetService {
    private static final Pattern DATASET_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private final DatasetRecordRepository repository;
    private final ObjectMapper mapper;

    public DatasetService(DatasetRecordRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }

    @Transactional
    public InsertRecordResponse insert(String datasetName, JsonNode payload) {
        validateDatasetName(datasetName);
        if (payload == null || !payload.isObject()) throw new BadRequestException("Record payload must be a JSON object");
        try {
            DatasetRecord saved = repository.save(new DatasetRecord(datasetName, payload.deepCopy()));
            return new InsertRecordResponse("Record added successfully", datasetName, saved.getId());
        } catch (Exception ex) { throw new BadRequestException("Record contains invalid JSON values"); }
    }

    @Transactional(readOnly = true)
    public QueryResponse query(String datasetName, String groupBy, String sortBy, String order) {
        validateDatasetName(datasetName);
        if (sortBy == null && order != null) throw new BadRequestException("order requires sortBy");
        boolean descending = false;
        if (sortBy != null) {
            if (order == null) order = "asc";
            if (!order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc")) throw new BadRequestException("Invalid sort order. Allowed values: asc, desc");
            descending = order.equalsIgnoreCase("desc");
        }
        List<DatasetRecord> stored = repository.findAllByDatasetNameOrderByIdAsc(datasetName);
        if (stored.isEmpty()) throw new ResourceNotFoundException("Dataset not found or contains no records: " + datasetName);
        List<Map<String, Object>> records = stored.stream().map(this::decode).toList();
        if (groupBy == null && sortBy == null) return QueryResponse.all(records);
        if (groupBy != null && groupBy.isBlank()) throw new BadRequestException("groupBy field must not be blank");
        if (sortBy != null && sortBy.isBlank()) throw new BadRequestException("sortBy field must not be blank");
        if (sortBy != null) records = sorted(records, sortBy, descending);
        if (groupBy == null) return QueryResponse.sorted(records);
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            Object value = record.get(groupBy);
            String key = value == null ? "null" : String.valueOf(value);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
        }
        return QueryResponse.grouped(groups);
    }

    private List<Map<String, Object>> sorted(List<Map<String, Object>> records, String field, boolean descending) {
        Comparator<Map<String, Object>> comparator = (left, right) -> {
            Object leftValue = left.get(field);
            Object rightValue = right.get(field);
            // Null and missing values stay last in both directions.
            if (leftValue == null) return rightValue == null ? 0 : 1;
            if (rightValue == null) return -1;
            int result = compare(leftValue, rightValue, field);
            return descending ? -result : result;
        };
        return new ArrayList<>(records.stream().sorted(comparator).toList());
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object left, Object right, String field) {
        if (left == null) return right == null ? 0 : 1;
        if (right == null) return -1;
        if (left instanceof Number l && right instanceof Number r) return new BigDecimal(l.toString()).compareTo(new BigDecimal(r.toString()));
        if (left instanceof String l && right instanceof String r) return l.compareToIgnoreCase(r);
        if (left instanceof Boolean l && right instanceof Boolean r) return l.compareTo(r);
        if (left.getClass().equals(right.getClass()) && left instanceof Comparable comparable) return comparable.compareTo(right);
        throw new BadRequestException("Values for sortBy field '" + field + "' have incompatible or unsupported types");
    }
    private Map<String, Object> decode(DatasetRecord record) {
        try { return mapper.convertValue(record.getRecordData(), new TypeReference<>() { }); }
        catch (Exception ex) { throw new IllegalStateException("Stored JSON record could not be read", ex); }
    }
    private void validateDatasetName(String name) {
        if (name == null || !DATASET_NAME.matcher(name).matches()) throw new BadRequestException("Dataset name must be 1-128 characters and contain only letters, numbers, underscore, or hyphen");
    }
}
