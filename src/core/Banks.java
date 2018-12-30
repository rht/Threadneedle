/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableMap
 */
package core;

import core.Bank;
import core.CentralBank;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class Banks {
    public CentralBank centralBank = null;
    private HashMap<String, Bank> banklist = new HashMap(20);
    public ObservableMap<String, Bank> obsBanks = FXCollections.observableMap(this.banklist);

    public int noBanks() {
        return this.banklist.size();
    }

    public Bank getBank(String string) {
        if (this.centralBank != null && this.centralBank.name.equals(string)) {
            return this.centralBank;
        }
        return this.banklist.get(string);
    }

    public void addBank(Bank bank) {
        if (bank instanceof CentralBank) {
            this.centralBank = (CentralBank)bank;
        } else if (this.banklist.get(bank.name) == null) {
            this.obsBanks.put((Object)bank.name, (Object)bank);
        }
        if (this.centralBank != null) {
            this.centralBank.addReserveAccount(bank);
        }
    }

    public void addCentralBank(CentralBank centralBank) {
        this.obsBanks.put((Object)centralBank.name, (Object)centralBank);
    }

    public int getTotalBankDeposits() {
        int n = 0;
        for (Bank bank : this.banklist.values()) {
            if (bank instanceof CentralBank) continue;
            n += bank.getTotalDeposits();
        }
        return n;
    }

    public int getTotalBankLoans() {
        int n = 0;
        for (Bank bank : this.banklist.values()) {
            if (bank instanceof CentralBank) continue;
            n = (int)((long)n + bank.getTotalLoans());
        }
        return n;
    }

    public HashMap<String, Bank> getBankList() {
        return this.banklist;
    }

    public void clear() {
        this.banklist.clear();
    }

    public void report() {
        System.out.println("Banks Report:");
        System.out.println("Central Bank: " + this.centralBank);
        this.banklist.values().forEach(System.out::println);
    }
}

