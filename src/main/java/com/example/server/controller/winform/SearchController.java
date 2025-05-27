package com.example.server.controller.winform;

import com.example.server.service.DICOMService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/winform")
public class SearchController {
    private static final String HAND_SERIES_DESCRIPTION = "Hand";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    private final DICOMService dicomService;

    public SearchController(DICOMService dicomService) {
        this.dicomService = dicomService;
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

            List<Attributes> studies = dicomService.studySearch(patientID);
            if (studies.isEmpty()) {
                return completedErrorResponse(HttpStatus.NOT_FOUND, "未找到Study信息");
            }

            List<String> pngPaths = processAllStudies(patientID, studies);
            if (pngPaths.isEmpty()) {
                return completedErrorResponse(HttpStatus.NOT_FOUND, "未找到符合条件的Hand系列图像");
            }



            return CompletableFuture.completedFuture(
                    ResponseEntity.ok(Map.of(MESSAGE_KEY, "推理成功"))
            );
        } catch (Exception e) {
            return CompletableFuture.completedFuture(handleException(e));
        }
    }

    /**
     * 处理所有Study层级的图像
     */
    private List<String> processAllStudies(String patientID, List<Attributes> studies) {
        List<String> pngPaths = new ArrayList<>();

        for (Attributes study : studies) {
            String studyUID = study.getString(Tag.StudyInstanceUID);
            String studyDate = study.getString(Tag.StudyDate, "");

            List<Attributes> seriesList = dicomService.seriesSearch(studyUID);
            List<Attributes> handSeries = filterHandSeries(seriesList);

            for (Attributes series : handSeries) {
                String seriesUID = series.getString(Tag.SeriesInstanceUID);
                List<Attributes> images = dicomService.imageSearch(seriesUID);

                for (Attributes image : images) {
                    String sopUID = image.getString(Tag.SOPInstanceUID);
                    String downloadUrl = dicomService.buildDownloadUrl(
                            studyUID, seriesUID, sopUID, studyDate, patientID
                    );
                    pngPaths.add(dicomService.downloadAndConvertToPng(downloadUrl, sopUID));
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