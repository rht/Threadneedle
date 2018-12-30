/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  statistics.Statistic
 */
package core;

import base.Base;
import core.Account;
import core.Agent;
import core.Widget;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import statistics.Statistic;

public abstract class Loan {
    public static int CAPITAL = 1;
    public static int INTEREST = 0;
    public int DEFAULT_LIMIT = 3;
    protected int defaultLimit;
    public Integer Id;
    public long capitalAmount;
    public long originalCapital;
    public long negAmCapital;
    public long negAmCapitalRecognised;
    public long capitalWrittenOff;
    public double interestRate;
    public int duration;
    protected Type loanType;
    protected int frequency;
    protected int start;
    public long[] interestSchedule;
    public long[] capitalSchedule;
    public long[] paidCapital;
    public long[] paidInterest;
    public boolean inWriteOff = false;
    public boolean inDefault = false;
    protected int lossProvisionAmnt;
    protected int noPayments;
    public int payIndex;
    public long interestPaid;
    public long capitalPaid;
    public int defaultCount;
    public Integer ownerId;
    public Account ownerAcct;
    public Integer borrowerId;
    public Account borrower;
    protected int totalDefaults;
    protected static int lastLoanId = 0;
    protected static int daysInYear = 365;
    protected int risktype;
    public Widget collateral;
    public boolean dbg_transfer = false;
    public boolean negAm = false;
    private HashMap<Integer, Account> accountList = new HashMap(2);

    public Loan() {
        this.Id = Loan.getLoanId();
    }

    public Loan(long l, double d, int n, int n2, int n3, Account account, Type type) {
        this.noPayments = n / n2;
        this.paidCapital = new long[this.noPayments];
        this.paidInterest = new long[this.noPayments];
        this.interestSchedule = new long[this.noPayments];
        this.capitalSchedule = new long[this.noPayments];
        this.capitalAmount = l;
        this.originalCapital = l;
        this.capitalWrittenOff = 0L;
        this.interestRate = d;
        this.duration = n;
        this.frequency = n2;
        this.start = n3;
        this.loanType = type;
        this.borrower = account;
        this.ownerId = -1;
        if (this.borrower != null) {
            this.borrowerId = account.getId();
            this.Id = Loan.getLoanId();
        }
        Base.DEBUG((String)("New loan: " + this.Id + " " + this.getClass().getSimpleName() + " " + l + "@ " + d + "/" + n + " to " + account));
        if (this.noPayments == 0) {
            throw new RuntimeException("Incorrect loan specification duration = " + n + "frequency= " + n2);
        }
        this.setSchedules();
    }

    public int setOwner(int n, Agent agent) {
        if (agent.getAccount() == null) {
            throw new RuntimeException("No account specified for loan owner");
        }
        int n2 = this.ownerId;
        this.ownerId = n;
        this.ownerAcct = agent.getAccount();
        return n2;
    }

    public void addCollateral(Widget widget) {
        if (this.collateral != null) {
            throw new RuntimeException("todo: Loan is already collaterised");
        }
        this.collateral = widget;
    }

    public void setSchedules() {
        if (this.loanType == Type.SIMPLE) {
            this.setSimpleSchedules();
        } else if (this.loanType == Type.COMPOUND || this.loanType == Type.VARIABLE) {
            this.setCompoundSchedules(0);
        } else if (this.loanType == Type.INTERBANKLOAN) {
            this.setCompoundSchedules(0);
        } else {
            throw new RuntimeException("System: unknown loan type: " + (Object)((Object)this.loanType));
        }
    }

    private void setSimpleSchedules() {
        long l = (long)(this.duration / 12) * this.capitalAmount * (long)(this.interestRate / 100.0);
        long l2 = l / (long)this.noPayments;
        long l3 = this.capitalAmount / (long)this.noPayments;
        for (int i = 0; i < this.noPayments - 1; ++i) {
            this.interestSchedule[i] = l2;
            this.capitalSchedule[i] = l3;
        }
        this.interestSchedule[this.noPayments - 1] = l2 + l - l2 * (long)this.noPayments;
        this.capitalSchedule[this.noPayments - 1] = l3 + this.capitalAmount - l3 * (long)this.noPayments;
    }

