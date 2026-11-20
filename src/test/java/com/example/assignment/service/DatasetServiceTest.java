package com.example.assignment.service;

import com.example.assignment.entity.DatasetRecord;
import com.example.assignment.exception.BadRequestException;
import com.example.assignment.exception.ResourceNotFoundException;
import com.example.assignment.repository.DatasetRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatasetServiceTest {
    private DatasetRecordRepository repository;
    private DatasetService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach void setUp() { repository = mock(DatasetRecordRepository.class); service = new DatasetService(repository, mapper); }
    private DatasetRecord record(long id, String json) {
        DatasetRecord record;
        try { record = new DatasetRecord("employees", mapper.readTree(json)); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        try { var field = DatasetRecord.class.getDeclaredField("id"); field.setAccessible(true); field.set(record, id); }
        catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        return record;
    }
    private void seed() { when(repository.findAllByDatasetNameOrderByIdAsc("employees")).thenReturn(List.of(
            record(1, "{\"name\":\"Zed\",\"dept\":\"A\",\"age\":30}"),
            record(2, "{\"name\":\"Amy\",\"dept\":\"A\",\"age\":25}"),
            record(3, "{\"name\":\"Bo\",\"dept\":\"B\",\"age\":28}"),
            record(4, "{\"name\":\"Null\",\"dept\":null}")));
    }
    @Test void insertsOnlyJsonObjectsAndReturnsId() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(BadRequestException.class, () -> service.insert("employees", mapper.readTree("[1,2]")));
        assertThrows(BadRequestException.class, () -> service.insert("bad name", mapper.readTree("{}")));
    }
    @Test void groupsByDynamicStringFieldAndKeepsFullRecords() { seed();
        var response = service.query("employees", "dept", null, null);
        assertEquals(3, response.groupedRecords().size());
        assertEquals(2, response.groupedRecords().get("A").size());
        assertEquals("Zed", response.groupedRecords().get("A").get(0).get("name"));
        assertTrue(response.groupedRecords().containsKey("null"));
    }
    @Test void sortsNumbersAndStringsInBothDirections() { seed();
        assertEquals(25, service.query("employees", null, "age", "asc").sortedRecords().get(0).get("age"));
        assertEquals(30, service.query("employees", null, "age", "desc").sortedRecords().get(0).get("age"));
        assertEquals("Amy", service.query("employees", null, "name", "asc").sortedRecords().get(0).get("name"));
    }
    @Test void sortsWithinGroupsAndValidatesArguments() { seed();
        var groups = service.query("employees", "dept", "age", "asc").groupedRecords();
        assertEquals(25, groups.get("A").get(0).get("age"));
        assertThrows(BadRequestException.class, () -> service.query("employees", null, "age", "sideways"));
        assertThrows(BadRequestException.class, () -> service.query("employees", null, null, "asc"));
    }
    @Test void missingSortFieldsSortAfterPresentFieldsAndEmptyDatasetIsNotFound() { seed();
        var records = service.query("employees", null, "age", "asc").sortedRecords();
        assertEquals("Null", records.get(records.size() - 1).get("name"));
        when(repository.findAllByDatasetNameOrderByIdAsc("empty")).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> service.query("empty", null, null, null));
    }
}
