package com.example.server.repository;

import com.example.server.model.FaultInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
public class FaultInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FaultInfoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeDatabase();
    }

    private void initializeDatabase() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS fault_info (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                deviceIP TEXT NOT NULL,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP
            )""");
    }

    public void save(FaultInfo faultInfo) {
        String sql = "INSERT INTO fault_info (content, deviceIP) VALUES (?, ?)";
        jdbcTemplate.update(sql, faultInfo.getContent(), faultInfo.getDeviceIP());
    }
}
