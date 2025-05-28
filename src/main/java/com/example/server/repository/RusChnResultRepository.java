package com.example.server.repository;

import com.example.server.model.RusChnResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RusChnResultRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RusChnResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS RUS_CHN_Result (
                RCResultID INTEGER PRIMARY KEY AUTOINCREMENT,
                MCPFirst INTEGER,
                MCPThird INTEGER,
                MCPFifth INTEGER,
                PIPFirst INTEGER,
                PIPThird INTEGER,
                PIPFifth INTEGER,
                MIPThird INTEGER,
                MIPFifth INTEGER,
                DIPFirst INTEGER,
                DIPThird INTEGER,
                DIPFifth INTEGER,
                Radius INTEGER,
                Ulna INTEGER,
                Total INTEGER,
                BoneAge REAL,
                CreateTime DATETIME DEFAULT CURRENT_TIMESTAMP,
                UpdateTime DATETIME DEFAULT CURRENT_TIMESTAMP
            )""");
    }

    public void save(RusChnResult result) {
        String sql = """
            INSERT INTO RUS_CHN_Result (
                MCPFirst, MCPThird, MCPFifth,
                PIPFirst, PIPThird, PIPFifth,
                MIPThird, MIPFifth,
                DIPFirst, DIPThird, DIPFifth,
                Radius, Ulna, Total, BoneAge,
                UpdateTime
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";

        jdbcTemplate.update(sql,
                result.getMcpFirst(),
                result.getMcpThird(),
                result.getMcpFifth(),
                result.getPipFirst(),
                result.getPipThird(),
                result.getPipFifth(),
                result.getMipThird(),
                result.getMipFifth(),
                result.getDipFirst(),
                result.getDipThird(),
                result.getDipFifth(),
                result.getRadius(),
                result.getUlna(),
                result.getTotal(),
                result.getBoneAge(),
                result.getUpdateTime()
        );
    }
}
