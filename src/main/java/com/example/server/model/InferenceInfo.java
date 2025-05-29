package com.example.server.model;

import lombok.Data;

@Data
public class InferenceInfo {
    private Long InferenceID;
    private Long DetectionID;
    private Long RCResultID;
    private Long TCRResultID;
    private Long TCCResultID;
}