/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Loan;

public class BaselWeighting {
    private double BASEL_MULTIPLIER = 10.0;
    public static final int CONSTRUCTION = 0;
    public static final int MORTGAGE = 1;
    public static final int GOVERNMENT = 2;
    public static final int IBL = 3;
    private static double construction = 0.25;
    private static double mortgage = 0.5;
    private static double government = 1.0;
    private static double ibl = 1.0;
    private static double[] riskmatrix = new double[]{construction, mortgage, government, ibl};

    public static double riskWeighting(Loan loan) {
        if (loan.risktype == -1) {
            return 1.0;
        }
        return riskmatrix[loan.risktype];
    }

    public double getRiskWeighting(int n) {
        return riskmatrix[n];
    }

    public double getBaselMultiplier() {
        return this.BASEL_MULTIPLIER;
    }

    public void setBaselMultiplier(double d) {
        this.BASEL_MULTIPLIER = d;
    }
}

