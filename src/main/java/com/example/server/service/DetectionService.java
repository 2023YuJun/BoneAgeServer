package com.example.server.service;

import ai.onnxruntime.*;
import ai.onnxruntime.OrtEnvironment;
import org.apache.commons.imaging.Imaging;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

public class DetectionService {
    // 模型配置常量
    private static final int INPUT_SIZE = 640;
    private static final float CONF_THRESHOLD = 0.5f;
    private static final String[] CLASS_NAMES = {
            "DistalPhalanx", "MCP", "MCPFirst", "MiddlePhalanx",
            "ProximalPhalanx", "Radius", "Ulna"
    };
    private static final int NUM_CLASSES = CLASS_NAMES.length;

    // ONNX运行时组件
    private final OrtEnvironment env;
    private final OrtSession session;

    public DetectionService() throws Exception {
        env = OrtEnvironment.getEnvironment();
        try (InputStream modelStream = getClass().getResourceAsStream("/model/detection.onnx")) {
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            opts.setInterOpNumThreads(1);
            opts.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
            session = env.createSession(modelStream.readAllBytes(), opts);
        }
    }

    public Map<String, Object> detect(String imagePath) throws Exception {
        // 读取原始图像
        BufferedImage originImage = Imaging.getBufferedImage(new File(imagePath));
        int originWidth = originImage.getWidth();
        int originHeight = originImage.getHeight();

        // 预处理图像
        ProcessedImage processed = processImage(originImage);

        // 准备模型输入
        float[][][][] inputData = prepareInput(processed.paddedImage);
        OnnxTensor inputTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(flattenArray(inputData)),
                new long[]{1, 3, INPUT_SIZE, INPUT_SIZE});

