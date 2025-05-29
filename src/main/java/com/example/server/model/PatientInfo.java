package com.example.server.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatientInfo {
    private Long PID;
    private String PatientID;
    private LocalDate BrithDate;
    private String Sex;
    private String StudyInstanceUID;
    private String SeriesInstanceUID;
    private String SOPInstanceUID;
    private Long InferenceID;
    private LocalDate StudyDate;
    private LocalDateTime CreateTime;

}