package com.example.server.service;

import com.example.server.model.InferenceInfo;
import com.example.server.model.PatientInfo;
import com.example.server.repository.InferenceInfoRepository;
import com.example.server.repository.PatientInfoRepository;
import org.apache.commons.imaging.Imaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageProcessingService {

    private final DetectionService detectionService;
    private final ClassifyService classifyService;
    private final BoneAgeService boneAgeService;
    private final PatientInfoRepository patientInfoRepository;
    private final InferenceInfoRepository inferenceInfoRepository;

    @Autowired
    public ImageProcessingService(
            DetectionService detectionService,
            ClassifyService classifyService,
            BoneAgeService boneAgeService,
            PatientInfoRepository patientInfoRepository,
            InferenceInfoRepository inferenceInfoRepository
    ) {
        this.detectionService = detectionService;
        this.classifyService = classifyService;
        this.boneAgeService = boneAgeService;
        this.patientInfoRepository = patientInfoRepository;
        this.inferenceInfoRepository = inferenceInfoRepository;
    }

    @Async("imageProcessingExecutor")
    public CompletableFuture<Void> processImageAsync(String pngPath, boolean isMale) {
        try {
            BufferedImage originImage = Imaging.getBufferedImage(new File(pngPath));

            // 1. 调用检测服务
            Map<String, Object> detectionResult = detectionService.detect(originImage);
            if (detectionResult.containsKey("error")) {
                System.err.println("检测失败: " + detectionResult.get("error"));
                return CompletableFuture.completedFuture(null);
            }

            // 2. 调用分类服务
            Map<String, Integer> classifyResult = classifyService.classify(detectionResult, originImage);

            // 3. 调用骨龄计算工具
            Map<String, Object> rusResult = boneAgeService.processRusChn(isMale, classifyResult);
            Map<String, Object> tw3Result = boneAgeService.processTw3CRus(isMale, classifyResult);

            // 4. 保存推理信息
            saveInferenceInfo(detectionResult, rusResult, tw3Result, pngPath);

            System.out.println("成功处理图像: " + pngPath);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            System.err.println("处理图像时发生异常: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    private void saveInferenceInfo(
            Map<String, Object> detectionResult,
            Map<String, Object> rusResult,
            Map<String, Object> tw3Result,
            String pngPath
    ) {
        InferenceInfo inferenceInfo = new InferenceInfo();

        // 安全处理 DetectionID
        Object detectionIdObj = detectionResult.get("detectionId");
        if (detectionIdObj instanceof Number) {
            inferenceInfo.setDetectionID(((Number) detectionIdObj).longValue());
        } else {
            System.err.println("检测ID类型错误: " + detectionIdObj);
            return;
        }

        // 安全处理 RCResultID
        Object rcResultIdObj = rusResult.get("rcResultId");
        if (rcResultIdObj instanceof Number) {
            inferenceInfo.setRCResultID(((Number) rcResultIdObj).longValue());
        } else {
            System.err.println("RUS结果ID类型错误: " + rcResultIdObj);
            return;
        }

        // 安全处理 TCRResultID
        Object tcrResultIdObj = tw3Result.get("tcrResultId");
        if (tcrResultIdObj instanceof Number) {
            inferenceInfo.setTCRResultID(((Number) tcrResultIdObj).longValue());
        } else {
            System.err.println("TW3结果ID类型错误: " + tcrResultIdObj);
            return;
        }

        inferenceInfo.setTCCResultID(null);

        // 保存推理信息并获取InferenceID
        Long inferenceID = inferenceInfoRepository.save(inferenceInfo);

        // 更新患者信息的InferenceID
        String sopUID = extractSopUIDFromPath(pngPath);
        PatientInfo patientInfo = patientInfoRepository.findBySOPInstanceUID(sopUID);
        if (patientInfo != null && patientInfo.getPID() != null) {
            patientInfoRepository.updateInferenceID(patientInfo.getPID(), inferenceID);
        } else {
            System.err.println("未找到SOPInstanceUID为 " + sopUID + " 的患者记录");
        }
    }

    private String extractSopUIDFromPath(String pngPath) {
        return new File(pngPath).getName().replace(".png", "");
    }
}