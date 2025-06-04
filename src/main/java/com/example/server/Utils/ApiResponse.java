package com.example.server.Utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ApiResponse {

    // 成功响应快捷方法
    public static ResponseEntity<Map<String, Object>> success() {
        return success("操作成功");
    }

    public static ResponseEntity<Map<String, Object>> success(String message) {
        return success(message, null);
    }

    public static ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return ResponseEntity.ok(buildResponse(0, message, data));
    }

    // 错误响应快捷方法
    public static ResponseEntity<Map<String, Object>> error(int code, String message) {
        return ResponseEntity.badRequest().body(buildResponse(code, message, null));
    }

    public static ResponseEntity<Map<String, Object>> notFound(int code, String message) {
        return ResponseEntity.status(404).body(buildResponse(code, message, null));
    }

    public static ResponseEntity<Map<String, Object>> serverError(int code, String message) {
        return ResponseEntity.internalServerError().body(buildResponse(code, message, null));
    }

    public static ResponseEntity<Map<String, Object>> safeServerError(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    // 自定义状态码响应
    public static ResponseEntity<Map<String, Object>> response(int httpStatus, int code, String message, Object data) {
        return ResponseEntity.status(httpStatus).body(buildResponse(code, message, data));
    }

    // 私有方法构建响应体
    private static Map<String, Object> buildResponse(int code, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}