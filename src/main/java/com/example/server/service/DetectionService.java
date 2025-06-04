package com.example.server.service;

import ai.onnxruntime.*;
import ai.onnxruntime.OrtEnvironment;
import com.example.server.model.DetectionInfo;
import com.example.server.repository.DetectionInfoRepository;
import org.apache.commons.imaging.Imaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DetectionService {
    // 模型配置常量
    private BufferedImage currentOriginImage;
    private static final int INPUT_SIZE = 640;
    private static final float CONF_THRESHOLD = 0.5f;
    private static final String[] CLASS_NAMES = {
            "DistalPhalanx", "MCP", "MCPFirst", "MiddlePhalanx",
            "ProximalPhalanx", "Radius", "Ulna"
    };
    private static final int NUM_CLASSES = CLASS_NAMES.length;
    // 重命名规则配置
    private static final Map<String, Map<Boolean, Map<Integer, String>>> RENAME_RULES = new HashMap<>();
    static {
        // DistalPhalanx
        Map<Boolean, Map<Integer, String>> distalRules = new HashMap<>();
        distalRules.put(true, Map.of(0, "DIPFifth", 2, "DIPThird", 4, "DIPFirst"));
        distalRules.put(false, Map.of(0, "DIPFirst",2, "DIPThird", 4, "DIPFifth"));
        RENAME_RULES.put("DistalPhalanx", distalRules);

        // MCP
        Map<Boolean, Map<Integer, String>> mcpRules = new HashMap<>();
        mcpRules.put(true, Map.of(0, "MCPFifth", 2, "MCPThird"));
        mcpRules.put(false, Map.of(1, "MCPThird", 3, "MCPFifth"));
        RENAME_RULES.put("MCP", mcpRules);

        // MiddlePhalanx
        Map<Boolean, Map<Integer, String>> middleRules = new HashMap<>();
        middleRules.put(true, Map.of(0, "MIPFifth", 2, "MIPThird"));
        middleRules.put(false, Map.of(1, "MIPThird", 3, "MIPFifth"));
        RENAME_RULES.put("MiddlePhalanx", middleRules);

        // ProximalPhalanx
        Map<Boolean, Map<Integer, String>> proximalRules = new HashMap<>();
        proximalRules.put(true, Map.of(0, "PIPFifth", 2, "PIPThird", 4, "PIPFirst"));
        proximalRules.put(false, Map.of(0, "PIPFirst", 2, "PIPThird", 4, "PIPFifth"));
        RENAME_RULES.put("ProximalPhalanx", proximalRules);
    }

    // ONNX运行时组件
    private final OrtEnvironment env;
    private final OrtSession session;

    // 数据库表
    private final DetectionInfoRepository detectionRepo;

    @Autowired
    public DetectionService(DetectionInfoRepository detectionRepo) throws Exception {
        this.detectionRepo = detectionRepo;
        env = OrtEnvironment.getEnvironment();
        try (InputStream modelStream = getClass().getResourceAsStream("/model/detection.onnx")) {
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            opts.setInterOpNumThreads(1);
            opts.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
            session = env.createSession(modelStream.readAllBytes(), opts);
        }
    }

    public Map<String, Object> detect(BufferedImage originImage) throws Exception {
        // 读取原始图像
        currentOriginImage = originImage;

        // 预处理图像
        ProcessedImage processed = processImage(currentOriginImage);

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
                saveImageWithDetections(originImage, detections);
                return Collections.singletonMap("error",
                        "检测到 "+detections.size()+" 个结果，预期应为21个");
            }

            // 打包最终结果
            Map<String, Object> resultMap = new HashMap<>(packageResults(detections));
            Long detectionId = saveDetectionData(detections);
            resultMap.put("detectionId", detectionId);
            return resultMap;
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

    // 工具方法：判断左右手
    private boolean isLeftHand(List<DetectionResult> detections) {
        DetectionResult ulna = detections.stream()
                .filter(d -> d.className.equals("Ulna"))
                .findFirst()
                .orElseThrow();

        DetectionResult radius = detections.stream()
                .filter(d -> d.className.equals("Radius"))
                .findFirst()
                .orElseThrow();

        return ulna.center[0] < radius.center[0];
    }

    // 打包最终结果
    private Map<String, Object> packageResults(List<DetectionResult> detections) {
        boolean isLeftHand = isLeftHand(detections);
        // 遍历所有需要重命名的骨骼类型
        Arrays.asList(
                "DistalPhalanx",
                "MCP",
                "MiddlePhalanx",
                "ProximalPhalanx"
        ).forEach(boneType ->
                processBoneType(detections, boneType, isLeftHand)
        );

        // 直接处理所有检测结果
        Map<String, List<Map<String, Object>>> resultMap = detections.stream()
                .collect(Collectors.groupingBy(
                        d -> d.className,
                        LinkedHashMap::new,
                        Collectors.mapping(this::convertToMap, Collectors.toList())
                ));

        return Collections.singletonMap("results", resultMap);
    }

    private void processBoneType(
            List<DetectionResult> detections,
            String boneType,
            boolean isLeftHand
    ) {
        // 1. 获取原始检测结果并过滤
        List<DetectionResult> originItems = detections.stream()
                .filter(d -> d.className.equals(boneType))
                .collect(Collectors.toList());

        // 2. 按中心点 X 坐标升序排序（从左到右）
        originItems.sort(Comparator.comparingDouble(d -> d.center[0]));

        // 3. 获取当前骨骼类型和手性对应的重命名规则
        Map<Integer, String> renameMap = RENAME_RULES.get(boneType).get(isLeftHand);

        // 4. 收集需要保留的已重命名对象
        List<DetectionResult> renamedItems = new ArrayList<>();
        for (int index = 0; index < originItems.size(); index++) {
            if (renameMap.containsKey(index)) {
                DetectionResult origin = originItems.get(index);
                origin.className = renameMap.get(index); // 修改类名
                renamedItems.add(origin);
            }
        }

        // 5. 从原始列表中移除所有该类型的原始对象
        detections.removeAll(originItems);

        // 6. 将重命名后的对象添加回列表
        detections.addAll(renamedItems);
    }

    private Map<String, Object> convertToMap(DetectionResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("bbox", result.bbox);
        return map;
    }

    private Long saveDetectionData(List<DetectionResult> detections) {
        DetectionInfo info = new DetectionInfo();

        // 遍历检测结果填充数据
        detections.forEach(d -> {
            String coord = String.format("[%.2f,%.2f]", d.center[0], d.center[1]);
            switch(d.className) {
                case "MCPFirst":   info.setMCPFirst(coord); break;
                case "MCPThird":   info.setMCPThird(coord); break;
                case "MCPFifth":   info.setMCPFifth(coord); break;
                case "PIPFirst":   info.setPIPFirst(coord); break;
                case "PIPThird":   info.setPIPThird(coord); break;
                case "PIPFifth":   info.setPIPFifth(coord); break;
                case "MIPThird":   info.setMIPThird(coord); break;
                case "MIPFifth":   info.setMIPFifth(coord); break;
                case "DIPFirst":   info.setDIPFirst(coord); break;
                case "DIPThird":   info.setDIPThird(coord); break;
                case "DIPFifth":   info.setDIPFifth(coord); break;
                case "Radius":    info.setRadius(coord); break;
                case "Ulna":      info.setUlna(coord); break;
            }
        });

        try {
            return detectionRepo.save(info);
        } catch (Exception e) {
            System.err.println("数据库保存失败: " + e.getMessage());
            return null;
        }
    }

    // 绘制检测框到图像副本并保存
    private void saveImageWithDetections(BufferedImage originImage, List<DetectionResult> detections) {
        // 创建图像副本
        BufferedImage imageCopy = new BufferedImage(
                originImage.getWidth(),
                originImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g2d = imageCopy.createGraphics();
        g2d.drawImage(originImage, 0, 0, null);

        // 绘制检测框
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));

        for (DetectionResult det : detections) {
            float[] bbox = det.bbox;
            g2d.drawRect(
                    (int) Math.round(bbox[0]),
                    (int) Math.round(bbox[1]),
                    (int) Math.round(bbox[2] - bbox[0]),
                    (int) Math.round(bbox[3] - bbox[1])
            );
            g2d.drawString(det.className, (int) Math.round(bbox[0]), (int) Math.round(bbox[1] - 5));
        }

        g2d.dispose();

        // 保存图像到项目根目录
        File outputDir = new File("detection_errors");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String filename = String.format("error_%s.png", new Date().getTime());
        File outputFile = new File(outputDir, filename);

        try {
            ImageIO.write(imageCopy, "PNG", outputFile);
            System.out.println("检测框异常图像已保存到: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存图像失败: " + e.getMessage());
        }
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
        }
    }

    // 检测结果类
    private static class DetectionResult {
        String className;
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

}