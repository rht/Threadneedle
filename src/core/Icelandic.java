/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Account;
import core.Bank;
import core.BaselGovt;
import core.CentralBank;
import core.Govt;
import core.Loan;
import java.io.PrintStream;

public class Icelandic
extends Loan {
    static String name = "Icelandic";
    private static int frequency = 30;
    private static double daysOfInterest = 0.08333333333333333;
    private double[] AF;
    private double[] II;
    private double[] CPI;
    private double[] principal;
    private double[] excessCapital;
    private BaselGovt govt;
    private Bank bank;

    public Icelandic(Govt govt, Bank bank, long l, double d, int n, int n2, Account account) {
        super(l, d, n, frequency, n2, account, Loan.Type.COMPOUND);
        this.bank = bank;
        this.ownerId = bank.Id;
        this.govt = (BaselGovt)govt;
        this.ownerAcct = bank.getAccount();
        this.negAm = true;
        this.loanType = Loan.Type.INDEXED;
        this.originalCapital = this.capitalAmount;
        this.negAmCapital = 0L;
        this.principal[0] = l;
    }

    @Override
    public void setSchedules() {
        int n = this.duration / frequency;
        this.AF = new double[n];
        this.II = new double[n];
        this.CPI = new double[n];
        this.principal = new double[n];
        this.excessCapital = new double[n];
        double d = daysOfInterest * this.interestRate * 0.01;
        for (int i = 0; i < this.duration / frequency; ++i) {
            this.AF[i] = 1.0 / d - 1.0 / (d * Math.pow(1.0 + d, n - i));
            this.CPI[i] = -1.0;
        }
        double d2 = daysOfInterest;
        double d3 = Math.pow(1.0, 0.08333333333333333) - 1.0;
        this.II[0] = 100.0 + 100.0 * d3;
        this.principal[0] = (double)this.capitalAmount * this.II[0] / 100.0;
        double d4 = this.principal[0] / this.AF[0];
        d = this.principal[0] * this.interestRate / 100.0 * d2;
        double d5 = d4 - d;
        this.capitalSchedule[0] = (long)d5;
        this.interestSchedule[0] = (long)d;
    }

    @Override
    public long[] getNextLoanRepayment() {
        this.recalculateSchedule();
        return super.getNextLoanRepayment();
    }

    private void recalculateSchedule() {
        double d = this.govt.centralbank.getCPI();
        if (d < 0.0) {
            d = 0.0;
        }
        this.CPI[this.payIndex] = d;
        if (this.payIndex == 0) {
            this.II[0] = 100.0 + 100.0 * (Math.pow(1.0 + d, 0.08333333333333333) - 1.0);
            this.principal[0] = (double)this.capitalAmount * this.II[0] / 100.0;
            this.excessCapital[0] = (double)this.capitalAmount * (this.II[0] / 100.0 - 1.0);
        } else {
            this.II[this.payIndex] = this.II[this.payIndex - 1] + this.II[this.payIndex - 1] * (Math.pow(1.0 + d, 0.08333333333333333) - 1.0);
            this.principal[this.payIndex] = (this.principal[this.payIndex - 1] - (double)this.capitalSchedule[this.payIndex - 1]) * this.II[this.payIndex] / this.II[this.payIndex - 1];
            this.excessCapital[this.payIndex] = (this.principal[this.payIndex - 1] - (double)this.capitalSchedule[this.payIndex - 1]) * (this.II[this.payIndex] / this.II[this.payIndex - 1] - 1.0);
        }
        double d2 = this.principal[this.payIndex] / this.AF[this.payIndex];
        double d3 = this.principal[this.payIndex] * this.interestRate / 100.0 * daysOfInterest;
        double d4 = d2 - d3;
        this.capitalSchedule[this.payIndex] = (long)d4;
        this.interestSchedule[this.payIndex] = (long)d3;
        if (this.payIndex == this.capitalSchedule.length - 1) {
            long l = this.totalCapitalSchedule();
            long l2 = l - (long)((double)(this.capitalAmount + this.negAmCapital) + this.excessCapital[this.payIndex]);
            double[] arrd = this.excessCapital;
            int n = this.payIndex;
            arrd[n] = arrd[n] + (double)l2;
        }
    }

    @Override
    public long getCapitalOutstanding() {
        return this.capitalAmount - this.capitalPaid + this.negAmCapital - this.capitalWrittenOff;
    }

    @Override
    public long getNegamDecrease(long l) {
        double d;
        double d2 = this.principal[this.payIndex] - (double)l;
        if (d2 > (double)this.originalCapital) {
            d = l;
        } else {
            d = this.negAmCapital - this.negAmCapitalRecognised;
            if (d > (double)l) {
                d = l;
            }
        }
        return (long)d;
    }

    @Override
    public boolean principalIncreasing() {
        return this.payIndex != 0 && this.principal[this.payIndex] - this.principal[this.payIndex - 1] > 0.0;
    }

    @Override
    public void remove() {
        if (this.negAmCapital != this.negAmCapitalRecognised) {
            System.out.println(this.Id + " Neg am discrepancy " + this.negAmCapital + " != " + this.negAmCapitalRecognised);
            this.printSchedule(this.capitalSchedule, "Capital");
            this.printSchedule(this.interestSchedule, "Interest");
            this.printSchedule(this.excessCapital, "Excess");
            System.out.println("Paid capital " + this.totalCapitalSchedule() + " Paid negam " + (this.totalCapitalSchedule() - this.originalCapital));
            this.printRepayments();
            throw new RuntimeException("Negam mismatch");
        }
        super.remove();
    }

    @Override
    public long getPrincipalIncrease() {
        return (long)this.excessCapital[this.payIndex];
    }

    @Override
    public String toString() {
        return "Icelandic: " + this.Id + " " + this.capitalAmount + "@" + this.interestRate + "%/" + this.duration + "[" + this.ownerAcct.getName() + "=>" + this.ownerId + "]";
    }
}

