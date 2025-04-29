package com.example.server.interceptor;

import com.example.server.model.RequestLog;
import com.example.server.repository.RequestLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {
    private final RequestLogRepository logRepository;

    public RequestLogInterceptor(RequestLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestLog log = new RequestLog();
        log.setMethod(request.getMethod());
        log.setPath(request.getRequestURI());
        log.setClientIp(request.getRemoteAddr());
        logRepository.save(log);
        return true;
    }
}