    public static int getMinLoan(double d, int n) {
        return (int)(12.0 / (d * 0.01));
    }

    public void setCompoundSchedules(int n) {
        double d = 0.0;
        int n2 = 0;
        int n3 = this.duration / this.frequency;
        double d2 = n == 0 ? (double)this.capitalAmount : (double)this.capitalSchedule[n - 1];
        double d3 = this.interestRate * 0.01 / 12.0;
        double d4 = d2 * d3 / (1.0 - Math.pow(1.0 + d3, - n3));
        for (int i = n; i < n3; ++i) {
            this.interestSchedule[i] = Math.round((float)(d2 * d3));
            d += d2 * d3;
            this.capitalSchedule[i] = Math.round((float)(d4 - d2 * d3));
            d2 -= (double)this.capitalSchedule[i];
        }
        long l = this.totalCapitalSchedule() - this.capitalAmount;
        if (l > 0L) {
            n2 = -1;
        } else {
            n2 = 1;
            l *= -1L;
        }
        int n4 = n3 - 1;
        while (l-- > 0L) {
            long[] arrl = this.capitalSchedule;
            int n5 = n4--;
            arrl[n5] = arrl[n5] + (long)n2;
        }
        l = this.totalInterestSchedule() - (long)((int)Math.ceil(d));
        if (l > 0L) {
            n2 = -1;
        } else {
            n2 = 1;
            l *= -1L;
        }
        n4 = n3 - 1;
        while (l-- > 0L) {
            long[] arrl = this.interestSchedule;
            int n6 = n4--;
            arrl[n6] = arrl[n6] + (long)n2;
        }
        if (this.totalCapitalSchedule() != this.capitalAmount) {
            System.out.println("" + this.totalCapitalSchedule() + " " + this.capitalAmount);
            throw new RuntimeException("Capital repayment incorrect in compoundschedule");
        }
    }

    public void printSchedule(long[] arrl, String string) {
        long l = 0L;
        System.out.print(string + "[" + arrl.length + "]: ");
        for (int i = 0; i < arrl.length; ++i) {
            System.out.print("" + arrl[i] + " ");
            l += arrl[i];
        }
        System.out.println("  (Total: " + l + ")");
    }

    protected void printSchedule(double[] arrd, String string) {
        double d = 0.0;
        System.out.print(string + "[" + arrd.length + "]: ");
        for (int i = 0; i < arrd.length; ++i) {
            System.out.print("" + (int)arrd[i] + " ");
            d += arrd[i];
        }
        System.out.println("  (Total: " + (int)d + ")");
    }

    public long[] getNextLoanRepayment() {
        long[] arrl = new long[]{0L, 0L};
        if (!this.inWriteOff) {
            arrl[Loan.INTEREST] = this.interestSchedule[this.payIndex];
            arrl[Loan.CAPITAL] = this.capitalSchedule[this.payIndex];
        }
        return arrl;
    }

    public long getPrevInterestPayment() {
        return this.interestSchedule[this.payIndex - 1];
    }

    public long getNextInterestRepayment() {
        return this.interestSchedule[this.payIndex];
    }

    public long getNextCapitalRepayment() {
        return this.capitalSchedule[this.payIndex];
    }

    public boolean makePayment(long[] arrl) {
        if (arrl[INTEREST] == this.interestSchedule[this.payIndex] && arrl[CAPITAL] == this.capitalSchedule[this.payIndex]) {
            this.ownerAcct.owner.s_income.add(arrl[INTEREST]);
            this.paidCapital[this.payIndex] = arrl[CAPITAL];
            this.paidInterest[this.payIndex] = arrl[INTEREST];
            ++this.payIndex;
            this.defaultCount = 0;
            this.capitalPaid += arrl[CAPITAL];
            this.interestPaid += arrl[INTEREST];
            return true;
        }
        if (arrl[INTEREST] == 0L && arrl[CAPITAL] > 0L) {
            this.paidCapital[this.payIndex] = arrl[CAPITAL];
            this.capitalPaid += arrl[CAPITAL];
            return true;
        }
        throw new RuntimeException("partial capital repayment not implemented");
    }

    public boolean installmentDue() {
        return this.defaultCount != 0 || Base.step % this.frequency == 0 && !this.repaid();
    }

