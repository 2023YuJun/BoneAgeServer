package com.example.server.service;

import ai.onnxruntime.*;
import org.apache.commons.imaging.Imaging;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassifyService {
    // 模型映射关系
    private static final Map<String, String> MODEL_MAPPING = Map.ofEntries(
            Map.entry("MCPFirst", "MCPFirst"),
            Map.entry("MCPThird", "MCP"),
            Map.entry("MCPFifth", "MCP"),
            Map.entry("PIPFirst", "PIPFirst"),
            Map.entry("PIPThird", "PIP"),
            Map.entry("PIPFifth", "PIP"),
            Map.entry("MIPThird", "MIP"),
            Map.entry("MIPFifth", "MIP"),
            Map.entry("DIPFirst", "DIPFirst"),
            Map.entry("DIPThird", "DIP"),
            Map.entry("DIPFifth", "DIP"),
            Map.entry("Radius", "Radius"),
            Map.entry("Ulna", "Ulna")
    );

    // 模型配置
    private static final int INPUT_SIZE = 224; // 分类模型输入尺寸
    private final OrtEnvironment env;
    private final Map<String, OrtSession> modelSessions = new HashMap<>();

    public ClassifyService() throws OrtException {
        env = OrtEnvironment.getEnvironment();
        loadModels();
    }

    // 通过类路径资源流加载模型
    private void loadModels() throws OrtException {
        Set<String> requiredModels = new HashSet<>(MODEL_MAPPING.values());
        for (String modelName : requiredModels) {
            try (InputStream modelStream = getClass().getResourceAsStream("/model/" + modelName + ".onnx")) {
                if (modelStream == null) {
                    System.err.println("警告: 模型 " + modelName + " 未找到，跳过加载");
                    continue;
                }

                OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
                opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
                opts.setInterOpNumThreads(1);
                opts.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());

                // 从流中读取字节并创建会话
                byte[] modelBytes = modelStream.readAllBytes();
                modelSessions.put(modelName, env.createSession(modelBytes, opts));
                System.out.println("成功加载模型: " + modelName);
            } catch (Exception e) {
                System.err.println("加载模型 " + modelName + " 失败: " + e.getMessage());
            }
        }
    }

    // 主分类方法
    public Map<String, Integer> classify(Map<String, Object> detectionResults, BufferedImage originImage) {
        Map<String, Integer> classificationResults = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> results =
                    (Map<String, List<Map<String, Object>>>) detectionResults.get("results");

            for (Map.Entry<String, List<Map<String, Object>>> entry : results.entrySet()) {
                String className = entry.getKey();
                String modelName = MODEL_MAPPING.get(className);

                // 检查模型是否加载成功
                if (!modelSessions.containsKey(modelName)) {
                    System.err.println("警告: " + className + " 的模型 " + modelName + " 未加载");
                    continue;
                }

                for (Map<String, Object> detection : entry.getValue()) {
                    float[] bbox = (float[]) detection.get("bbox");
                    BufferedImage crop = cropImage(originImage, bbox);

                    try {
                        int result = infer(crop, modelSessions.get(modelName));
                        classificationResults.put(className, result);
                    } catch (Exception e) {
                        System.err.println(className + " 分类失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("分类流程异常: " + e.getMessage());
        }
        return classificationResults;
    }

    // 图像裁剪
    private BufferedImage cropImage(BufferedImage image, float[] bbox) {
        int x = (int) bbox[0];
        int y = (int) bbox[1];
        int width = (int) (bbox[2] - bbox[0]);
        int height = (int) (bbox[3] - bbox[1]);

        // 边界检查
        x = Math.max(0, x);
        y = Math.max(0, y);
        width = Math.min(width, image.getWidth() - x);
        height = Math.min(height, image.getHeight() - y);

        return image.getSubimage(x, y, width, height);
    }

    // 模型推理
    private int infer(BufferedImage image, OrtSession session) throws OrtException {
        // 预处理
        float[][][][] inputData = preprocess(image);
        OnnxTensor tensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(flattenArray(inputData)),
                new long[]{1, 3, INPUT_SIZE, INPUT_SIZE});

        // 执行推理
        try (OrtSession.Result result = session.run(Collections.singletonMap("images", tensor))) {
            float[][] output = (float[][]) result.get(0).getValue();
            return argMax(output[0]);
        }
    }

    // 图像预处理（RGB归一化）
    private float[][][][] preprocess(BufferedImage image) {
        BufferedImage resized = resizeImage(image);
        float[][][][] arr = new float[1][3][INPUT_SIZE][INPUT_SIZE];

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = resized.getRGB(x, y);
                // 通道顺序：RGB
                arr[0][0][y][x] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                arr[0][1][y][x] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                arr[0][2][y][x] = (pixel & 0xFF) / 255.0f;         // B
            }
        }
        return arr;
    }

    // 图像缩放
    private BufferedImage resizeImage(BufferedImage image) {
        BufferedImage resized = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(
                image.getScaledInstance(INPUT_SIZE, INPUT_SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        return resized;
    }

    // 工具方法：平铺数组
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

    // 工具方法：找最大值索引
    private int argMax(float[] array) {
        int maxIdx = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIdx]) maxIdx = i;
        }
        return maxIdx;
    }
}