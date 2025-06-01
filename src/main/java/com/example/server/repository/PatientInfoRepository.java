package com.example.server.repository;

import com.example.server.model.PatientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
                SOPInstanceUID TEXT UNIQUE,
                InferenceID INTEGER,
                StudyDate DATE,
                CreateTime DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (InferenceID) REFERENCES InferenceInfo(InferenceID)
            )""");
    }

    public long save(PatientInfo patientInfo) {
        String sql = "INSERT INTO patient_info (PatientID, BrithDate, Sex, StudyInstanceUID, " +
                "SeriesInstanceUID, SOPInstanceUID, InferenceID, StudyDate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );
            int index = 1;
            ps.setString(index++, patientInfo.getPatientID());

            // 使用字符串格式存储日期
            ps.setString(index++, patientInfo.getBrithDate() != null ?
                    patientInfo.getBrithDate().toString() : null);

            ps.setString(index++, patientInfo.getSex());
            ps.setString(index++, patientInfo.getStudyInstanceUID());
            ps.setString(index++, patientInfo.getSeriesInstanceUID());
            ps.setString(index++, patientInfo.getSOPInstanceUID());
            ps.setObject(index++, patientInfo.getInferenceID());

            // 使用字符串格式存储日期
            ps.setString(index++, patientInfo.getStudyDate() != null ?
                    patientInfo.getStudyDate().toString() : null);

            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public LocalDate findLatestStudyDateByPatient(String patientID) {
        String sql = "SELECT MAX(StudyDate) FROM patient_info WHERE PatientID = ?";
        String dateStr = jdbcTemplate.queryForObject(sql, String.class, patientID);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.err.println("解析最新 StudyDate 失败: " + dateStr);
            return null;
        }
    }

    public void updateInferenceID(Long pid, Long inferenceID) {
        String sql = "UPDATE patient_info SET InferenceID = ? WHERE PID = ?";
        jdbcTemplate.update(sql, inferenceID, pid);
    }

    public List<PatientInfo> findUnprocessedRecords(String patientID) {
        String sql = "SELECT * FROM patient_info WHERE PatientID = ? AND InferenceID IS NULL";
        return jdbcTemplate.query(sql, new Object[]{patientID}, (rs, rowNum) -> {
            PatientInfo info = new PatientInfo();
            // 安全处理 PID (Long)
            Object pidObj = rs.getObject("PID");
            if (pidObj instanceof Number) {
                info.setPID(((Number) pidObj).longValue());
            } else {
                info.setPID(null);
                System.err.println("PID 值类型不匹配: " + pidObj);
            }
            info.setPatientID(rs.getString("PatientID"));

            // 安全解析 BrithDate
            String birthDateStr = rs.getString("BrithDate");
            if (birthDateStr != null && !birthDateStr.isEmpty()) {
                try {
                    info.setBrithDate(LocalDate.parse(birthDateStr));
                } catch (Exception e) {
                    System.err.println("解析 BrithDate 失败: " + birthDateStr);
                    info.setBrithDate(null);
                }
            } else {
                info.setBrithDate(null);
            }

            info.setSex(rs.getString("Sex"));
            info.setStudyInstanceUID(rs.getString("StudyInstanceUID"));
            info.setSeriesInstanceUID(rs.getString("SeriesInstanceUID"));
            info.setSOPInstanceUID(rs.getString("SOPInstanceUID"));

            // 安全处理 InferenceID (Long)
            Object inferenceIdObj = rs.getObject("InferenceID");
            if (inferenceIdObj instanceof Number) {
                info.setInferenceID(((Number) inferenceIdObj).longValue());
            } else if (inferenceIdObj == null) {
                info.setInferenceID(null);
            } else {
                info.setInferenceID(null);
                System.err.println("InferenceID 值类型不匹配: " + inferenceIdObj);
            }

            // 安全解析 StudyDate
            String studyDateStr = rs.getString("StudyDate");
            if (studyDateStr != null && !studyDateStr.isEmpty()) {
                try {
                    info.setStudyDate(LocalDate.parse(studyDateStr));
                } catch (Exception e) {
                    System.err.println("解析 StudyDate 失败: " + studyDateStr);
                    info.setStudyDate(null);
                }
            } else {
                info.setStudyDate(null);
            }

            return info;
        });
    }

    public List<PatientInfo> findByPatientID(String patientID) {
        String sql = "SELECT * FROM patient_info WHERE PatientID = ?";
        return jdbcTemplate.query(sql, new Object[]{patientID}, (rs, rowNum) -> {
            PatientInfo info = new PatientInfo();
            // 安全处理 PID (Long)
            Object pidObj = rs.getObject("PID");
            if (pidObj instanceof Number) {
                info.setPID(((Number) pidObj).longValue());
            }
            info.setPatientID(rs.getString("PatientID"));

            // 安全解析 BrithDate
            String birthDateStr = rs.getString("BrithDate");
            if (birthDateStr != null && !birthDateStr.isEmpty()) {
                try {
                    info.setBrithDate(LocalDate.parse(birthDateStr));
                } catch (Exception e) {
                    System.err.println("解析 BrithDate 失败: " + birthDateStr);
                }
            }

            info.setSex(rs.getString("Sex"));
            info.setStudyInstanceUID(rs.getString("StudyInstanceUID"));
            info.setSeriesInstanceUID(rs.getString("SeriesInstanceUID"));
            info.setSOPInstanceUID(rs.getString("SOPInstanceUID"));

            // 安全处理 InferenceID (Long)
            Object inferenceIdObj = rs.getObject("InferenceID");
            if (inferenceIdObj instanceof Number) {
                info.setInferenceID(((Number) inferenceIdObj).longValue());
            }

            // 安全解析 StudyDate
            String studyDateStr = rs.getString("StudyDate");
            if (studyDateStr != null && !studyDateStr.isEmpty()) {
                try {
                    info.setStudyDate(LocalDate.parse(studyDateStr));
                } catch (Exception e) {
                    System.err.println("解析 StudyDate 失败: " + studyDateStr);
                }
            }

            // 添加 createTime 字段处理
            Object createTimeObj = rs.getObject("CreateTime");
            if (createTimeObj != null) {
                try {
                    info.setCreateTime(LocalDateTime.parse(createTimeObj.toString()));
                } catch (Exception e) {
                    System.err.println("解析 CreateTime 失败: " + createTimeObj);
                }
            }

            return info;
        });
    }

    public PatientInfo findBySOPInstanceUID(String sopInstanceUID) {
        String sql = "SELECT * FROM patient_info WHERE SOPInstanceUID = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{sopInstanceUID}, (rs, rowNum) -> {
                PatientInfo info = new PatientInfo();
                // 安全处理 PID
                Object pidObj = rs.getObject("PID");
                if (pidObj instanceof Number) {
                    info.setPID(((Number) pidObj).longValue());
                }
                info.setPatientID(rs.getString("PatientID"));

                // 解析 BrithDate
                String birthDateStr = rs.getString("BrithDate");
                if (birthDateStr != null && !birthDateStr.isEmpty()) {
                    try {
                        info.setBrithDate(LocalDate.parse(birthDateStr));
                    } catch (Exception e) {
                        System.err.println("解析 BrithDate 失败: " + birthDateStr);
                    }
                }

                info.setSex(rs.getString("Sex"));
                info.setStudyInstanceUID(rs.getString("StudyInstanceUID"));
                info.setSeriesInstanceUID(rs.getString("SeriesInstanceUID"));
                info.setSOPInstanceUID(rs.getString("SOPInstanceUID"));

                // 处理 InferenceID
                Object inferenceIdObj = rs.getObject("InferenceID");
                if (inferenceIdObj instanceof Number) {
                    info.setInferenceID(((Number) inferenceIdObj).longValue());
                }

                // 解析 StudyDate
                String studyDateStr = rs.getString("StudyDate");
                if (studyDateStr != null && !studyDateStr.isEmpty()) {
                    try {
                        info.setStudyDate(LocalDate.parse(studyDateStr));
                    } catch (Exception e) {
                        System.err.println("解析 StudyDate 失败: " + studyDateStr);
                    }
                }

                return info;
            });
        } catch (EmptyResultDataAccessException e) {
            return null; // 无匹配记录
        }
    }
}