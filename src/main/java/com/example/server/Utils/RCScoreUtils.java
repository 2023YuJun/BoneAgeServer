package com.example.server.Utils;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

import java.util.*;

public class RCScoreUtils {
    private static final PolynomialSplineFunction maleSpline;
    private static final PolynomialSplineFunction femaleSpline;
    public static final Map<Boolean, Map<String, List<Integer>>> SCORE_TABLES;

    static {
        // 初始化性别-部位-分数表
        SCORE_TABLES = new HashMap<>();

        // 女性数据
        Map<String, List<Integer>> femaleScores = new HashMap<>();
        femaleScores.put("Radius", Arrays.asList(10, 15, 22, 25, 40, 59, 91, 125, 138, 178, 192, 199, 203, 210));
        femaleScores.put("Ulna", Arrays.asList(27, 31, 36, 50, 73, 95, 120, 157, 168, 176, 182, 189));
        femaleScores.put("MCPFirst", Arrays.asList(5, 7, 10, 16, 23, 28, 34, 41, 47, 53, 66));
        femaleScores.put("MCPThird", Arrays.asList(3, 5, 6, 9, 14, 21, 32, 40, 47, 51));
        femaleScores.put("MCPFifth", Arrays.asList(4, 5, 7, 10, 15, 22, 33, 43, 47, 51));
        femaleScores.put("PIPFirst", Arrays.asList(6, 7, 8, 11, 17, 26, 32, 38, 45, 53, 60, 67));
        femaleScores.put("PIPThird", Arrays.asList(3, 5, 7, 9, 15, 20, 25, 29, 35, 41, 46, 51));
        femaleScores.put("PIPFifth", Arrays.asList(4, 5, 7, 11, 18, 21, 25, 29, 34, 40, 45, 50));
        femaleScores.put("MIPThird", Arrays.asList(4, 5, 7, 10, 16, 21, 25, 29, 35, 43, 46, 51));
        femaleScores.put("MIPFifth", Arrays.asList(3, 5, 7, 12, 19, 23, 27, 32, 35, 39, 43, 49));
        femaleScores.put("DIPFirst", Arrays.asList(5, 6, 8, 10, 20, 31, 38, 44, 45, 52, 67));
        femaleScores.put("DIPThird", Arrays.asList(3, 5, 7, 10, 16, 24, 30, 33, 36, 39, 49));
        femaleScores.put("DIPFifth", Arrays.asList(5, 6, 7, 11, 18, 25, 29, 33, 35, 39, 49));
        SCORE_TABLES.put(false, femaleScores);

        // 男性数据
        Map<String, List<Integer>> maleScores = new HashMap<>();
        maleScores.put("Radius", Arrays.asList(8, 11, 15, 18, 31, 46, 76, 118, 135, 171, 188, 197, 201, 209));
        maleScores.put("Ulna", Arrays.asList(25, 30, 35, 43, 61, 80, 116, 157, 168, 180, 187, 194));
        maleScores.put("MCPFirst", Arrays.asList(4, 5, 8, 16, 22, 26, 34, 39, 45, 52, 66));
        maleScores.put("MCPThird", Arrays.asList(3, 4, 5, 8, 13, 19, 30, 38, 44, 51));
        maleScores.put("MCPFifth", Arrays.asList(3, 4, 6, 9, 14, 19, 31, 41, 46, 50));
        maleScores.put("PIPFirst", Arrays.asList(4, 5, 7, 11, 17, 23, 29, 36, 44, 52, 59, 66));
        maleScores.put("PIPThird", Arrays.asList(3, 4, 5, 8, 14, 19, 23, 28, 34, 40, 45, 50));
        maleScores.put("PIPFifth", Arrays.asList(3, 4, 6, 10, 16, 19, 24, 28, 33, 40, 44, 50));
        maleScores.put("MIPThird", Arrays.asList(3, 4, 5, 9, 14, 18, 23, 28, 35, 42, 45, 50));
        maleScores.put("MIPFifth", Arrays.asList(3, 4, 6, 11, 17, 21, 26, 31, 36, 40, 43, 49));
        maleScores.put("DIPFirst", Arrays.asList(4, 5, 6, 9, 19, 28, 36, 43, 46, 51, 67));
        maleScores.put("DIPThird", Arrays.asList(3, 4, 5, 9, 15, 23, 29, 33, 37, 40, 49));
        maleScores.put("DIPFifth", Arrays.asList(3, 4, 6, 11, 17, 23, 29, 32, 36, 40, 49));
        SCORE_TABLES.put(true, maleScores);

        // 初始化样条插值器
        double[] xMale = {0, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10,
                10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5, 16, 16.5, 17, 17.5, 18};
        double[] yMale = {0, 15, 27, 38, 50, 62, 68, 75, 78, 85, 93, 101, 109, 118, 130, 145, 166, 189,
                212, 238, 268, 305, 359, 416, 480, 547, 628, 710, 781, 865, 926, 965, 980, 987, 996, 1000};

        double[] xFemale = {0, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10,
                10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5, 16, 16.5, 17};
        double[] yFemale = {0, 40, 64, 78, 90, 101, 107, 116, 123, 131, 146, 165, 184, 211, 236, 268,
                307, 348, 399, 453, 510, 572, 637, 597, 775, 855, 907, 946, 968, 980, 990, 992, 996, 1000};

        try {
            maleSpline = new org.apache.commons.math3.analysis.interpolation.SplineInterpolator()
                    .interpolate(xMale, yMale);
            femaleSpline = new org.apache.commons.math3.analysis.interpolation.SplineInterpolator()
                    .interpolate(xFemale, yFemale);
        } catch (Exception e) {
            throw new RuntimeException("初始化样条插值器失败", e);
        }
    }

