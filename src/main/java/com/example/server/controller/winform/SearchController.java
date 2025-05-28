package com.example.server.controller.winform;

import com.example.server.Utils.RCScoreUtils;
import com.example.server.Utils.TRScoreUtils;
import com.example.server.model.PatientInfo;
import com.example.server.repository.PatientInfoRepository;
import com.example.server.service.DICOMService;
import com.example.server.service.DetectionService;
import com.example.server.service.ClassifyService;
import org.apache.commons.imaging.Imaging;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/winform")
public class SearchController {
    private static final String HAND_SERIES_DESCRIPTION = "Hand";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    private final DICOMService dicomService;
    private final DetectionService detectionService;
    private final ClassifyService classifyService;
    private final PatientInfoRepository patientInfoRepository;

    public SearchController(
            DICOMService dicomService,
            DetectionService detectionService,
            ClassifyService classifyService,
            PatientInfoRepository patientInfoRepository
    ) {
        this.dicomService = dicomService;
        this.detectionService = detectionService;
        this.classifyService = classifyService;
        this.patientInfoRepository = patientInfoRepository;
    }

    /**
     * 处理搜索请求（请求级别异步，内部同步处理）
     */
    @GetMapping("/search")
    @Async
    public CompletableFuture<ResponseEntity<?>> handleSearchRequest(
            @RequestParam("patientID") String patientID
    ) {
        try {
            List<Attributes> patients = dicomService.patientSearch(patientID);
            if (patients.isEmpty()) {
                return completedErrorResponse(HttpStatus.NOT_FOUND, "未找到患者ID: " + patientID);
            }

            // 获取患者元数据
            Attributes patientAttr = patients.getFirst();
            String birthDateStr = patientAttr.getString(Tag.PatientBirthDate, "");
            String sex = patientAttr.getString(Tag.PatientSex, "U");

            List<Attributes> studies = dicomService.studySearch(patientID);
            if (studies.isEmpty()) {
                return completedErrorResponse(HttpStatus.NOT_FOUND, "未找到Study信息");
            }

            List<String> pngPaths = processAllStudies(patientID, birthDateStr, sex, studies);
            if (pngPaths.isEmpty()) {
                return completedErrorResponse(HttpStatus.NOT_FOUND, "未找到符合条件的Hand系列图像");
            }

            boolean isMale = sex.equalsIgnoreCase("M");

            return processPngPaths(pngPaths, isMale);

        } catch (Exception e) {
            return CompletableFuture.completedFuture(handleException(e));
        }
    }

    /**
     * 处理所有Study层级的图像
     */
    private List<String> processAllStudies(
            String patientID,
            String birthDateStr,
            String sex,
            List<Attributes> studies
    ) {
        List<String> pngPaths = new ArrayList<>();
        LocalDate birthDate = parseDicomDate(birthDateStr);

        // 获取数据库中该患者最新的Study日期
        LocalDate latestStudyDateInDB = patientInfoRepository.findLatestStudyDateByPatient(patientID);

        for (Attributes study : studies) {
            String studyDateStr = study.getString(Tag.StudyDate, "");
            LocalDate studyDate = parseDicomDate(studyDateStr);

            if (latestStudyDateInDB != null &&
                    !studyDate.isAfter(latestStudyDateInDB)) {
                continue;
            }

            String studyUID = study.getString(Tag.StudyInstanceUID);
            List<Attributes> seriesList = dicomService.seriesSearch(studyUID);
            List<Attributes> handSeries = filterHandSeries(seriesList);

            for (Attributes series : handSeries) {
                String seriesUID = series.getString(Tag.SeriesInstanceUID);
                List<Attributes> images = dicomService.imageSearch(seriesUID);

                for (Attributes image : images) {
                    String sopUID = image.getString(Tag.SOPInstanceUID);
                    String downloadUrl = dicomService.buildDownloadUrl(
                            studyUID, seriesUID, sopUID, studyDateStr, patientID
                    );

                    // 下载并转换DICOM文件
                    String pngPath = dicomService.downloadAndConvertToPng(downloadUrl, sopUID);
                    if (pngPath != null) {
                        pngPaths.add(pngPath);

                        // 保存患者信息到数据库
                        savePatientInfo(
                                patientID, birthDate, sex,
                                studyUID, seriesUID, sopUID, studyDate
                        );
                    }
                }
            }
        }
        return pngPaths;
    }

