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

    public Map<String, Object> detect(String imagePath) throws Exception {
        // 读取原始图像
        currentOriginImage = Imaging.getBufferedImage(new File(imagePath));

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
                return Collections.singletonMap("error",
                        "检测到 "+detections.size()+" 个结果，预期应为21个");
            }

            // 打包最终结果
            return packageResults(detections);
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
        Map<String, List<Map<String, Object>>> resultMap = new LinkedHashMap<>();

        // 处理保持原样的类型
        Arrays.asList("MCPFirst", "Radius", "Ulna").forEach(type -> {
            resultMap.put(type, detections.stream()
                    .filter(d -> d.className.equals(type))
                    .map(this::convertToMap)
                    .collect(Collectors.toList()));
        });

        // 处理需要转换的类型
        processBoneType(resultMap, detections, "DistalPhalanx", isLeftHand);
        processBoneType(resultMap, detections, "MCP", isLeftHand);
        processBoneType(resultMap, detections, "MiddlePhalanx", isLeftHand);
        processBoneType(resultMap, detections, "ProximalPhalanx", isLeftHand);

        saveDetectionData(detections);

        return Collections.singletonMap("results", resultMap);
    }

    private void processBoneType(Map<String, List<Map<String, Object>>> resultMap,
                                 List<DetectionResult> detections,
                                 String boneType,
                                 boolean isLeftHand) {
        // 1. 获取原始检测结果并过滤
        List<DetectionResult> originItems = detections.stream()
                .filter(d -> d.className.equals(boneType))
                .collect(Collectors.toList());

        // 2. 按中心点 X 坐标升序排序（从左到右）
        originItems.sort(Comparator.comparingDouble(d -> d.center[0]));

        // 3. 获取当前骨骼类型和手性对应的重命名规则
        Map<Integer, String> renameMap = RENAME_RULES.get(boneType).get(isLeftHand);

        // 4. 遍历排序后的结果，仅处理规则中定义的索引
        for (int index = 0; index < originItems.size(); index++) {
            if (renameMap.containsKey(index)) {
                DetectionResult origin = originItems.get(index);
                String newClassName = renameMap.get(index);
                DetectionResult renamed = new DetectionResult(
                        newClassName,
                        origin.center,
                        origin.bbox,
                        origin.confidence
                );
                resultMap.computeIfAbsent(newClassName, k -> new ArrayList<>())
                        .add(convertToMap(renamed));
            }
        }
    }

    private Map<String, Object> convertToMap(DetectionResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("center", result.center);
        map.put("bbox", result.bbox);
        map.put("crop", getSubImage(result));
        return map;
    }

    private BufferedImage getSubImage(DetectionResult det) {
        int x1 = (int) det.bbox[0];
        int y1 = (int) det.bbox[1];
        int width = (int) (det.bbox[2] - x1);
        int height = (int) (det.bbox[3] - y1);

        // 边界检查
        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);
        width = Math.min(width, currentOriginImage.getWidth() - x1);
        height = Math.min(height, currentOriginImage.getHeight() - y1);

        return currentOriginImage.getSubimage(x1, y1, width, height);
    }

    private void saveDetectionData(List<DetectionResult> detections) {
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
            detectionRepo.save(info);
        } catch (Exception e) {
            System.err.println("数据库保存失败: " + e.getMessage());
            // 可添加重试逻辑或异常处理
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
//    public static void main(String[] args) {
//        try {
//            DetectionService detector = new DetectionService();
//
//            // 获取测试图像路径
//            String imagePath = Objects.requireNonNull(
//                    DetectionService.class.getClassLoader().getResource("static/test_handX_R.png")
//            ).getFile();
//
//            Map<String, Object> results = detector.detect(imagePath);
//
//            if (results.containsKey("error")) {
//                System.err.println("检测错误: " + results.get("error"));
//                return;
//            }
//
//            // 打印结果
//            @SuppressWarnings("unchecked")
//            Map<String, List<Map<String, Object>>> detections =
//                    (Map<String, List<Map<String, Object>>>) results.get("results");
//            System.out.println("=== 最终检测结果 ===");
//            detections.forEach((className, items) -> {
//                System.out.printf("%s (%d 个):\n", className, items.size());
//                items.forEach(item -> {
//                    float[] center = (float[]) item.get("center");
//                    System.out.printf("\t中心点: (%.1f, %.1f)\n", center[0], center[1]);
//                    float[] bbox = (float[]) item.get("bbox");
//                    System.out.printf("\t边界框: [%.1f, %.1f, %.1f, %.1f]\n",
//                            bbox[0], bbox[1], bbox[2], bbox[3]);
//                });
//            });
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public static void main(String[] args) {
//        try {
//            DetectionService detector = new DetectionService();
//            String imagePath = Objects.requireNonNull(
//                    DetectionService.class.getClassLoader().getResource("static/test_handX_L.png")
//            ).getFile();
//
//            // 执行检测
//            Map<String, Object> results = detector.detect(imagePath);
//            if (results.containsKey("error")) {
//                System.err.println("检测错误: " + results.get("error"));
//                return;
//            }
//
//            // 获取原始图像副本用于绘制
//            BufferedImage outputImage = new BufferedImage(
//                    detector.currentOriginImage.getWidth(),
//                    detector.currentOriginImage.getHeight(),
//                    BufferedImage.TYPE_INT_RGB
//            );
//            Graphics2D g = outputImage.createGraphics();
//            g.drawImage(detector.currentOriginImage, 0, 0, null);
//
//            // 设置绘制参数
//            g.setFont(new Font("Arial", Font.BOLD, 14));
//            g.setStroke(new BasicStroke(2));
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            // 遍历检测结果并绘制
//            @SuppressWarnings("unchecked")
//            Map<String, List<Map<String, Object>>> detections =
//                    (Map<String, List<Map<String, Object>>>) results.get("results");
//
//            detections.forEach((className, items) -> {
//                items.forEach(item -> {
//                    float[] bbox = (float[]) item.get("bbox");
//                    float[] center = (float[]) item.get("center");
//
//                    // 绘制检测框（红色）
//                    g.setColor(Color.RED);
//                    g.drawRect((int)bbox[0], (int)bbox[1],
//                            (int)(bbox[2]-bbox[0]), (int)(bbox[3]-bbox[1]));
//
//                    // 绘制中心点（蓝色）
//                    g.setColor(Color.BLUE);
//                    g.fillOval((int)center[0]-4, (int)center[1]-4, 8, 8);
//
//                    // 绘制坐标文本（黑色）
//                    g.setColor(Color.BLACK);
//                    String coordText = String.format("[%.0f,%.0f,%.0f,%.0f]",
//                            bbox[0], bbox[1], bbox[2], bbox[3]);
//                    g.drawString(coordText, (int)bbox[0], (int)bbox[1]-5);
//
//                    // 绘制类别名称（绿色）
//                    g.setColor(new Color(0, 150, 0));
//                    g.drawString(className, (int)center[0]+10, (int)center[1]+5);
//                });
//            });
//
//            g.dispose();
//
//            // 保存结果图像
//            File outputFile = new File("detection_result.jpg");
//            ImageIO.write(outputImage, "jpg", outputFile);
//            System.out.println("检测结果已保存至: " + outputFile.getAbsolutePath());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}