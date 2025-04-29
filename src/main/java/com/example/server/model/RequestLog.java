package com.example.server.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestLog {
    private Long id;
    private String method;      // 请求方法（GET/POST等）
    private String path;        // 请求路径
    private String clientIp;    // 客户端IP
    private LocalDateTime timestamp; // 请求时间
}