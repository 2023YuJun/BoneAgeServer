package com.example.server.repository;

import com.example.server.model.RusChnResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

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

    public Long save(RusChnResult result) {
        String sql = """
            INSERT INTO RUS_CHN_Result (
                MCPFirst, MCPThird, MCPFifth,
                PIPFirst, PIPThird, PIPFifth,
                MIPThird, MIPFifth,
                DIPFirst, DIPThird, DIPFifth,
                Radius, Ulna, Total, BoneAge,
                UpdateTime
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            // 设置参数
            int index = 1;
            ps.setObject(index++, result.getMcpFirst());
            ps.setObject(index++, result.getMcpThird());
            ps.setObject(index++, result.getMcpFifth());
            ps.setObject(index++, result.getPipFirst());
            ps.setObject(index++, result.getPipThird());
            ps.setObject(index++, result.getPipFifth());
            ps.setObject(index++, result.getMipThird());
            ps.setObject(index++, result.getMipFifth());
            ps.setObject(index++, result.getDipFirst());
            ps.setObject(index++, result.getDipThird());
            ps.setObject(index++, result.getDipFifth());
            ps.setObject(index++, result.getRadius());
            ps.setObject(index++, result.getUlna());
            ps.setObject(index++, result.getTotal());
            ps.setObject(index++, result.getBoneAge());
            ps.setTimestamp(index, Timestamp.valueOf(LocalDateTime.now()));

            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }
}