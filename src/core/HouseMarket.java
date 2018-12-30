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
import core.Account;
import core.Agent;
import core.Bank;
import core.Govt;
import core.Market;
import core.Widget;
import java.awt.Color;
import java.io.PrintStream;
import java.util.PriorityQueue;
import statistics.Statistic;

public class HouseMarket
extends Market {
    private int maxInventory = -1;
    PriorityQueue<Widget> inventory = new PriorityQueue(10);
    private int totalSaleValue = 0;
    private int totalQuantitySold = 0;

    public HouseMarket(String string, String string2, Govt govt, Bank bank) {
        super(string, string2, govt, bank, 0L);
        this.bidPrice = -1L;
        this.sellPrice = -1L;
        this.useLoan = true;
        this.myColor = Color.blue;
        if (this.getDeposit() > 0L) {
            throw new RuntimeException("Sanity failed: HouseMarket has non-zero deposit: " + this.getDeposit());
        }
        this.s_sellprice = Statistic.getStatistic((String)(this.name + ":ask-price"), (String)"prices", (Statistic.Type)Statistic.Type.SINGLE);
    }

    public HouseMarket() {
        this.useLoan = true;
        this.setProduct();
        this.s_sellprice = Statistic.getStatistic((String)(this.name + "-" + this.getProduct() + ":ask-price"), (String)"prices", (Statistic.Type)Statistic.Type.SINGLE);
    }

    @Override
    public void evaluate() {
    }

    @Override
    public long sell(Widget widget, long l, Account account) {
        widget.price = l;
        widget.owner = account.owner;
        this.inventory.add(widget);
        this.bidPrice = this.sellPrice = this.inventory.peek().price;
        return l;
    }

    @Override
    public Widget buy(Widget widget, Agent agent, long l) {
        if (agent.getDeposit() < l || l < widget.price) {
            return null;
        }
        if (this.inventory.size() == 0) {
            return null;
        }
        widget = this.inventory.poll();
        System.out.println("" + Base.step + ": " + widget.owner.getName() + " sold " + widget.wid + " @ " + widget.price + " to " + agent.getName());
        this.s_sellprice.add(widget.price);
        widget.owner.s_income.add(widget.price);
        agent.transfer(l, widget.owner, "House sale");
        widget.owner = agent;
        if (this.inventory.peek() != null) {
            this.bidPrice = this.sellPrice = this.inventory.peek().price;
        }
        return widget;
    }

    @Override
    public Widget getLowestPrice() {
        return this.inventory.peek();
    }

    @Override
    public void evaluate(boolean bl, int n) {
        this.sold = false;
        this.bought = false;
        if (bl) {
            this.print();
        }
    }

    @Override
    public Long getTotalItems() {
        return this.inventory.size();
    }
}