        // 执行推理
        try (OrtSession.Result results = session.run(Collections.singletonMap("images", inputTensor))) {
            // 解析输出 [1, 11, 8400] -> [batch, features, num_predictions]
            OnnxValue outputValue = results.get("output0").orElseThrow(() -> new RuntimeException("模型输出节点不存在"));
            float[][][] output = (float[][][]) outputValue.getValue();

            // 后处理
            List<DetectionResult> detections = processPredictions(output, processed);

            // 验证结果数量
            if (detections.size() != 21) {
                return Collections.singletonMap("error",
                        "检测到 "+detections.size()+" 个结果，预期应为21个");
            }

            // 打包最终结果
            return packageResults(detections, originImage);
        }
    }

    // 图像预处理（保持宽高比填充）
    private ProcessedImage processImage(BufferedImage originImage) {
        // 计算缩放比例
        float scale = Math.min(
                (float) INPUT_SIZE / originImage.getWidth(),
                (float) INPUT_SIZE / originImage.getHeight()
        );

        // 缩放图像
        int scaledWidth = (int)(originImage.getWidth() * scale);
        int scaledHeight = (int)(originImage.getHeight() * scale);
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_3BYTE_BGR);
        scaledImage.getGraphics().drawImage(
                originImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH),
                0, 0, null);

        // 创建填充图像
        BufferedImage paddedImage = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = paddedImage.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, INPUT_SIZE, INPUT_SIZE);
        int xOffset = (INPUT_SIZE - scaledWidth) / 2;
        int yOffset = (INPUT_SIZE - scaledHeight) / 2;
        g.drawImage(scaledImage, xOffset, yOffset, null);
        g.dispose();

        return new ProcessedImage(
                paddedImage, scaledWidth, scaledHeight,
                xOffset, yOffset, scale,
                originImage.getWidth(), originImage.getHeight()
        );
    }

    // 准备输入张量（BGR归一化）
    private float[][][][] prepareInput(BufferedImage image) {
        float[][][][] inputArray = new float[1][3][INPUT_SIZE][INPUT_SIZE];
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = image.getRGB(x, y);
                // 通道顺序：BGR
                inputArray[0][0][y][x] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                inputArray[0][1][y][x] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                inputArray[0][2][y][x] = (pixel & 0xFF) / 255.0f;         // B
            }
        }
        return inputArray;
    }

    // 处理模型预测结果
    private List<DetectionResult> processPredictions(float[][][] output, ProcessedImage processed) {
        List<DetectionResult> results = new ArrayList<>();
        int numFeatures = output[0].length; // 11
        int numPredictions = output[0][0].length; // 8400
        System.out.println("Total predictions: " + numPredictions);
        for (int i = 0; i < numPredictions; i++) {
            // 提取每个预测的11个参数
            float[] pred = new float[numFeatures];
            for (int j = 0; j < numFeatures; j++) {
                pred[j] = output[0][j][i]; // 维度顺序
            }

            // 坐标转换（验证是否超出范围）
            float xCenter = pred[0];
            float yCenter = pred[1];
            float width = pred[2];
            float height = pred[3];

            // 解析类别
            float[] classProbs = Arrays.copyOfRange(pred, 4, 4 + NUM_CLASSES);
            // 找到最大置信度的类别
            int classId = argMax(classProbs);
            float maxConfidence = classProbs[classId];

            // 跳过低置信度的检测框
            if (maxConfidence < CONF_THRESHOLD) continue;

            System.out.println("Class probabilities: " + Arrays.toString(classProbs));
            System.out.println("Selected class ID: " + CLASS_NAMES[classId]);


            // 转换为原始图像坐标
            float x1 = (xCenter - width / 2 - processed.xOffset) / processed.scale;
            float y1 = (yCenter - height / 2 - processed.yOffset) / processed.scale;
            float x2 = (xCenter + width / 2 - processed.xOffset) / processed.scale;
            float y2 = (yCenter + height / 2 - processed.yOffset) / processed.scale;

            // 边界约束
            x1 = clamp(x1, 0, processed.originWidth);
            y1 = clamp(y1, 0, processed.originHeight);
            x2 = clamp(x2, 0, processed.originWidth);
            y2 = clamp(y2, 0, processed.originHeight);

            // 跳过无效区域
            if (x2 <= x1 || y2 <= y1) {
                System.out.println("Invalid bbox: " + x1 + "," + y1 + " " + x2 + "," + y2);
                continue;
            }

            results.add(new DetectionResult(
                    CLASS_NAMES[classId],
                    new float[]{(x1 + x2)/2, (y1 + y2)/2},
                    new float[]{x1, y1, x2, y2},
                    maxConfidence
            ));
        }
        return applyNMS(results);
    }

    // 非极大值抑制（按类别分组处理）
    private List<DetectionResult> applyNMS(List<DetectionResult> detections) {
        Map<String, List<DetectionResult>> classGroups = new HashMap<>();
        for (DetectionResult det : detections) {
            classGroups.computeIfAbsent(det.className, k -> new ArrayList<>()).add(det);
        }

        List<DetectionResult> filtered = new ArrayList<>();
        for (List<DetectionResult> group : classGroups.values()) {
            // 按置信度降序排序（需在 DetectionResult 中添加 confidence 字段）
            group.sort((a, b) -> Float.compare(b.confidence, a.confidence));

            // 遍历并过滤重叠框
            for (int i = 0; i < group.size(); i++) {
                DetectionResult current = group.get(i);
                if (current == null) continue;
                filtered.add(current);
                for (int j = i + 1; j < group.size(); j++) {
                    DetectionResult other = group.get(j);
                    if (other != null && calculateIoU(current.bbox, other.bbox) > 0.5) {
                        group.set(j, null); // 标记为已过滤
                    }
                }
            }
        }
        return filtered;
    }

    // 计算两个框的IoU
    private float calculateIoU(float[] bbox1, float[] bbox2) {
        float x1 = Math.max(bbox1[0], bbox2[0]);
        float y1 = Math.max(bbox1[1], bbox2[1]);
        float x2 = Math.min(bbox1[2], bbox2[2]);
        float y2 = Math.min(bbox1[3], bbox2[3]);
        float intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float area1 = (bbox1[2] - bbox1[0]) * (bbox1[3] - bbox1[1]);
        float area2 = (bbox2[2] - bbox2[0]) * (bbox2[3] - bbox2[1]);
        return intersection / (area1 + area2 - intersection);
    }


    // 工具方法：查找最大值索引
    private int argMax(float[] array) {
        int maxIdx = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    // 工具方法：数值范围约束
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // 平铺多维数组
    private static float[] flattenArray(float[][][][] array) {
        float[] flat = new float[1 * 3 * INPUT_SIZE * INPUT_SIZE];
        int idx = 0;
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < INPUT_SIZE; y++) {
                for (int x = 0; x < INPUT_SIZE; x++) {
                    flat[idx++] = array[0][c][y][x];
                }
            }
        }
        return flat;
    }

    // 打包最终结果
    private Map<String, Object> packageResults(List<DetectionResult> detections, BufferedImage originImage) {
        Map<String, List<Map<String, Object>>> resultMap = new LinkedHashMap<>();

        for (DetectionResult det : detections) {
            // 裁剪检测区域
            int x1 = (int) det.bbox[0];
            int y1 = (int) det.bbox[1];
            int width = (int) (det.bbox[2] - x1);
            int height = (int) (det.bbox[3] - y1);

            // 边界二次验证
            if (x1 < 0 || y1 < 0 || width <= 0 || height <= 0) continue;
            if (x1 + width > originImage.getWidth()) width = originImage.getWidth() - x1;
            if (y1 + height > originImage.getHeight()) height = originImage.getHeight() - y1;

            BufferedImage crop = originImage.getSubimage(x1, y1, width, height);

            // 构建结果项
            Map<String, Object> item = new HashMap<>();
            item.put("center", det.center);
            item.put("bbox", det.bbox);
            item.put("crop", crop);

            resultMap.computeIfAbsent(det.className, k -> new ArrayList<>()).add(item);
        }

        return Collections.singletonMap("results", resultMap);
    }

    // 辅助类：预处理后的图像信息
    private static class ProcessedImage {
        final BufferedImage paddedImage;
        final int scaledWidth, scaledHeight;
        final int xOffset, yOffset;
        final float scale;
        final int originWidth, originHeight;

        ProcessedImage(BufferedImage paddedImage,
                       int scaledWidth, int scaledHeight,
                       int xOffset, int yOffset, float scale,
                       int originWidth, int originHeight) {
            this.paddedImage = paddedImage;
            this.scaledWidth = scaledWidth;
            this.scaledHeight = scaledHeight;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.scale = scale;
            this.originWidth = originWidth;
            this.originHeight = originHeight;
            System.out.printf("Scale: %.2f, OffsetX: %d, OffsetY: %d%n",
                    scale, xOffset, yOffset);
        }
    }

    // 检测结果类
    private static class DetectionResult {
        final String className;
        final float[] center;
        final float[] bbox;
        final float confidence;

        DetectionResult(String className, float[] center, float[] bbox, float confidence) {
            this.className = className;
            this.center = center;
            this.bbox = bbox;
            this.confidence = confidence;
        }
    }

    // 测试方法
    public static void main(String[] args) {
        try {
            DetectionService detector = new DetectionService();

            // 获取测试图像路径
            String imagePath = Objects.requireNonNull(
                    DetectionService.class.getClassLoader().getResource("static/test_handX_R.png")
            ).getFile();

            Map<String, Object> results = detector.detect(imagePath);

            if (results.containsKey("error")) {
                System.err.println("检测错误: " + results.get("error"));
                return;
            }

            // 打印结果
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> detections =
                    (Map<String, List<Map<String, Object>>>) results.get("results");

            System.out.println("=== 检测结果 ===");
            detections.forEach((className, items) -> {
                System.out.printf("%s (%d 个):\n", className, items.size());
                items.forEach(item -> {
                    float[] center = (float[]) item.get("center");
                    float[] bbox = (float[]) item.get("bbox");
                    System.out.printf("\t中心点: (%.1f, %.1f)\n", center[0], center[1]);
                    System.out.printf("\t区域: %.1f×%.1f\n",
                            bbox[2] - bbox[0], bbox[3] - bbox[1]);
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}