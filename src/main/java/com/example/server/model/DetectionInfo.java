package com.example.server.model;

import lombok.Data;

@Data
public class DetectionInfo {
    private Long detectionId;
    private String MCPFirst;
    private String MCPThird;
    private String MCPFifth;
    private String PIPFirst;
    private String PIPThird;
    private String PIPFifth;
    private String MIPThird;
    private String MIPFifth;
    private String DIPFirst;
    private String DIPThird;
    private String DIPFifth;
    private String Radius;
    private String Ulna;
}
