package com.example.server.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FaultInfo {
    private Long id;
    private String content;
    private String deviceIP;
    private LocalDateTime createTime;
}