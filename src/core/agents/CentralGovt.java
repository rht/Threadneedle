/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Account;
import core.Agent;
import core.Bank;
import core.Banks;
import core.CentralBank;
import core.Govt;
import core.Treasury;
import java.io.PrintStream;
import java.util.HashMap;

public class CentralGovt
extends Govt {
    protected Bank bank = null;
    public boolean extendedMoneySupply = false;
    public boolean nonCashIncome = false;

    CentralGovt(String string, String string2, long l) {
        super(string, string2, l);
        this.govt = this;
        this.Id = 0;
        this.bankname = string2;
    }

    CentralGovt() {
        this.Id = 0;
        this.govt = this;
    }

    @Override
    public void evaluate(boolean bl, int n) {
        super.evaluate(bl, n);
    }

    @Override
    public long getDepositSupply() {
        return this.banks.getTotalBankDeposits();
    }

    public int getCapitalLimit() {
        if (this.centralbank != null) {
            return this.centralbank.getCapitalReserveLimit();
        }
        return 0;
    }

    @Override
    public HashMap<String, Bank> getBankList() {
        return this.banks.getBankList();
    }

    @Override
    public Bank lookupBank(String string) {
        if (this.banks == null) {
            System.out.println("** List of Banks has not been initialised **");
        }
        return this.banks.getBank(string);
    }

    public Treasury buyTreasury(int n, int n2, Agent agent, Account account) {
        return null;
    }

    @Override
    public boolean getNonCashIncome() {
        return this.centralbank.nonCashIncome;
    }

    @Override
    public void print(String string) {
        System.out.println(string + ":" + this.name);
    }
}

