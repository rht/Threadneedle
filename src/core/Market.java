/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  com.google.gson.annotations.Expose
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import base.Base;
import com.google.gson.annotations.Expose;
import core.Account;
import core.Agent;
import core.Bank;
import core.Company;
import core.Govt;
import core.Inventory;
import core.Markets;
import core.Person;
import core.Region;
import core.Widget;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import statistics.Statistic;

public class Market
extends Company {
    @Expose
    public int maxEmployees = 1;
    @Expose
    public boolean bidEqualsAsk = false;
    @Expose
    public int maxInventory = 20;
    @Expose
    public int ttl = -1;
    @Expose
    public int minSpread = 1;
    @Expose
    public int maxSpread = 5;
    @Expose
    public int minCapital = 200;
    @Expose
    public boolean payDividend = false;
    public boolean useLoan = false;
    public Inventory inventory;
    protected long bidPrice;
    protected long sellPrice;
    protected long spread;
    protected boolean bought = false;
    protected boolean sold = false;
    protected int salesTax = 0;
    protected int cashBuffer = 4;
    private int totalSaleValue = 0;
    private int totalPurchaseValue = 0;
    private int totalQuantitySold = 0;
    public int s_soldLastRound = 0;
    public int s_soldThisRound = 0;
    public int s_maxInventory = 0;
    public Statistic s_inventory;
    public Statistic s_sellprice;
    public Statistic s_bidprice;

    public Market(String string, Govt govt, Bank bank, HashMap<String, String> hashMap) {
        this(string, hashMap.get("product"), govt, bank, Long.parseLong(hashMap.get("initialDeposit")));
    }

    public Market(String string, String string2, Govt govt, Bank bank, long l) {
        super(string, l, govt, bank);
        System.out.println(string + " " + this.getAccount());
        this.product = string2.trim();
        this.inventory = new Inventory(null, true, false);
        this.setProduct();
        this.bidPrice = 4L;
        this.sellPrice = 5L;
        this.offeredSalary = 1L;
    }

    public Market() {
        this.inventory = new Inventory(this.product, true, false);
        this.bidPrice = 4L;
        this.sellPrice = 5L;
        this.offeredSalary = 1L;
        if (!this.regionName.equals("")) {
            this.region = this.govt.regions.get(this.regionName);
        }
    }

    public void setProduct() {
        if (this.inventory.product == null) {
            this.inventory.product = this.product;
            this.s_inventory = Statistic.getStatistic((String)this.getProduct(), (String)"inventory", (Statistic.Type)Statistic.Type.SINGLE);
            this.s_sellprice = Statistic.getStatistic((String)(this.getProduct() + ":ask-price"), (String)"prices", (Statistic.Type)Statistic.Type.SINGLE);
            this.s_bidprice = Statistic.getStatistic((String)(this.getProduct() + ":bid-price"), (String)"prices", (Statistic.Type)Statistic.Type.SINGLE);
        } else {
            System.out.println("Product is already set in " + this.name);
        }
        this.spread = this.minSpread;
    }

    public void setTTL(int n) {
        this.ttl = n;
        this.inventory.setTTL(n);
    }

    public Market reset(Govt govt, Bank bank, Markets markets) {
        System.out.println("Re-implement reset");
        return null;
    }

    public String getProduct() {
        return this.inventory.product;
    }

    public long getBidPrice() {
        return this.bidPrice;
    }

    public long getAskPrice() {
        return this.sellPrice;
    }

    public long getMaxLot() {
        if (this.inventory.getTotalItems() >= (long)this.maxInventory) {
            this.adjustPrices(-1L);
            return 0L;
        }
        long l = this.maxInventory == 0 ? this.getDeposit() / this.bidPrice : (long)this.maxInventory - this.inventory.getTotalItems();
        return Math.min(this.getDeposit() / this.bidPrice, l);
    }

    public long resetTotalSaleValue() {
        long l = this.totalSaleValue + this.totalPurchaseValue;
        this.totalPurchaseValue = 0;
        this.totalSaleValue = 0;
        return l;
    }

    public long resetTotalQuantitySold() {
        long l = this.totalQuantitySold;
        this.totalQuantitySold = 0;
        return l;
    }

    public long sell(Widget widget, long l, Account account) {
        if (this.inventory.getTotalItems() >= (long)this.maxInventory) {
            System.out.println(this.name + " unable to buy - inventory limit reached :" + this.maxInventory);
            ++this.s_maxInventory;
            return 0L;
        }
        long l2 = l == -1L ? this.bidPrice : l;
        if (widget.quantity() <= this.getMaxLot()) {
            if (!this.getAccount().transfer(account, l2 * widget.quantity(), "sale to market: " + this.product)) {
                System.err.println("Sell failed - insufficient funds " + l2 * widget.quantity());
                return -1L;
            }
            this.inventory.add(widget);
            this.bought = true;
            this.totalPurchaseValue = (int)((long)this.totalPurchaseValue + widget.quantity() * l2);
            return l2;
        }
        System.err.println("*** " + this.name + " Sell failed - insufficient funds " + l2 * widget.quantity());
        this.adjustPrices(-1L);
        return 0L;
    }

    public Inventory buy(long l, Account account) {
        return this.buy(-1L, l, account);
    }

    public Widget buy(Widget widget, Agent agent, long l) {
        throw new RuntimeException("Not supported for class Market");
    }

    public Inventory buy(long l, long l2, Account account) {
        long l3;
        long l4;
        Inventory inventory = new Inventory(this.inventory.product, this.inventory.lifetime, this.inventory.unique);
        long l5 = l2;
        if (this.inventory.getTotalItems() == 0L) {
            return null;
        }
        if (this.inventory.getTotalItems() < l2) {
            l5 = this.inventory.getTotalItems();
        }
        if (l == -1L) {
            l3 = this.sellPrice * (long)this.salesTax / 100L;
            l4 = this.sellPrice + l3;
        } else {
            System.out.println("Warning: buyer specified price");
            l3 = l * (long)this.salesTax / 100L;
            l4 = l + l3;
        }
        if (account.getDeposit() > l4 * l5) {
            if (!account.transfer(this.getAccount(), l4 * l5, "purchase from market: " + this.product)) {
                return null;
            }
            this.totalSaleValue = (int)((long)this.totalSaleValue + l4 * l5);
            this.totalQuantitySold = (int)((long)this.totalQuantitySold + l5);
            this.s_soldThisRound = (int)((long)this.s_soldThisRound + l5);
            this.s_income.add(l4 * l5);
            if (this.salesTax > 0) {
                this.transfer((long)this.salesTax * l5, this.govt, "Sales tax: " + this.salesTax + " on " + l5 + " " + this.getProduct());
            }
            Base.DEBUG((String)("Market:" + this.inventory.product + " sold #" + l5 + " @ $" + this.sellPrice + " to " + account.getName() + " [" + this.getAccount().getDeposit() + "]"));
            this.inventory.remove(l5, inventory);
            this.sold = true;
            Iterator<Widget> iterator = inventory.getIterator();
            while (iterator.hasNext()) {
                Widget widget = iterator.next();
                widget.lastSoldPrice = l4;
            }
            return inventory;
        }
        return null;
    }

    private void adjustPrices() {
        if (this.inventory.getTotalItems() > this.s_inventory.get(2)) {
            if (this.bidPrice > 1L) {
                Base.DEBUG((String)("Inventory is growing - reducing prices: " + this.inventory.getTotalItems() + " " + this.s_inventory.get(2) + " Bid: " + this.bidPrice + " Sell:" + this.sellPrice));
                this.adjustPrices(-1L);
            }
        } else if (this.inventory.getTotalItems() < this.s_inventory.get()) {
            long l = (long)this.maxInventory - this.inventory.getTotalItems();
            if (l < 3L) {
                l = 3L;
            }
            if (this.getDeposit() < this.bidPrice * l) {
                Base.DEBUG((String)"Blocking price increase on funding issues: ");
                Base.DEBUG((String)("     Available:  " + this.getDeposit()));
                Base.DEBUG((String)("     Needed   :  " + ((long)this.maxInventory - this.inventory.getTotalItems()) + " * " + this.bidPrice));
            } else {
                Base.DEBUG((String)"Inventory is shrinking - increasing prices");
                this.adjustPrices(1L);
            }
        } else if (this.inventory.getTotalItems() == 0L) {
            this.adjustPrices(1L);
        } else {
            this.adjustPrices(-1L);
        }
        this.s_sellprice.add(this.sellPrice);
        this.s_bidprice.add(this.bidPrice);
    }

    private boolean adjustPrices(long l) {
        long l2;
        if (this.bidPrice + l < 1L) {
            return false;
        }
        long l3 = (long)this.maxInventory - this.inventory.getTotalItems();
        if (l3 < 3L) {
            l3 = 3L;
        }
        if ((l2 = this.getDeposit() - l3 * this.bidPrice) < this.getDeposit() / (long)this.cashBuffer && l >= 1L) {
            return false;
        }
        this.bidPrice += l;
        this.sellPrice += l;
        return true;
    }

    private void increaseSpread(int n) {
        if (this.spread + (long)n < (long)this.maxSpread) {
            this.spread += (long)n;
            this.sellPrice = this.bidPrice + this.spread;
        } else {
            this.spread = this.maxSpread;
        }
    }

    private void decreaseSpread(int n) {
        this.spread = this.spread - (long)n > (long)this.minSpread ? (this.spread -= (long)n) : (long)this.minSpread;
        this.sellPrice = this.bidPrice + this.spread;
    }

    public long getSpread() {
        return this.sellPrice - this.bidPrice;
    }

    @Override
    public void evaluate() {
    }

    @Override
    protected void evaluate(boolean bl, int n) {
        if (this.employees.size() < this.maxEmployees) {
            this.hireEmployee();
        }
        Collections.shuffle(this.employees, Base.random);
        Iterator<Person> iterator = this.employees.iterator();
        while (iterator.hasNext()) {
            Person person = (Person)iterator.next();
            if (this.getAccount().getDeposit() - person.getSalary() > 2L * this.bidPrice) {
                person.paySalary(this.getAccount());
                Base.DEBUG((String)(this.name + " Paid salary to " + person.name + "/" + person.getSalary()));
                continue;
            }
            Base.DEBUG((String)("" + n + " Market: " + this.inventory.product + " Unable to pay salary:" + person.name + " [" + this.getAccount().getDeposit() + "/" + person.getSalary() + "]"));
            this.fireEmployee(person, iterator);
            if (this.offeredSalary <= (long)this.govt.minWage) continue;
            --this.offeredSalary;
        }
        this.adjustPrices();
        if (this.getAccount().incoming - this.getAccount().outgoing < 0L) {
            this.increaseSpread(1);
        }
        this.payTax(this.govt.corporateTaxRate, this.govt.corporateCutoff);
        if (this.payDividend && (double)this.getAccount().getDeposit() > (double)this.minCapital * 1.1 && this.employees.size() > 0) {
            long l = (this.getAccount().getDeposit() - (long)this.minCapital) / (long)this.employees.size();
            for (Person person : this.employees) {
                this.getAccount().transfer(person.getAccount(), l, "Market Dividend (" + this.name + ")");
            }
        }
        if (bl) {
            this.print();
        }
        this.s_inventory.add(this.inventory.getTotalItems());
        this.s_soldLastRound = this.s_soldThisRound;
        this.s_soldThisRound = 0;
        this.sold = false;
        this.bought = false;
        if (this.ttl > 0) {
            this.inventory.expire();
        }
    }

    public Widget getLowestPrice() {
        if (this.inventory.size() == 0L) {
            return null;
        }
        if (this.inventory.size() == 1L) {
            return this.inventory.getFirst();
        }
        Collections.sort(this.inventory.inventory, new Comparator<Widget>(){

            @Override
            public int compare(Widget widget, Widget widget2) {
                return (int)(widget.price - widget2.price);
            }
        });
        return this.inventory.getFirst();
    }

    public Long getTotalItems() {
        return this.inventory.getTotalItems();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getCurrentSetup() {
        return this.name + " " + this.product + " " + this.getBankName() + ": " + this.getDeposit();
    }

    public void print() {
        System.out.println("Market " + this.name + ": " + this.inventory.product);
        for (Person person : this.employees) {
            System.out.println(person.Id + " " + person.getSalary());
        }
    }

}

