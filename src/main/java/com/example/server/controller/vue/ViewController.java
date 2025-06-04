package com.example.server.controller.vue;

import com.example.server.model.*;
import com.example.server.repository.*;
import com.example.server.service.DICOMService;
import com.example.server.Utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @Autowired
    private DICOMService dicomService;

    @Async("controllerTaskExecutor")
    @GetMapping("/patientInfo")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPatientInfo(
            @RequestParam(value = "id", required = false) String patientId) {
        return CompletableFuture.supplyAsync(() -> {
            if (patientId == null || patientId.isEmpty()) {
                return ApiResponse.error(1, "患者ID不能为空");
            }

            try {
                List<PatientInfo> patients = patientInfoRepository.findByPatientID(patientId);

                // 为每个患者信息动态添加 downloadUrl
                for (PatientInfo patient : patients) {
                    String studyUID = patient.getStudyInstanceUID();
                    String seriesUID = patient.getSeriesInstanceUID();
                    String sopUID = patient.getSOPInstanceUID();
                    LocalDate studyDate = patient.getStudyDate();

                    if (studyUID != null && seriesUID != null && sopUID != null && studyDate != null) {
                        String downloadUrl = dicomService.buildDownloadUrl(
                                studyUID,
                                seriesUID,
                                sopUID,
                                studyDate.toString(),
                                patient.getPatientID()
                        );
                        patient.setDownLoadUrl(downloadUrl);
                    } else {
                        patient.setDownLoadUrl(null);
                        System.err.println("无法生成下载链接: 缺少必要字段");
                    }
                }

                Map<String, Object> data = new HashMap<>();
                data.put("totalRecords", patients.size());
                data.put("patients", patients);

                return ApiResponse.success("患者影像信息获取成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "获取患者信息失败: " + e.getMessage());
            }
        });

    }

    @Async("controllerTaskExecutor")
    @GetMapping("/inferenceInfo")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getInferenceInfo(
            @RequestParam(value = "id", required = false) Long inferenceId) {
        return CompletableFuture.supplyAsync(() -> {
            if (inferenceId == null) {
                return ApiResponse.error(1, "推理ID不能为空");
            }

            try {
                InferenceInfo inferenceInfo = inferenceInfoRepository.findById(inferenceId);
                if (inferenceInfo == null) {
                    return ApiResponse.notFound(3, "未找到推理信息");
                }

                Map<String, Object> data = new HashMap<>();
                data.put("InferenceID", inferenceInfo.getInferenceID());
                data.put("DetectionID", inferenceInfo.getDetectionID());
                data.put("RCResultID", inferenceInfo.getRCResultID());
                data.put("TCRResultID", inferenceInfo.getTCRResultID());
                data.put("TCCResultID", inferenceInfo.getTCCResultID());

                return ApiResponse.success("患者推理信息获取成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "获取推理信息失败: " + e.getMessage());
            }
        });

    }

    @Async("controllerTaskExecutor")
    @GetMapping("/detectionInfo")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getDetectionInfo(
            @RequestParam(value = "id", required = false) Long detectionId) {
        return CompletableFuture.supplyAsync(() -> {
            if (detectionId == null) {
                return ApiResponse.error(1, "检测ID不能为空");
            }

            try {
                DetectionInfo detectionInfo = detectionInfoRepository.findById(detectionId);
                if (detectionInfo == null) {
                    return ApiResponse.notFound(3, "未找到检测信息");
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

                return ApiResponse.success("影像检测信息获取成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "获取检测信息失败: " + e.getMessage());
            }
        });

    }

    @Async("controllerTaskExecutor")
    @GetMapping("/tw3CRusResult")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getTw3CRusResult(
            @RequestParam(value = "id", required = false) Long tcrResultId) {
        return CompletableFuture.supplyAsync(() -> {
            if (tcrResultId == null) {
                return ApiResponse.error(1, "TW3-C RUS ID不能为空");
            }

            try {
                Tw3CRusResult result = tw3CRusResultRepository.findById(tcrResultId);
                if (result == null) {
                    return ApiResponse.notFound(3, "未找到TW3-C RUS信息");
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

                return ApiResponse.success("TW3-C RUS数据获取成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "获取TW3-C RUS信息失败: " + e.getMessage());
            }
        });

    }

    @Async("controllerTaskExecutor")
    @GetMapping("/rusChnResult")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getRusChnResult(
            @RequestParam(value = "id", required = false) Long rcResultId) {
        return CompletableFuture.supplyAsync(() -> {
            if (rcResultId == null) {
                return ApiResponse.error(1, "RUS-CHN ID不能为空");
            }

            try {
                RusChnResult result = rusChnResultRepository.findById(rcResultId);
                if (result == null) {
                    return ApiResponse.notFound(3, "未找到RUS-CHN信息");
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

                return ApiResponse.success("RUS-CHN信息获取成功", data);
            } catch (Exception e) {
                return ApiResponse.serverError(2, "获取RUS-CHN信息失败: " + e.getMessage());
            }
        });

    }
}