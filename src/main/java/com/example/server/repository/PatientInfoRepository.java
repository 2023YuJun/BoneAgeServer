package com.example.server.repository;

import com.example.server.model.PatientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;

@Repository
public class PatientInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PatientInfoRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initializeDatabase();
    }

    private void initializeDatabase() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS patient_info (
                PID INTEGER PRIMARY KEY AUTOINCREMENT,
                PatientID TEXT NOT NULL,
                BrithDate DATE NOT NULL,
                Sex TEXT,
                StudyInstanceUID TEXT NOT NULL,
                SeriesInstanceUID TEXT NOT NULL,
                SOPInstanceUID TEXT NOT NULL,
                InferenceID INTEGER NOT NULL,
                StudyDate DATE,
                CreateTime DATETIME DEFAULT CURRENT_TIMESTAMP
                FOREIGN KEY (InferenceID) REFERENCES InferenceInfo(InferenceID)
            )""");
    }

    public void save(PatientInfo patientInfo) {
        String sql = "INSERT INTO patient_info (PatientID, BrithDate, Sex, StudyInstanceUID, SeriesInstanceUID, SOPInstanceUID, InferenceID, StudyDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                patientInfo.getPatientID(),
                patientInfo.getBrithDate(),
                patientInfo.getSex(),
                patientInfo.getStudyInstanceUID(),
                patientInfo.getSeriesInstanceUID(),
                patientInfo.getSOPInstanceUID(),
                patientInfo.getInferenceID(),
                patientInfo.getStudyDate()
        );
    }

    public LocalDate findLatestStudyDateByPatient(String patientID) {
        String sql = "SELECT MAX(StudyDate) FROM patient_info WHERE PatientID = ?";
        return jdbcTemplate.queryForObject(sql, LocalDate.class, patientID);
    }
}