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
            ps.setString(index, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

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

                // 添加时间字段处理
                Object createTime = rs.getObject("CreateTime");
                if (createTime != null) {
                    result.setCreateTime(LocalDateTime.parse(createTime.toString().replace(" ", "T")));
                }

                Object updateTime = rs.getObject("UpdateTime");
                if (updateTime != null) {
                    result.setUpdateTime(LocalDateTime.parse(updateTime.toString().replace(" ", "T")));
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
                Timestamp.valueOf(LocalDateTime.now()),
                result.getRcResultId());
    }
}