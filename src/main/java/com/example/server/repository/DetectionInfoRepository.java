package com.example.server.repository;

import com.example.server.model.DetectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class DetectionInfoRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DetectionInfoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeDatabase();
    }

    private void initializeDatabase() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS DetectionInfo (
                DetectionID INTEGER PRIMARY KEY AUTOINCREMENT,
                MCPFirst TEXT,
                MCPThird TEXT,
                MCPFifth TEXT,
                PIPFirst TEXT,
                PIPThird TEXT,
                PIPFifth TEXT,
                MIPThird TEXT,
                MIPFifth TEXT,
                DIPFirst TEXT,
                DIPThird TEXT,
                DIPFifth TEXT,
                Radius TEXT,
                Ulna TEXT
            )""");
    }

    public Long save(DetectionInfo detectionInfo) {
        String sql = """
            INSERT INTO DetectionInfo (
                MCPFirst, MCPThird, MCPFifth, 
                PIPFirst, PIPThird, PIPFifth,
                MIPThird, MIPFifth, 
                DIPFirst, DIPThird, DIPFifth,
                Radius, Ulna
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            int index = 1;
            ps.setString(index++, detectionInfo.getMCPFirst());
            ps.setString(index++, detectionInfo.getMCPThird());
            ps.setString(index++, detectionInfo.getMCPFifth());
            ps.setString(index++, detectionInfo.getPIPFirst());
            ps.setString(index++, detectionInfo.getPIPThird());
            ps.setString(index++, detectionInfo.getPIPFifth());
            ps.setString(index++, detectionInfo.getMIPThird());
            ps.setString(index++, detectionInfo.getMIPFifth());
            ps.setString(index++, detectionInfo.getDIPFirst());
            ps.setString(index++, detectionInfo.getDIPThird());
            ps.setString(index++, detectionInfo.getDIPFifth());
            ps.setString(index++, detectionInfo.getRadius());
            ps.setString(index++, detectionInfo.getUlna());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public DetectionInfo findById(Long detectionId) {
        String sql = "SELECT * FROM DetectionInfo WHERE DetectionID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{detectionId}, (rs, rowNum) -> {
                DetectionInfo info = new DetectionInfo();
                info.setDetectionId(rs.getLong("DetectionID"));
                info.setMCPFirst(rs.getString("MCPFirst"));
                info.setMCPThird(rs.getString("MCPThird"));
                info.setMCPFifth(rs.getString("MCPFifth"));
                info.setPIPFirst(rs.getString("PIPFirst"));
                info.setPIPThird(rs.getString("PIPThird"));
                info.setPIPFifth(rs.getString("PIPFifth"));
                info.setMIPThird(rs.getString("MIPThird"));
                info.setMIPFifth(rs.getString("MIPFifth"));
                info.setDIPFirst(rs.getString("DIPFirst"));
                info.setDIPThird(rs.getString("DIPThird"));
                info.setDIPFifth(rs.getString("DIPFifth"));
                info.setRadius(rs.getString("Radius"));
                info.setUlna(rs.getString("Ulna"));
                return info;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}