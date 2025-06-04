package com.example.server.controller;

import com.example.server.model.RusChnResult;
import com.example.server.model.Tw3CRusResult;
import com.example.server.repository.RusChnResultRepository;
import com.example.server.repository.Tw3CRusResultRepository;
import com.example.server.Utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@EnableAsync
public class UpdateController {

    @Autowired
    private Tw3CRusResultRepository tw3CRusResultRepository;

    @Autowired
    private RusChnResultRepository rusChnResultRepository;

    @Async("controllerTaskExecutor")
    @PutMapping("/updateTw3CRusResult")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateTw3CRusResult(
            @RequestBody Tw3CRusResult request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request.getTcrResultId() == null) {
                return ApiResponse.error(1, "TCRResultID不能为空");
            }

            try {
                int updated = tw3CRusResultRepository.update(request);
                if (updated == 0) {
                    return ApiResponse.notFound(3, "更新失败，记录不存在");
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

                return ApiResponse.success("TW3-C RUS数据更新成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "更新TW3-C RUS信息失败: " + e.getMessage());
            }
        });

    }

    @Async("controllerTaskExecutor")
    @PutMapping("/updateRusChnResult")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateRusChnResult(
            @RequestBody RusChnResult request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request.getRcResultId() == null) {
                return ApiResponse.error(1, "RCResultID不能为空");
            }

            try {
                int updated = rusChnResultRepository.update(request);
                if (updated == 0) {
                    return ApiResponse.notFound(3, "更新失败，记录不存在");
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

                return ApiResponse.success("RUS-CHN数据更新成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "更新RUS-CHN信息失败: " + e.getMessage());
            }
        });

    }
}