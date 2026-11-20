package com.example.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Dataset JSON API",
        version = "1.0.0",
        description = "Store JSON records in named datasets and query them with dynamic grouping and sorting."
))
public class AssignmentApplication {
    public static void main(String[] args) { SpringApplication.run(AssignmentApplication.class, args); }
}
