package com.example.server.controller;

import com.example.server.model.RusChnResult;
import com.example.server.model.Tw3CRusResult;
import com.example.server.repository.RusChnResultRepository;
import com.example.server.repository.Tw3CRusResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UpdateController {

    @Autowired
    private Tw3CRusResultRepository tw3CRusResultRepository;

    @Autowired
    private RusChnResultRepository rusChnResultRepository;

    @PutMapping("/updateTw3CRusResult")
    public ResponseEntity<Map<String, Object>> updateTw3CRusResult(
            @RequestBody Tw3CRusResult request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getTcrResultId() == null) {
            response.put("code", 1);
            response.put("message", "TCRResultID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            int updated = tw3CRusResultRepository.update(request);
            if (updated == 0) {
                response.put("code", 3);
                response.put("message", "更新失败，记录不存在");
                return ResponseEntity.status(404).body(response);
            }

            Tw3CRusResult updatedResult = tw3CRusResultRepository.findById(request.getTcrResultId());

            Map<String, Object> data = new HashMap<>();
            data.put("tcrResultId", updatedResult.getTcrResultId());
            data.put("mcpFirst", updatedResult.getMcpFirst());
            data.put("mcpThird", updatedResult.getMcpThird());
            data.put("mcpFifth", updatedResult.getMcpFifth());
            data.put("pipFirst", updatedResult.getPipFirst());
            data.put("pipThird", updatedResult.getPipThird());
            data.put("pipFifth", updatedResult.getPipFifth());
            data.put("mipThird", updatedResult.getMipThird());
            data.put("mipFifth", updatedResult.getMipFifth());
            data.put("dipFirst", updatedResult.getDipFirst());
            data.put("dipThird", updatedResult.getDipThird());
            data.put("dipFifth", updatedResult.getDipFifth());
            data.put("radius", updatedResult.getRadius());
            data.put("ulna", updatedResult.getUlna());
            data.put("total", updatedResult.getTotal());
            data.put("boneAge", updatedResult.getBoneAge());
            data.put("updateTime", updatedResult.getUpdateTime());

            response.put("code", 0);
            response.put("message", "TW3-C RUS数据更新成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "更新TW3-C RUS信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/updateRusChnResult")
    public ResponseEntity<Map<String, Object>> updateRusChnResult(
            @RequestBody RusChnResult request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getRcResultId() == null) {
            response.put("code", 1);
            response.put("message", "RCResultID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            int updated = rusChnResultRepository.update(request);
            if (updated == 0) {
                response.put("code", 3);
                response.put("message", "更新失败，记录不存在");
                return ResponseEntity.status(404).body(response);
            }

            RusChnResult updatedResult = rusChnResultRepository.findById(Long.valueOf(request.getRcResultId()));

            Map<String, Object> data = new HashMap<>();
            data.put("rcResultId", updatedResult.getRcResultId());
            data.put("mcpFirst", updatedResult.getMcpFirst());
            data.put("mcpThird", updatedResult.getMcpThird());
            data.put("mcpFifth", updatedResult.getMcpFifth());
            data.put("pipFirst", updatedResult.getPipFirst());
            data.put("pipThird", updatedResult.getPipThird());
            data.put("pipFifth", updatedResult.getPipFifth());
            data.put("mipThird", updatedResult.getMipThird());
            data.put("mipFifth", updatedResult.getMipFifth());
            data.put("dipFirst", updatedResult.getDipFirst());
            data.put("dipThird", updatedResult.getDipThird());
            data.put("dipFifth", updatedResult.getDipFifth());
            data.put("radius", updatedResult.getRadius());
            data.put("ulna", updatedResult.getUlna());
            data.put("total", updatedResult.getTotal());
            data.put("boneAge", updatedResult.getBoneAge());
            data.put("updateTime", updatedResult.getUpdateTime());

            response.put("code", 0);
            response.put("message", "RUS-CHN数据更新成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "更新RUS-CHN信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}