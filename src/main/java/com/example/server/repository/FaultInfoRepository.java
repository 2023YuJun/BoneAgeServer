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
                FaultID INTEGER PRIMARY KEY AUTOINCREMENT,
                Content TEXT NOT NULL,
                DeviceIP TEXT NOT NULL,
                Create_time DATETIME DEFAULT CURRENT_TIMESTAMP
            )""");
    }

    public void save(FaultInfo faultInfo) {
        String sql = "INSERT INTO fault_info (Content, DeviceIP) VALUES (?, ?)";
        jdbcTemplate.update(sql, faultInfo.getContent(), faultInfo.getDeviceIP());
    }
}