    /**
     * 计算骨龄
     * @param isMale 是否为男性（true=男，false=女）
     * @param partIndices 部位及其对应的索引
     * @return 骨龄
     */
    public static double calculateBoneAge(boolean isMale, Map<String, Integer> partIndices) {
        int totalScore = calculateTotalScore(isMale, partIndices);
        return RCScore(totalScore, isMale);
    }

    /**
     * 计算总分
     * @param isMale 性别标识
     * @param partIndices 部位索引
     * @return 总分
     */
    public static int calculateTotalScore(boolean isMale, Map<String, Integer> partIndices) {
        Map<String, List<Integer>> scores = SCORE_TABLES.get(isMale);
        if (scores == null) {
            throw new IllegalArgumentException("无效的性别标识");
        }

        int total = 0;
        for (Map.Entry<String, Integer> entry : partIndices.entrySet()) {
            String part = entry.getKey();
            int externalIndex = entry.getValue();
            List<Integer> partScores = scores.get(part);
            if (partScores == null) continue;

            int internalIndex = externalIndex - 1;
            if (internalIndex < 0 || internalIndex >= partScores.size()) {
                continue;
            }
            total += partScores.get(internalIndex);
        }
        return total;
    }

    public static double RCScore(double yInput, boolean isMale) {
        final PolynomialSplineFunction spline = isMale ? maleSpline : femaleSpline;
        final double xMin = spline.getKnots()[0];
        final double xMax = spline.getKnots()[spline.getKnots().length - 1];
        final double yMin = spline.value(xMin);
        final double yMax = spline.value(xMax);

        if (yInput < Math.min(yMin, yMax) || yInput > Math.max(yMin, yMax)) {
            throw new IllegalArgumentException("输入值超出有效范围: [" + Math.min(yMin, yMax) + ", " + Math.max(yMin, yMax) + "]");
        }

        UnivariateFunction func = x -> spline.value(x) - yInput;
        UnivariateSolver solver = new BrentSolver(1e-6, 1e-12);

        try {
            return solver.solve(100, func, xMin, xMax);
        } catch (NoBracketingException | TooManyEvaluationsException e) {
            throw new RuntimeException("求解失败，请检查输入参数有效性", e);
        }
    }

    public static void main(String[] args) {
        // 示例用法
        Map<String, Integer> testIndices = new HashMap<>();
        testIndices.put("Radius", 5);
        testIndices.put("Ulna", 3);

        double boneAgeMale = calculateBoneAge(true, testIndices);
        double boneAgeFemale = calculateBoneAge(false, testIndices);

        System.out.println("男性骨龄: " + boneAgeMale);
        System.out.println("女性骨龄: " + boneAgeFemale);
    }
}