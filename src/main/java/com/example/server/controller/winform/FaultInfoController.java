package com.example.server.controller.winform;

import com.example.server.model.FaultInfo;
import com.example.server.repository.FaultInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/winform")
public class FaultInfoController {

    private final FaultInfoRepository repository;

    @Autowired
    public FaultInfoController(FaultInfoRepository repository) {
        this.repository = repository;
    }
    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("后端服务正常，通信成功！");
    }

    @PostMapping("/faultinfo")
    public ResponseEntity<?> receiveFaultInfo(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        String deviceIP = request.get("deviceIP");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("内容不能为空");
        }
        if (deviceIP == null || deviceIP.isBlank()) {
            return ResponseEntity.badRequest().body("设备IP不能为空");
        }

        FaultInfo faultInfo = new FaultInfo();
        faultInfo.setContent(content);
        faultInfo.setDeviceIP(deviceIP);
        repository.save(faultInfo);

        return ResponseEntity.ok("故障反馈已成功保存");
    }
}
