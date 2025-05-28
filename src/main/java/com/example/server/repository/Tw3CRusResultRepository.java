package com.example.server.repository;

import com.example.server.model.Tw3CRusResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Tw3CRusResultRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Tw3CRusResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS TW3_C_RUS_Result (
                TCRResultID INTEGER PRIMARY KEY AUTOINCREMENT,
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

    public void save(Tw3CRusResult result) {
        String sql = """
            INSERT INTO TW3_C_RUS_Result (
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