    public long getPaymentDue() {
        return this.capitalSchedule[this.payIndex] + this.interestSchedule[this.payIndex];
    }

    public long getPaymentDue(int n, int n2) {
        long l = 0L;
        if (n + this.payIndex > this.capitalSchedule.length || n2 + this.payIndex > this.capitalSchedule.length) {
            n2 = this.capitalSchedule.length - 1;
        }
        for (int i = n + this.payIndex; i < n2; ++i) {
            l += this.capitalSchedule[this.payIndex + i] + this.interestSchedule[this.payIndex + i];
        }
        return l;
    }

    public long getCapitalOutstanding() {
        return this.originalCapital - this.capitalPaid - this.capitalWrittenOff;
    }

    public long getLoanAmount() {
        return this.originalCapital;
    }

    public boolean repaid() {
        if (this.getCapitalOutstanding() == 0L) {
            return true;
        }
        if (this.payIndex >= this.capitalSchedule.length) {
            if (this.getCapitalOutstanding() > 1L) {
                throw new RuntimeException("Error in capital repayment " + this.getCapitalOutstanding());
            }
            return true;
        }
        return false;
    }

    public boolean writeOff(long l) {
        this.inWriteOff = true;
        System.out.println("Writing off loan to: " + this.borrower.getName());
        if (l > this.getCapitalOutstanding()) {
            throw new RuntimeException("Loan write-off > remaining capital");
        }
        this.capitalWrittenOff += l;
        if (this.getCapitalOutstanding() == 0L) {
            return true;
        }
        System.out.println(this.Id + " :Partial write-off outstanding = " + this.getCapitalOutstanding() + " written off= " + l);
        return false;
    }

    public void adjustTerms(int n) {
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    public boolean incDefault(int n) {
        ++this.totalDefaults;
        ++this.defaultCount;
        if (this.totalDefaults >= this.DEFAULT_LIMIT) {
            this.inDefault = true;
        }
        return this.defaultCount >= n;
    }

    public boolean maxDefaults() {
        return this.totalDefaults >= this.DEFAULT_LIMIT;
    }

    public void putLoanIntoDefault() {
        this.defaultCount = this.DEFAULT_LIMIT;
        this.totalDefaults = this.DEFAULT_LIMIT;
        this.inDefault = true;
    }

    public boolean hasCollateral() {
        return this.collateral != null;
    }

    public void remove() {
        if (this.getCapitalOutstanding() != 0L) {
            this.printRepayments();
            throw new RuntimeException("capital outstanding on repaid loan: " + this.getCapitalOutstanding());
        }
        for (Account account : this.accountList.values()) {
            account.removeLoan(this);
        }
    }

    public void addAccount(Account account) {
        this.accountList.put(account.getId(), account);
    }

    public boolean negAm() {
        return this.negAm;
    }

    public long getNegamDecrease(long l) {
        return 0L;
    }

    public String toString() {
        return "Loan " + this.Id + " Amount: " + this.capitalAmount + " @ " + this.interestRate + "/" + this.duration + " " + this.loanType.name();
    }

    public String getType() {
        return this.loanType.name();
    }

    public void printRepayments() {
        System.out.println("Repayments:");
        for (int i = 0; i < this.payIndex; ++i) {
            System.out.println("" + this.paidCapital[i] + "  " + this.paidInterest[i]);
        }
    }

    public boolean principalIncreasing() {
        return false;
    }

    public long getPrincipalIncrease() {
        return 0L;
    }

    public void adjustCapital(int n) {
    }

    private static int getLoanId() {
        return lastLoanId++;
    }

    public long totalCapitalSchedule() {
        long l = 0L;
        for (int i = 0; i < this.capitalSchedule.length; ++i) {
            l += this.capitalSchedule[i];
        }
        return l;
    }

    public long totalInterestSchedule() {
        long l = 0L;
        for (int i = 0; i < this.interestSchedule.length; ++i) {
            l += this.interestSchedule[i];
        }
        return l;
    }

    public static enum Type {
        SIMPLE,
        COMPOUND,
        INDEXED,
        INTERBANKLOAN,
        VARIABLE;
        

        private Type() {
        }
    }

}

