/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 */
package core;

import base.Base;
import core.Account;
import core.Agent;
import core.CentralGovt;
import core.Govt;
import core.Loan;
import core.Person;
import core.Treasury;
import java.awt.Color;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class BaselGovt
extends CentralGovt {
    public double debtceiling = 0.0;
    public int minLoanSize = 12;
    public int maxLoanSize = 1200;
    public HashMap<Integer, Treasury> treasuries;
    public int debtIncomeDelta = 0;
    int totalNextRepayment = 0;

    BaselGovt(String string, String string2, long l) {
        super(string, string2, l);
        this.maxLoanSize = (int)this.debtceiling;
        this.hasCentralBank = true;
        this.treasuries = new HashMap(100);
    }

    BaselGovt() {
        this.hasCentralBank = true;
        this.treasuries = new HashMap(100);
    }

    public Treasury sellInvestment(Agent agent, int n, int n2, String string) {
        System.out.println("sale of treasury to " + agent.name + " " + n);
        if (!this.checkTreasuryAvailability(n)) {
            return null;
        }
        if (agent.getDeposit() < (long)n) {
            System.out.println("** Error: insufficient funds in sellInvestment");
            System.out.println(this.name + " " + agent.name);
            return null;
        }
        Treasury treasury = new Treasury(this.govt.getAccount(), n, this.treasuryRate, n2, Base.step, null);
        assert (this.treasuries.get(treasury.Id) == null);
        treasury.setOwner(agent.Id, agent);
        this.treasuries.put(treasury.Id, treasury);
        agent.getAccount().transfer(this.getAccount(), n, "Treasury sale");
        System.out.println("New Treasury: " + treasury.toString());
        return treasury;
    }

    public boolean checkTreasuryAvailability(int n) {
        if (n >= this.minLoanSize && n <= this.maxLoanSize && this.debtceiling - (double)this.getTotalDebt() >= 0.0) {
            return true;
        }
        if ((double)n >= this.debtceiling) {
            System.out.println("W: Treasury request for more than debt ceiling");
            System.out.println("A: " + n + "DC: " + this.debtceiling);
        }
        return false;
    }

    private void changeTaxRates(int n, int n2) {
        if (this.personalTaxRate + n > 0) {
            this.personalTaxRate += n;
        }
        if (this.corporateTaxRate + n2 > 0) {
            this.corporateTaxRate += n2;
        }
    }

    public int getNextDebtPayment() {
        int n = 0;
        for (Treasury treasury : this.treasuries.values()) {
            n = (int)((long)n + treasury.getPaymentDue());
        }
        return n;
    }

    @Override
    public void evaluate(boolean bl, int n) {
        long l = this.getSalaryBill();
        this.paySalaries();
        if (l < this.getDeposit()) {
            if ((long)this.employees.size() < this.getMaxCivilServants()) {
                this.hireEmployee();
            } else {
                this.increaseSalaries(1L);
            }
        } else {
            this.fireEmployee();
        }
        if ((long)this.totalNextRepayment > this.getDeposit()) {
            this.changeTaxRates(1, 1);
        }
        this.totalNextRepayment = 0;
        Iterator<Treasury> iterator = this.treasuries.values().iterator();
        while (iterator.hasNext()) {
            Treasury treasury = iterator.next();
            if (!treasury.installmentDue()) continue;
            long l2 = treasury.getPaymentDue();
            System.out.println(this.name + " Central Bank Account: " + this.centralBankAccount.getDeposit() + " " + l2);
            if (this.centralBankAccount.getDeposit() > l2) {
                this.centralBankAccount.payLoan(treasury);
                if (treasury.repaid()) {
                    System.out.println("Removing treasury: " + treasury);
                    iterator.remove();
                }
            } else {
                System.out.println("** Govt in default - unable to pay " + treasury);
                this.myColor = Color.RED;
            }
            if (treasury.repaid()) continue;
            this.totalNextRepayment = (int)((long)this.totalNextRepayment + treasury.getPaymentDue());
        }
        super.evaluate(bl, n);
    }

    @Override
    public int getTotalDebt() {
        int n = 0;
        for (Treasury treasury : this.treasuries.values()) {
            n = (int)((long)n + treasury.getCapitalOutstanding());
        }
        return n += this.getAccount().debtOutstanding();
    }

    public int calculateRequiredRevenue() {
        int n = 0;
        for (Treasury object : this.treasuries.values()) {
            n = (int)((long)n + object.getPaymentDue());
        }
        for (Person person : this.employees) {
            n = (int)((long)n + person.getSalary());
        }
        return n;
    }
}

