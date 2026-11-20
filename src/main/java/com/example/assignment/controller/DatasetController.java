package com.example.assignment.controller;

import com.example.assignment.dto.InsertRecordResponse;
import com.example.assignment.dto.QueryResponse;
import com.example.assignment.service.DatasetService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dataset/{datasetName}")
@Tag(name = "Datasets", description = "Insert and query arbitrary JSON records grouped by dataset.")
public class DatasetController {
    private final DatasetService service;
    public DatasetController(DatasetService service) { this.service = service; }

    @PostMapping("/record")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Insert a JSON record", description = "Adds one JSON object to the named dataset. Dataset names may contain letters, numbers, underscores, and hyphens.", responses = {
            @ApiResponse(responseCode = "201", description = "Record added"),
            @ApiResponse(responseCode = "400", description = "Invalid dataset name or JSON object")
    })
    public InsertRecordResponse insert(
            @Parameter(description = "Dataset identifier", example = "employees")
            @PathVariable @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,127}") String datasetName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Any valid JSON object. Fields are retained as provided and can be used later for grouping or sorting.",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                            examples = @ExampleObject(name = "Employee", value = "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}")))
            @RequestBody JsonNode body) {
        return service.insert(datasetName, body);
    }

    @GetMapping("/query")
    @Operation(summary = "Query dataset records", description = "Returns all records, sorted records, grouped records, or records grouped with each group's members sorted. Missing or null group values are placed in a `null` group. Sorting supports numeric, string, and boolean values.", responses = {
            @ApiResponse(responseCode = "200", description = "Query results"),
            @ApiResponse(responseCode = "400", description = "Invalid grouping or sorting parameters"),
            @ApiResponse(responseCode = "404", description = "Dataset has no records")
    })
    public QueryResponse query(
            @Parameter(description = "Dataset identifier", example = "employees") @PathVariable String datasetName,
            @Parameter(description = "JSON field used to group records; can be combined with sortBy", example = "department")
            @RequestParam(required = false) String groupBy,
            @Parameter(description = "JSON field used to sort records (within groups when groupBy is supplied)", example = "age")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction; `asc` or `desc` (defaults to `asc` when sortBy is set)", example = "asc", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(required = false) String order) {
        return service.query(datasetName, groupBy, sortBy, order);
    }
}
