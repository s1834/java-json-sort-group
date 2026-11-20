package com.example.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DatasetApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void servesSwaggerUiAndOpenApiDocument() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Dataset JSON API"))
                .andExpect(jsonPath("$.paths['/api/dataset/{datasetName}/record']").exists())
                .andExpect(jsonPath("$.paths['/api/dataset/{datasetName}/query']").exists());
    }

    @Test void insertsRecordsGroupsSortsAndKeepsDatasetsIsolated() throws Exception {
        mvc.perform(post("/api/dataset/team-a/record").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Zed\",\"team\":\"ops\",\"age\":40}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.recordId").isNumber());
        mvc.perform(post("/api/dataset/team-a/record").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Amy\",\"team\":\"ops\",\"age\":20}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/dataset/team-b/record").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bea\",\"age\":10}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/dataset/team-a/query").param("groupBy", "team").param("sortBy", "age").param("order", "asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.groupedRecords.ops[0].name").value("Amy"));
        mvc.perform(get("/api/dataset/team-a/query"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records.length()").value(2));
        mvc.perform(get("/api/dataset/team-b/query"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records.length()").value(1));
        mvc.perform(post("/api/dataset/team-a/record").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/api/dataset/missing/query")).andExpect(status().isNotFound());
        mvc.perform(get("/api/dataset/team-a/query").param("sortBy", "age").param("order", "sideways"))
                .andExpect(status().isBadRequest());
    }
}
