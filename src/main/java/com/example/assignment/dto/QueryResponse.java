package com.example.assignment.dto;

import java.util.List;
import java.util.Map;

public record QueryResponse(List<Map<String, Object>> records,
                            Map<String, List<Map<String, Object>>> groupedRecords,
                            List<Map<String, Object>> sortedRecords) {
    public static QueryResponse all(List<Map<String, Object>> records) { return new QueryResponse(records, null, null); }
    public static QueryResponse sorted(List<Map<String, Object>> records) { return new QueryResponse(null, null, records); }
    public static QueryResponse grouped(Map<String, List<Map<String, Object>>> records) { return new QueryResponse(null, records, null); }
}
