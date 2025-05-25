package com.example.server.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FaultInfo {
    private Long FaultID;
    private String Content;
    private String DeviceIP;
    private LocalDateTime CreateTime;
}