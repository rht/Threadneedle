/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import base.Base;
import core.Agent;
import core.Bank;
import core.Employee;
import core.Govt;
import core.Inventory;
import core.Market;
import core.Person;
import core.Widget;
import java.awt.Color;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import statistics.Statistic;

public class LabourMarket
extends Market {
    private int maxInventory = -1;
    private int MAX_UNEMPLOYMENT_TIME = 5;
    private int totalSaleValue = 0;
    private int totalQuantitySold = 0;
    public Statistic s_unemployed;

    public LabourMarket(String string, String string2, Govt govt, Bank bank) {
        super(string, string2, govt, bank, 0L);
        this.inventory = new Inventory(string2, true, true);
        this.bidPrice = Integer.MAX_VALUE;
        this.sellPrice = Integer.MAX_VALUE;
        this.myColor = Color.gray;
        if (this.getDeposit() > 0L) {
            throw new RuntimeException("Sanity failed: LabourMarket has non-zero deposit: " + this.getDeposit());
        }
        this.s_unemployed = Statistic.getStatistic((String)"unemployed", (String)"unemployed", (Statistic.Type)Statistic.Type.SINGLE);
    }

    public LabourMarket() {
        this.bidPrice = Integer.MAX_VALUE;
        this.sellPrice = Integer.MAX_VALUE;
        this.s_unemployed = Statistic.getStatistic((String)"unemployed", (String)"unemployed", (Statistic.Type)Statistic.Type.COUNTER);
    }

    public void init() {
        this.bidPrice = Integer.MAX_VALUE;
        this.sellPrice = Integer.MAX_VALUE;
    }

    @Override
    public void setProduct() {
        if (this.inventory.product == null) {
            this.inventory.product = this.product;
        }
    }

    public void sell(Employee employee) {
        if (employee.person.employer == employee.person) {
            return;
        }
        if (employee.person.getSalary() < this.sellPrice || this.sellPrice == -1L) {
            this.sellPrice = employee.person.getSalary() < (long)this.govt.minWage ? (long)this.govt.minWage : employee.person.getSalary();
        }
        this.bidPrice = this.sellPrice;
        if (this.sellPrice < 1L) {
            System.out.println("==" + employee.person.name);
        }
        employee.person.unemployed = true;
        employee.person.unemployedTime = 0;
        this.inventory.add(employee);
    }

    public boolean contains(Person person) {
        for (Widget widget : this.inventory.inventory) {
            if (((Employee)widget).person != person) continue;
            return true;
        }
        return false;
    }

    public Person hire(Person person) {
        Iterator<Widget> iterator = this.inventory.getIterator();
        while (iterator.hasNext()) {
            Employee employee = (Employee)iterator.next();
            if (employee.person != person) continue;
            iterator.remove();
            return person;
        }
        return null;
    }

    public Inventory hire(long l, Bank bank, String string) {
        Inventory inventory = this.buy(l, bank, string);
        if (inventory == null && l > this.sellPrice) {
            this.sellPrice = this.bidPrice = l;
        }
        return inventory;
    }

    public Inventory hire(long l, String string, Class<? extends Person> class_) {
        Inventory inventory = new Inventory(this.inventory.product, true, true);
        if (this.inventory.size() == 0L) {
            Base.DEBUG((String)("Inventory:hire() - None exist!" + class_.toString()));
            return null;
        }
        this.inventory.sort();
        Iterator<Widget> iterator = this.inventory.getIterator();
        while (iterator.hasNext()) {
            Employee employee = (Employee)iterator.next();
            if (!employee.person.getClass().isAssignableFrom(class_) || employee.person.desiredSalary > l || string != null && !employee.person.getRegionName().equals(string)) continue;
            iterator.remove();
            employee.person.unemployed = false;
            this.bidPrice = employee.person.getSalary();
            inventory.add(employee);
            return inventory;
        }
        return null;
    }

    public Inventory buy(long l, Bank bank, String string) {
        Inventory inventory = new Inventory(this.inventory.product, true, true);
        if (this.inventory.size() == 0L) {
            return null;
        }
        this.inventory.sort();
        Iterator<Widget> iterator = this.inventory.getIterator();
        while (iterator.hasNext()) {
            Employee employee = (Employee)iterator.next();
            if (l != -1L && employee.person.desiredSalary > l && employee.person.getDebt() <= 0L && employee.person.unemployedTime <= this.MAX_UNEMPLOYMENT_TIME || string != null && !employee.person.getRegionName().equals(string) || bank != null && bank != employee.person.getBank()) continue;
            iterator.remove();
            employee.person.unemployed = false;
            employee.person.unemployedTime = 0;
            this.bidPrice = employee.person.getSalary();
            inventory.add(employee);
            return inventory;
        }
        return null;
    }

    public void adjustPrices() {
        this.inventory.sort();
        Iterator<Widget> iterator = this.inventory.getIterator();
        while (iterator.hasNext()) {
            Employee employee = (Employee)iterator.next();
        }
        if (this.inventory.size() == 0L) {
            this.bidPrice = this.sellPrice = (long)this.govt.minWage;
        } else {
            this.bidPrice = this.sellPrice = ((Employee)this.inventory.getFirst()).person.desiredSalary;
            if (this.sellPrice < (long)this.govt.minWage) {
                this.bidPrice = this.sellPrice = (long)this.govt.minWage;
            }
        }
    }

    public long getLowestSalary() {
        return this.getAskPrice();
    }

    public boolean hasWorkers() {
        assert (this.inventory.size() == 0L && this.sellPrice == -1L);
        return this.inventory.size() != 0L;
    }

    public void setPrice(long l) {
        this.bidPrice = this.sellPrice = l;
    }

    @Override
    public void evaluate() {
    }

    @Override
    public void evaluate(boolean bl, int n) {
        this.sold = false;
        this.bought = false;
        this.s_unemployed.add(this.inventory.size());
        for (int i = 0; i < this.inventory.inventory.size(); ++i) {
            Employee employee = (Employee)this.inventory.inventory.get(i);
            ++employee.person.unemployedTime;
            employee.person.evaluate(bl, n);
        }
        if (bl) {
            this.print();
        }
    }

    public long totalAvailableWorkers() {
        return this.inventory.size();
    }
}

