# Dataset JSON API

A Spring Boot REST API that stores arbitrary JSON objects in named datasets and returns them as complete records, dynamically grouped, dynamically sorted, or grouped with sorted members. Records are persisted individually in PostgreSQL JSONB.

## Stack

- Java 17+
- Spring Boot 3, Spring Web, Spring Data JPA, Bean Validation
- PostgreSQL JSONB
- Maven, JUnit 5, Mockito, Spring MockMvc

## Prerequisites and configuration

Install Java 17 or newer, Maven 3.8+, and Docker (or a local PostgreSQL 14+ instance). Configure these optional environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/datasetdb` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | (empty) | Database user |
| `SPRING_DATASOURCE_PASSWORD` | (empty) | Database password |
| `PORT` | `8080` | HTTP port; Render supplies this automatically |

For backward compatibility, `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` are accepted when their corresponding `SPRING_DATASOURCE_*` variable is unset.

Start PostgreSQL locally using Docker Compose:

```sh
docker compose up -d
```

For the Compose defaults, provide credentials without committing them, for example by exporting `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` in your shell before starting the app.

Run the application:

```sh
mvn spring-boot:run
```

Hibernate creates/updates the `dataset_records` table. The application indexes `dataset_name`; persisted records survive restarts.

Run tests:

```sh
mvn clean test
```

Tests use an in-memory H2 database in PostgreSQL compatibility mode; a PostgreSQL server is not needed to run tests.

## API

Dataset names must contain 1–128 letters, numbers, underscores, or hyphens; the first character must be alphanumeric. Request bodies must be JSON objects. Each insertion returns HTTP `201 Created`.

### Insert a record

`POST /api/dataset/{datasetName}/record`

```sh
curl -i -X POST http://localhost:8080/api/dataset/employees/record \
  -H 'Content-Type: application/json' \
  -d '{"id":1,"name":"John Doe","age":30,"department":"Engineering"}'
```

Response:

```json
{"message":"Record added successfully","dataset":"employees","recordId":1}
```

### Get all records

`GET /api/dataset/{datasetName}/query`

Returns `{"records":[...]}`. An empty or unknown dataset returns HTTP 404.

### Sort records

`GET /api/dataset/{datasetName}/query?sortBy=age&order=asc`

`order` accepts `asc` or `desc` (case-insensitive) and defaults to `asc` when `sortBy` is provided. Numeric values compare numerically, strings case-insensitively, and booleans by boolean order. Missing/null values sort last in ascending order and first in descending order. Equal values retain insertion order. Incompatible value types for a field produce HTTP 400.

Returns `{"sortedRecords":[...]}`.

### Group records

`GET /api/dataset/{datasetName}/query?groupBy=department`

Returns `{"groupedRecords":{"Engineering":[...],"null":[...]}}`. Missing and explicit null field values share the `null` group. Group keys are strings; objects and arrays use their JSON string representation. Complete original records are returned in each group.

### Group and sort together

`GET /api/dataset/{datasetName}/query?groupBy=department&sortBy=age&order=asc`

Returns `groupedRecords`; records are ordered within each group using the requested sort field and direction.

Errors use a JSON object with `timestamp`, `status`, `error`, and `message`. Invalid requests return HTTP 400; missing/empty datasets return 404; database failures return a generic 500 response.

## Postman workflow

1. Start the app and create a `POST` request to `http://localhost:8080/api/dataset/employees/record` with `Content-Type: application/json` and the example JSON body.
2. Repeat with other employee records.
3. Send `GET http://localhost:8080/api/dataset/employees/query` to list them.
4. Add `?groupBy=department`, `?sortBy=age&order=desc`, or both parameters to explore grouping and sorting.

## Storage notes

The `dataset_records` table stores one record per row with an auto-generated primary key, indexed dataset name, JSONB payload, and creation/update timestamps. Querying loads records only for the requested dataset; grouping and sorting are performed in Java, so arbitrary JSON keys never enter SQL expressions.
