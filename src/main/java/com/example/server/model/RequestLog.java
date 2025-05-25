package com.example.server.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestLog {
    private Long RequestLogID;
    private String Method;      // 请求方法（GET/POST等）
    private String Path;        // 请求路径
    private String ClientIP;    // 客户端IP
    private LocalDateTime Timestamp; // 请求时间
}