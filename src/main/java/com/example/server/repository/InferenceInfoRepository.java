package com.example.server.repository;

import com.example.server.model.InferenceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

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
                DetectionID INTEGER NOT NULL,
                RCResultID INTEGER NOT NULL,
                TCRResultID INTEGER NOT NULL,
                TCCResultID INTEGER NOT NULL,
                FOREIGN KEY (DetectionID) REFERENCES DetectionInfo(DetectionID),
                FOREIGN KEY (RCResultID) REFERENCES RUS_CHN_Result(RCResultID),
                FOREIGN KEY (TCRResultID) REFERENCES TW3_C_RUS_Result(TCRResultID),
                FOREIGN KEY (TCCResultID) REFERENCES TW3_C_CARPAL_Result(TCCResultID)
            )""");
    }

    public void save(InferenceInfo inferenceInfo) {
        String sql = "INSERT INTO inference_info (DetectionID, RCResultID, TCRResultID, TCCResultID) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, inferenceInfo.getDetectionID(), inferenceInfo.getRCResultID(), inferenceInfo.getTCRResultID(), inferenceInfo.getTCCResultID());
    }
}