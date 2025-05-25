package com.example.server.Utils;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

public class RCScoreUtils {
    private static final PolynomialSplineFunction maleSpline;
    private static final PolynomialSplineFunction femaleSpline;

    static {
        // 男性数据
        double[] xMale = {0, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10,
                10.5, 11, 11.5, 12, 12.5, 13, 13.5, 14, 14.5, 15, 15.5, 16, 16.5, 17, 17.5, 18};
        double[] yMale = {0, 15, 27, 38, 50, 62, 68, 75, 78, 85, 93, 101, 109, 118, 130, 145, 166, 189,
                212, 238, 268, 305, 359, 416, 480, 547, 628, 710, 781, 865, 926, 965, 980, 987, 996, 1000};

        // 女性数据
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

    public static double RCScore(double yInput, boolean isMale) {
        final PolynomialSplineFunction spline = isMale ? maleSpline : femaleSpline;
        final double xMin = spline.getKnots()[0];
        final double xMax = spline.getKnots()[spline.getKnots().length - 1];
        final double yMin = spline.value(xMin);
        final double yMax = spline.value(xMax);

        // 验证输入范围
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
        double rc1 = RCScoreUtils.RCScore(500.0, true);  // 男性
        double rc2 = RCScoreUtils.RCScore(500.0, false); // 女性
        System.out.println("RC1: " + rc1);
        System.out.println("RC2: " + rc2);
    }
}