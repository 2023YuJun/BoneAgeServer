package com.example.server.repository;

import com.example.server.model.RequestLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;

@Repository
public class RequestLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public RequestLogRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeDatabase();
    }

    private void initializeDatabase() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS request_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                method TEXT NOT NULL,
                path TEXT NOT NULL,
                client_ip TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )""");
    }

    public void save(RequestLog log) {
        String sql = "INSERT INTO request_log (method, path, client_ip) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, log.getMethod(), log.getPath(), log.getClientIp());
    }
}
