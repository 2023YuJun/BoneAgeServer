package com.example.server.repository;

import com.example.server.model.DetectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;

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

    public void save(DetectionInfo detectionInfo) {
        String sql = """
            INSERT INTO DetectionInfo (
                MCPFirst, MCPThird, MCPFifth, 
                PIPFirst, PIPThird, PIPFifth,
                MIPThird, MIPFifth, 
                DIPFirst, DIPThird, DIPFifth,
                Radius, Ulna
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""";

        jdbcTemplate.update(sql,
                detectionInfo.getMCPFirst(),
                detectionInfo.getMCPThird(),
                detectionInfo.getMCPFifth(),
                detectionInfo.getPIPFirst(),
                detectionInfo.getPIPThird(),
                detectionInfo.getPIPFifth(),
                detectionInfo.getMIPThird(),
                detectionInfo.getMIPFifth(),
                detectionInfo.getDIPFirst(),
                detectionInfo.getDIPThird(),
                detectionInfo.getDIPFifth(),
                detectionInfo.getRadius(),
                detectionInfo.getUlna()
        );
    }
}