    /**
     * 过滤Hand系列
     */
    private List<Attributes> filterHandSeries(List<Attributes> seriesList) {
        return seriesList.stream()
                .filter(series -> HAND_SERIES_DESCRIPTION.equalsIgnoreCase(
                        series.getString(Tag.SeriesDescription, "")))
                .collect(Collectors.toList());
    }

    /**
     * 图像处理流程
     */
    private CompletableFuture<ResponseEntity<?>> processPngPaths(List<String> pngPaths, boolean isMale) throws Exception {
        for (String pngPath : pngPaths) {
            BufferedImage originImage = Imaging.getBufferedImage(new File(pngPath));
            // 1. 调用检测服务
            Map<String, Object> detectionResult = detectionService.detect(originImage);
            if (detectionResult.containsKey(ERROR_KEY)) {
                return completedErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "检测失败: " + detectionResult.get(ERROR_KEY));
            }

            // 2. 调用分类服务
            Map<String, Integer> classifyResult;
            try {
                classifyResult = classifyService.classify(detectionResult, originImage);
            } catch (Exception e) {
                return completedErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "分类失败: " + e.getMessage());
            }

            // 3. 调用两个骨龄计算工具
            try {
                RCScoreUtils.calculateBoneAge(isMale, classifyResult);
                TRScoreUtils.calculateBoneAge(isMale, classifyResult);
            } catch (IllegalArgumentException e) {
                return completedErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
            } catch (Exception e) {
                return completedErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "骨龄计算失败: " + e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(
                ResponseEntity.ok(Map.of(MESSAGE_KEY, "推理成功"))
        );
    }

    /**
     * 解析DICOM日期格式（如 "20230815" -> LocalDate）
     */
    private LocalDate parseDicomDate(String dicomDate) {
        if (dicomDate == null || dicomDate.length() < 8) {
            System.err.println("无效的DICOM日期格式: " + dicomDate);
            return null;
        }
        try {
            return LocalDate.parse(
                    dicomDate.substring(0, 4) + "-" +
                            dicomDate.substring(4, 6) + "-" +
                            dicomDate.substring(6, 8)
            );
        } catch (Exception e) {
            System.err.println("日期解析失败: " + dicomDate);
            return null;
        }
    }

    /**
     * 保存患者信息到数据库
     */
    private void savePatientInfo(
            String patientID,
            LocalDate birthDate,
            String sex,
            String studyUID,
            String seriesUID,
            String sopUID,
            LocalDate studyDate
    ) {
        if (studyDate == null) {
            System.err.println("跳过无效Study日期的记录");
            return;
        }
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPatientID(patientID);
        patientInfo.setBrithDate(birthDate);
        patientInfo.setSex(sex);
        patientInfo.setStudyInstanceUID(studyUID);
        patientInfo.setSeriesInstanceUID(seriesUID);
        patientInfo.setSOPInstanceUID(sopUID);
        patientInfo.setStudyDate(studyDate);
        patientInfo.setInferenceID(null);

        try {
            patientInfoRepository.save(patientInfo);
        } catch (Exception e) {
            System.err.println("保存患者信息失败: " + e.getMessage());
        }
    }

    /**
     * 快速构建错误响应
     */
    private CompletableFuture<ResponseEntity<?>> completedErrorResponse(HttpStatus status, String message) {
        return CompletableFuture.completedFuture(
                ResponseEntity.status(status).body(Map.of(ERROR_KEY, message))
        );
    }

    /**
     * 统一异常处理
     */
    private ResponseEntity<?> handleException(Exception ex) {
        Throwable rootCause = ex.getCause() != null ? ex.getCause() : ex;
        String errorMsg = "服务器错误: " + rootCause.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(ERROR_KEY, errorMsg));
    }
}