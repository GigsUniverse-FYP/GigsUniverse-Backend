package com.giguniverse.backend.Connection;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/check")
public class PostgresConnectionCheck {

    private final DataSource dataSource;

    public PostgresConnectionCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/postgres")
    public String checkPostgres() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(1) ? "PostgreSQL connection is OK" : "PostgreSQL connection failed";
        } catch (Exception e) {
            return "PostgreSQL connection error: " + e.getMessage();
        }
    }
}
