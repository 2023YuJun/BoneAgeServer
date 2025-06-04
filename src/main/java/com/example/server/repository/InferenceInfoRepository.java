package com.example.server.repository;

import com.example.server.model.InferenceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;

@Repository
public class InferenceInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public InferenceInfoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeDatabase();
    }

    private void initializeDatabase() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS inference_info (
                InferenceID INTEGER PRIMARY KEY AUTOINCREMENT,
                DetectionID INTEGER,
                RCResultID INTEGER,
                TCRResultID INTEGER,
                TCCResultID INTEGER,
                FOREIGN KEY (DetectionID) REFERENCES DetectionInfo(DetectionID),
                FOREIGN KEY (RCResultID) REFERENCES RUS_CHN_Result(RCResultID),
                FOREIGN KEY (TCRResultID) REFERENCES TW3_C_RUS_Result(TCRResultID),
                FOREIGN KEY (TCCResultID) REFERENCES TW3_C_CARPAL_Result(TCCResultID)
            )""");
    }

    public Long save(InferenceInfo inferenceInfo) {
        String sql = "INSERT INTO inference_info (DetectionID, RCResultID, TCRResultID, TCCResultID) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"InferenceID"});
            ps.setObject(1, inferenceInfo.getDetectionID());
            ps.setObject(2, inferenceInfo.getRCResultID());
            ps.setObject(3, inferenceInfo.getTCRResultID());
            ps.setObject(4, inferenceInfo.getTCCResultID());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public InferenceInfo findById(Long inferenceId) {
        String sql = "SELECT * FROM inference_info WHERE InferenceID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{inferenceId}, (rs, rowNum) -> {
                InferenceInfo info = new InferenceInfo();
                info.setInferenceID(rs.getLong("InferenceID"));
                info.setDetectionID(rs.getLong("DetectionID"));
                info.setRCResultID(rs.getLong("RCResultID"));
                info.setTCRResultID(rs.getLong("TCRResultID"));
                info.setTCCResultID(rs.getLong("TCCResultID"));
                return info;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}