/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  base.Base$Time
 *  com.google.gson.annotations.Expose
 */
package core;

import base.Base;
import com.google.gson.annotations.Expose;
import core.Account;
import core.Agent;
import core.Bank;
import core.Company;
import core.Govt;
import core.Loan;
import core.Person;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;

public class Borrower
extends Person {
    @Expose
    public long loanAmount;
    @Expose
    public Loan.Type loanType;
    @Expose
    public String lendername;
    @Expose
    public int loanDuration;
    @Expose
    public int borrowWindow = 1;
    @Expose
    public boolean bankEmployee = false;
    public Company lender = null;

    @Override
    protected void evaluate(boolean bl, int n) {
        if (n % this.borrowWindow == 0 && this.getAccount().debtOutstanding() == 0) {
            if (this.loanAmount > 0L) {
                if (this.getAccount().debts.size() != 0) {
                    this.getAccount().audit();
                    System.out.println(this.getAccount().debtOutstanding());
                    throw new RuntimeException("too many loans");
                }
                if (((Bank)this.lender).requestLoan(this.getAccount(), this.loanAmount, this.loanDuration, Base.Time.MONTH, 1, this.loanType) == null) {
                    Base.DEBUG((String)(this.name + " @ " + this.getAccount().bank + " received loan for: " + this.loanAmount + " from " + this.lender.name));
                } else {
                    Base.DEBUG((String)(this.name + "***  loan request refused for " + this.loanAmount));
                }
            } else {
                Base.DEBUG((String)(this.name + " loan amount " + this.loanAmount + " for " + this.loanDuration));
            }
        }
        if (this.loanPaymentDue()) {
            if (this.getDeposit() <= this.getAccount().getNextRepayment()) {
                if (this.getDeposit() > 0L) {
                    this.setSalary(this.getAccount().getNextRepayment() - this.getDeposit());
                } else if (this.getDeposit() == 0L) {
                    this.setSalary(this.getAccount().getNextRepayment());
                } else {
                    throw new RuntimeException("Deposit < 0 in evaluate: " + this.getDeposit());
                }
                this.paySalary(this.employer.getAccount(), this.getSalary());
            }
            this.payDebt();
        }
        super.evaluate(bl, n);
    }

    public Borrower(String string, Govt govt, Bank bank, long l) {
        super(string, govt, bank, l);
    }

    public Borrower() {
    }

    public void setLender(Company company) {
        this.lender = company;
        this.lendername = company.name;
    }
}

