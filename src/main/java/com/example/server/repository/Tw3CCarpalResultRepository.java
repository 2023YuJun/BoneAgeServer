package com.example.server.repository;

import com.example.server.model.Tw3CCarpalResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Tw3CCarpalResultRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Tw3CCarpalResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS TW3_C_CARPAL_Result (
                TCCResultID INTEGER PRIMARY KEY AUTOINCREMENT,
                Total INTEGER,
                BoneAge REAL
            )""");
    }
    public void save(Tw3CCarpalResult result) {
        String sql = """
            INSERT INTO TW3_C_CARPAL_Result (
                Total, BoneAge
            ) VALUES (?,?)""";

        jdbcTemplate.update(sql,
                result.getTotal(),
                result.getBoneAge()
        );
    }
}