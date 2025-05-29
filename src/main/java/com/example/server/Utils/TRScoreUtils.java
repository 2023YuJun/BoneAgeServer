package com.example.server.Utils;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TRScoreUtils {
    private static final PolynomialSplineFunction maleSpline;
    private static final PolynomialSplineFunction femaleSpline;
    private static final Map<Boolean, Map<String, List<Integer>>> SCORE_TABLES;

    static {
        // 初始化性别-部位-分数表
        SCORE_TABLES = new HashMap<>();

        // 男性数据
        Map<String, List<Integer>> maleScores = new HashMap<>();
        maleScores.put("Radius", Arrays.asList(16, 21, 30, 39, 59, 87, 138, 213));
        maleScores.put("Ulna", Arrays.asList(27, 30, 32, 40, 58, 107, 181));
        maleScores.put("MCPFirst", Arrays.asList(6, 9, 14, 21, 26, 36, 49, 67));
        maleScores.put("MCPThird", Arrays.asList(4, 5, 9, 12, 19, 31, 43, 52));
        maleScores.put("MCPFifth", Arrays.asList(4, 6, 9, 14, 18, 29, 43, 52));
        maleScores.put("PIPFirst", Arrays.asList(7, 8, 11, 17, 26, 38, 52, 67));
        maleScores.put("PIPThird", Arrays.asList(4, 4, 9, 15, 23, 31, 40, 53));
        maleScores.put("PIPFifth", Arrays.asList(4, 5, 9, 15, 21,30, 39, 51));
        maleScores.put("MIPThird", Arrays.asList(4, 6, 9, 15, 22, 32, 43, 52));
        maleScores.put("MIPFifth", Arrays.asList(6, 7, 9, 15, 23, 32, 42, 49));
        maleScores.put("DIPFirst", Arrays.asList(5, 6, 11, 17, 26, 38, 46, 66));
        maleScores.put("DIPThird", Arrays.asList(4, 6, 8, 13, 18, 28, 34, 49));
        maleScores.put("DIPFifth", Arrays.asList(5, 6, 9, 13, 18, 27, 34, 48));
        SCORE_TABLES.put(true, maleScores);

        // 女性数据
        Map<String, List<Integer>> femaleScores = new HashMap<>();
        femaleScores.put("Radius", Arrays.asList(23, 30, 44, 56, 78, 114, 160, 218));
        femaleScores.put("Ulna", Arrays.asList(30, 33, 37, 45, 74, 118, 173));
        femaleScores.put("MCPFirst", Arrays.asList(6, 12, 18, 24, 31, 43, 53, 67));
        femaleScores.put("MCPThird", Arrays.asList(5, 8, 12, 16, 23, 37, 47, 53));
        femaleScores.put("MCPFifth", Arrays.asList(6, 9, 12, 17, 23, 35, 48, 52));
        femaleScores.put("PIPFirst", Arrays.asList(9, 11, 14, 20, 31, 44, 56, 67));
        femaleScores.put("PIPThird", Arrays.asList(5, 7, 12, 19, 27, 37, 44, 54));
        femaleScores.put("PIPFifth", Arrays.asList(6, 7, 12, 18, 26, 35, 42, 51));
        femaleScores.put("MIPThird", Arrays.asList(6, 8, 12, 18, 27, 36, 45, 52));
        femaleScores.put("MIPFifth", Arrays.asList(7, 8, 12, 18, 28, 35, 43, 49));
        femaleScores.put("DIPFirst", Arrays.asList(7, 9, 15, 22, 33, 48, 51, 68));
        femaleScores.put("DIPThird", Arrays.asList(7, 8, 11, 15, 22, 33, 37, 49));
        femaleScores.put("DIPFifth", Arrays.asList(7, 8, 11, 15, 22, 32, 36, 47));
        SCORE_TABLES.put(false, femaleScores);

        // 初始化样条插值器
        double[] xMale = {
                0, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10,
                10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5, 16};
        double[] yMale = {
                0, 27, 48, 70, 91, 110, 120, 131, 141, 152, 163, 175, 185, 196, 211, 225, 240, 261, 276, 296,
                316, 342, 375, 420, 475, 543, 628, 714, 812, 914, 978, 1000};

        double[] xFemale = {
                0, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10,
                10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15};
        double[] yFemale = {
                0, 65, 114, 156, 175, 188, 197, 214, 228, 44, 263, 280, 297, 317, 335, 361,
                388, 423, 466, 516, 569, 625, 688, 753, 827, 921, 973, 994, 997, 1000};

        try {
            maleSpline = new org.apache.commons.math3.analysis.interpolation.SplineInterpolator()
                    .interpolate(xMale, yMale);
            femaleSpline = new org.apache.commons.math3.analysis.interpolation.SplineInterpolator()
                    .interpolate(xFemale, yFemale);
        } catch (Exception e) {
            throw new RuntimeException("初始化样条插值器失败", e);
        }
    }

    // 映射规则内部类
    private static class GradeMapping {
        final int min;
        final int max;
        final int mapped;

        GradeMapping(int min, int max, int mapped) {
            this.min = min;
            this.max = max;
            this.mapped = mapped;
        }

        boolean matches(int grade) {
            return grade >= min && grade <= max;
        }
    }

    // 等级映射表（新增静态成员）
    private static final Map<String, List<GradeMapping>> GRADE_MAPPINGS = new HashMap<>();

    static {
        // 初始化映射规则（在静态初始化块中添加）
        // MCP系列
        GRADE_MAPPINGS.put("MCPFirst", Arrays.asList(
                new GradeMapping(5, 6, 5),
                new GradeMapping(7, 8, 6),
                new GradeMapping(9, 10, 7),
                new GradeMapping(11, 11, 8)
        ));

        // MCPThird和MCPFifth使用相同规则
        List<GradeMapping> mcprMapping = Arrays.asList(
                new GradeMapping(4, 5, 4),
                new GradeMapping(6, 6, 5),
                new GradeMapping(7, 7, 6),
                new GradeMapping(8, 9, 7),
                new GradeMapping(10, 10, 8)
        );
        GRADE_MAPPINGS.put("MCPThird", mcprMapping);
        GRADE_MAPPINGS.put("MCPFifth", mcprMapping);

        // PIP系列统一规则
        List<GradeMapping> pipMapping = Arrays.asList(
                new GradeMapping(4, 5, 4),
                new GradeMapping(6, 7, 5),
                new GradeMapping(8, 9, 6),
                new GradeMapping(10, 11, 7),
                new GradeMapping(12, 12, 8)
        );
        Arrays.asList("PIPFirst", "PIPThird", "PIPFifth", "MIPThird", "MIPFifth")
                .forEach(part -> GRADE_MAPPINGS.put(part, pipMapping));

        // DIP系列统一规则
        List<GradeMapping> dipMapping = Arrays.asList(
                new GradeMapping(5, 6, 5),
                new GradeMapping(7, 8, 6),
                new GradeMapping(9, 10, 7),
                new GradeMapping(11, 11, 8)
        );
        Arrays.asList("DIPFirst", "DIPThird", "DIPFifth")
                .forEach(part -> GRADE_MAPPINGS.put(part, dipMapping));

        // Radius特殊规则
        GRADE_MAPPINGS.put("Radius", Arrays.asList(
                new GradeMapping(5, 6, 5),
                new GradeMapping(7, 7, 6),
                new GradeMapping(8, 9, 7),
                new GradeMapping(10, 14, 8) // 10-14映射到8
        ));

        // Ulna特殊规则
        GRADE_MAPPINGS.put("Ulna", Arrays.asList(
                new GradeMapping(5, 6, 5),
                new GradeMapping(7, 7, 6),
                new GradeMapping(8, 12, 7) // 8-12映射到7
        ));
    }

    /**
     * 等级转换方法
     * @param part 部位名称
     * @param originalGrade 原始等级（外部输入的1-based索引）
     * @return 转换后的等级（1-based）
     */
    public static int mapGrade(String part, int originalGrade) {
        // 检查是否为有效等级
        if (originalGrade < 1) {
            throw new IllegalArgumentException("无效的等级值: " + originalGrade);
        }

        // 获取映射规则
        List<GradeMapping> mappings = GRADE_MAPPINGS.get(part);

        // 无映射规则的直接返回原值
        if (mappings == null) return originalGrade;

        // 遍历规则查找匹配项
        for (GradeMapping mapping : mappings) {
            if (mapping.matches(originalGrade)) {
                return mapping.mapped;
            }
        }

        // 无匹配规则时返回原值
        return originalGrade;
    }

    /**
     * 计算骨龄
     * @param isMale 是否为男性（true=男，false=女）
     * @param partIndices 部位及其对应的索引
     * @return 骨龄
     */
    public static double calculateBoneAge(boolean isMale, Map<String, Integer> partIndices) {
        int totalScore = calculateTotalScore(isMale, partIndices);
        return TRScore(totalScore, isMale);
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

            // 检查越界情况
            List<Integer> partScores = scores.get(part);
            if (partScores == null) continue; // 无效部位跳过

            // 获取映射后的等级（可能返回-1）
            int mappedGrade;
            try {
                mappedGrade = mapGrade(part, externalIndex);
            } catch (IllegalArgumentException e) {
                mappedGrade = -1;
            }

            // 计算有效分数
            if (mappedGrade != -1) {
                int internalIndex = mappedGrade - 1;
                if (internalIndex >= 0 && internalIndex < partScores.size()) {
                    total += partScores.get(internalIndex);
                }
            }
        }
        return total;
    }

    public static double TRScore(double yInput, boolean isMale) {
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
        testIndices.put("Radius", 8);
        testIndices.put("Ulna", 9);

        System.out.println(mapGrade("MCPFirst", 5));  // 5 → 5
        System.out.println(mapGrade("Radius", 9));    // 9 → 7
        System.out.println(mapGrade("Ulna", 12));     // 12 → 7
        System.out.println(mapGrade("PIPThird", 8));  // 8 → 6

        double boneAgeMale = calculateBoneAge(true, testIndices);
        double boneAgeFemale = calculateBoneAge(false, testIndices);

        System.out.println("男性骨龄: " + boneAgeMale);
        System.out.println("女性骨龄: " + boneAgeFemale);
    }
}
