package com.example.server.repository;

import com.example.server.model.Tw3CCarpalResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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

    public Tw3CCarpalResult findById(Long tccResultId) {
        String sql = "SELECT * FROM TW3_C_CARPAL_Result WHERE TCCResultID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{tccResultId}, (rs, rowNum) -> {
                Tw3CCarpalResult result = new Tw3CCarpalResult();
                result.setTccResultId(rs.getLong("TCCResultID"));
                result.setTotal(rs.getInt("Total"));
                result.setBoneAge(rs.getDouble("BoneAge"));

                return result;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}