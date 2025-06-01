package com.example.server.controller.vue;

import com.example.server.model.*;
import com.example.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vue/view")
public class ViewController {

    @Autowired
    private PatientInfoRepository patientInfoRepository;

    @Autowired
    private InferenceInfoRepository inferenceInfoRepository;

    @Autowired
    private DetectionInfoRepository detectionInfoRepository;

    @Autowired
    private Tw3CRusResultRepository tw3CRusResultRepository;

    @Autowired
    private RusChnResultRepository rusChnResultRepository;

    @GetMapping("/patientInfo")
    public ResponseEntity<Map<String, Object>> getPatientInfo(
            @RequestParam(value = "id", required = false) String patientId) {
        Map<String, Object> response = new HashMap<>();

        if (patientId == null || patientId.isEmpty()) {
            response.put("code", 1);
            response.put("message", "患者ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<PatientInfo> patients = patientInfoRepository.findByPatientID(patientId);
            Map<String, Object> data = new HashMap<>();
            data.put("totalRecords", patients.size());
            data.put("patients", patients);

            response.put("code", 0);
            response.put("message", "患者影像信息获取成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "获取患者信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/inferenceInfo")
    public ResponseEntity<Map<String, Object>> getInferenceInfo(
            @RequestParam(value = "id", required = false) Long inferenceId) {
        Map<String, Object> response = new HashMap<>();

        if (inferenceId == null) {
            response.put("code", 1);
            response.put("message", "推理ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            InferenceInfo inferenceInfo = inferenceInfoRepository.findById(inferenceId);
            if (inferenceInfo == null) {
                response.put("code", 3);
                response.put("message", "未找到推理信息");
                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("InferenceID", inferenceInfo.getInferenceID());
            data.put("DetectionID", inferenceInfo.getDetectionID());
            data.put("RCResultID", inferenceInfo.getRCResultID());
            data.put("TCRResultID", inferenceInfo.getTCRResultID());
            data.put("TCCResultID", inferenceInfo.getTCCResultID());

            response.put("code", 0);
            response.put("message", "患者推理信息获取成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "获取推理信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/detectionInfo")
    public ResponseEntity<Map<String, Object>> getDetectionInfo(
            @RequestParam(value = "id", required = false) Long detectionId) {
        Map<String, Object> response = new HashMap<>();

        if (detectionId == null) {
            response.put("code", 1);
            response.put("message", "检测ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            DetectionInfo detectionInfo = detectionInfoRepository.findById(detectionId);
            if (detectionInfo == null) {
                response.put("code", 3);
                response.put("message", "未找到检测信息");
                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("DetectionID", detectionInfo.getDetectionId());
            data.put("MCPFirst", detectionInfo.getMCPFirst());
            data.put("MCPThird", detectionInfo.getMCPThird());
            data.put("MCPFifth", detectionInfo.getMCPFifth());
            data.put("PIPFirst", detectionInfo.getPIPFirst());
            data.put("PIPThird", detectionInfo.getPIPThird());
            data.put("PIPFifth", detectionInfo.getPIPFifth());
            data.put("MIPThird", detectionInfo.getMIPThird());
            data.put("MIPFifth", detectionInfo.getMIPFifth());
            data.put("DIPFirst", detectionInfo.getDIPFirst());
            data.put("DIPThird", detectionInfo.getDIPThird());
            data.put("DIPFifth", detectionInfo.getDIPFifth());
            data.put("Radius", detectionInfo.getRadius());
            data.put("Ulna", detectionInfo.getUlna());

            response.put("code", 0);
            response.put("message", "影像检测信息获取成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "获取检测信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/tw3CRusResult")
    public ResponseEntity<Map<String, Object>> getTw3CRusResult(
            @RequestParam(value = "id", required = false) Long tcrResultId) {
        Map<String, Object> response = new HashMap<>();

        if (tcrResultId == null) {
            response.put("code", 1);
            response.put("message", "TW3-C RUS ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Tw3CRusResult result = tw3CRusResultRepository.findById(tcrResultId);
            if (result == null) {
                response.put("code", 3);
                response.put("message", "未找到TW3-C RUS信息");
                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("TCRResultID", result.getTcrResultId());
            data.put("MCPFirst", result.getMcpFirst());
            data.put("MCPThird", result.getMcpThird());
            data.put("MCPFifth", result.getMcpFifth());
            data.put("PIPFirst", result.getPipFirst());
            data.put("PIPThird", result.getPipThird());
            data.put("PIPFifth", result.getPipFifth());
            data.put("MIPThird", result.getMipThird());
            data.put("MIPFifth", result.getMipFifth());
            data.put("DIPFirst", result.getDipFirst());
            data.put("DIPThird", result.getDipThird());
            data.put("DIPFifth", result.getDipFifth());
            data.put("Radius", result.getRadius());
            data.put("Ulna", result.getUlna());
            data.put("Total", result.getTotal());
            data.put("BoneAge", result.getBoneAge());
            data.put("CreateTime", result.getCreateTime());
            data.put("UpdateTime", result.getUpdateTime());

            response.put("code", 0);
            response.put("message", "TW3-C RUS数据获取成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "获取TW3-C RUS信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/rusChnResult")
    public ResponseEntity<Map<String, Object>> getRusChnResult(
            @RequestParam(value = "id", required = false) Long rcResultId) {
        Map<String, Object> response = new HashMap<>();

        if (rcResultId == null) {
            response.put("code", 1);
            response.put("message", "RUS-CHN ID不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            RusChnResult result = rusChnResultRepository.findById(rcResultId);
            if (result == null) {
                response.put("code", 3);
                response.put("message", "未找到RUS-CHN信息");
                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("RCResultID", result.getRcResultId());
            data.put("MCPFirst", result.getMcpFirst());
            data.put("MCPThird", result.getMcpThird());
            data.put("MCPFifth", result.getMcpFifth());
            data.put("PIPFirst", result.getPipFirst());
            data.put("PIPThird", result.getPipThird());
            data.put("PIPFifth", result.getPipFifth());
            data.put("MIPThird", result.getMipThird());
            data.put("MIPFifth", result.getMipFifth());
            data.put("DIPFirst", result.getDipFirst());
            data.put("DIPThird", result.getDipThird());
            data.put("DIPFifth", result.getDipFifth());
            data.put("Radius", result.getRadius());
            data.put("Ulna", result.getUlna());
            data.put("Total", result.getTotal());
            data.put("BoneAge", result.getBoneAge());
            data.put("CreateTime", result.getCreateTime());
            data.put("UpdateTime", result.getUpdateTime());

            response.put("code", 0);
            response.put("message", "RUS-CHN信息获取成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 2);
            response.put("message", "获取RUS-CHN信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}