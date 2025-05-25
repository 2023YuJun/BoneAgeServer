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
                RequestLogID INTEGER PRIMARY KEY AUTOINCREMENT,
                Method TEXT NOT NULL,
                Path TEXT NOT NULL,
                ClientIP TEXT NOT NULL,
                Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )""");
    }

    public void save(RequestLog log) {
        String sql = "INSERT INTO request_log (Method, Path, ClientIP) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, log.getMethod(), log.getPath(), log.getClientIP());
    }

    public void deleteOldLogs() {
        String sql = "DELETE FROM request_log WHERE Timestamp < datetime('now', '-180 day')";
        jdbcTemplate.update(sql);
    }
}
