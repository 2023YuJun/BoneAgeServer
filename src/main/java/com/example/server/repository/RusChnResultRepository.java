package com.example.server.repository;

import com.example.server.model.RusChnResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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
                CreateTime, UpdateTime
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";

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
            // 设置 CreateTime 和 UpdateTime 为 UTC 时间
            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            ps.setString(index++, nowUtc.format(formatter));
            ps.setString(index, nowUtc.format(formatter));

            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public RusChnResult findById(Long rcResultId) {
        String sql = "SELECT * FROM RUS_CHN_Result WHERE RCResultID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{rcResultId}, (rs, rowNum) -> {
                RusChnResult result = new RusChnResult();
                result.setRcResultId(rs.getInt("RCResultID"));
                result.setMcpFirst(rs.getInt("MCPFirst"));
                result.setMcpThird(rs.getInt("MCPThird"));
                result.setMcpFifth(rs.getInt("MCPFifth"));
                result.setPipFirst(rs.getInt("PIPFirst"));
                result.setPipThird(rs.getInt("PIPThird"));
                result.setPipFifth(rs.getInt("PIPFifth"));
                result.setMipThird(rs.getInt("MIPThird"));
                result.setMipFifth(rs.getInt("MIPFifth"));
                result.setDipFirst(rs.getInt("DIPFirst"));
                result.setDipThird(rs.getInt("DIPThird"));
                result.setDipFifth(rs.getInt("DIPFifth"));
                result.setRadius(rs.getInt("Radius"));
                result.setUlna(rs.getInt("Ulna"));
                result.setTotal(rs.getInt("Total"));
                result.setBoneAge(rs.getDouble("BoneAge"));

                // 创建格式化器
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                // 处理 CreateTime
                String createTimeStr = rs.getString("CreateTime");
                if (createTimeStr != null && !createTimeStr.isEmpty()) {
                    try {
                        result.setCreateTime(LocalDateTime.parse(createTimeStr, formatter));
                    } catch (Exception e) {
                        System.err.println("解析 CreateTime 失败: " + createTimeStr);
                    }
                }

                // 处理 UpdateTime
                String updateTimeStr = rs.getString("UpdateTime");
                if (updateTimeStr != null && !updateTimeStr.isEmpty()) {
                    try {
                        result.setUpdateTime(LocalDateTime.parse(updateTimeStr, formatter));
                    } catch (Exception e) {
                        System.err.println("解析 UpdateTime 失败: " + updateTimeStr);
                    }
                }

                return result;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public int update(RusChnResult result) {
        String sql = """
            UPDATE RUS_CHN_Result SET
                MCPFirst = ?, MCPThird = ?, MCPFifth = ?,
                PIPFirst = ?, PIPThird = ?, PIPFifth = ?,
                MIPThird = ?, MIPFifth = ?,
                DIPFirst = ?, DIPThird = ?, DIPFifth = ?,
                Radius = ?, Ulna = ?, Total = ?, BoneAge = ?,
                UpdateTime = ?
            WHERE RCResultID = ?""";

        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String updateTime = nowUtc.format(formatter);

        return jdbcTemplate.update(sql,
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
                updateTime,
                result.getRcResultId());
    }
}