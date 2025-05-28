package com.example.server.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RusChnResult {
    private Integer rcResultId;
    private Integer mcpFirst;
    private Integer mcpThird;
    private Integer mcpFifth;
    private Integer pipFirst;
    private Integer pipThird;
    private Integer pipFifth;
    private Integer mipThird;
    private Integer mipFifth;
    private Integer dipFirst;
    private Integer dipThird;
    private Integer dipFifth;
    private Integer radius;
    private Integer ulna;
    private Integer total;
    private Double boneAge;
    private LocalDateTime CreateTime;
    private LocalDateTime UpdateTime;
}
