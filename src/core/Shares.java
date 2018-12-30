/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Agent;
import core.Company;
import core.Widget;
import java.io.PrintStream;
import java.util.LinkedList;

public class Shares
extends Widget {
    long issuePrice;
    Company issuer;
    Agent owner;
    LinkedList<Shares> masterList;

    public Agent getOwner() {
        return this.owner;
    }

    public Shares(String string, long l, long l2, Company company, LinkedList<Shares> linkedList) {
        super(string, -1, l);
        if (this.quantity <= 0L) {
            throw new RuntimeException("Invalid quantity of Shares: " + l);
        }
        this.issuePrice = l2;
        this.issuer = company;
        this.masterList = linkedList;
        this.owner = company;
        linkedList.add(this);
    }

    public Shares(String string, long l, long l2, Company company, LinkedList<Shares> linkedList, Shares shares, long l3) {
        super(string, -1, shares, l3);
        if (this.quantity <= 0L || l3 <= 0L) {
            throw new RuntimeException("Invalid quantity of Shares: " + l);
        }
        this.issuePrice = l2;
        this.issuer = company;
        this.masterList = linkedList;
        linkedList.add(this);
    }

    public boolean issued() {
        return this.owner != this.issuer;
    }

    public void transfer(Agent agent) {
        this.owner = agent;
        agent.addInvestment(this);
    }

    public double getDividend(double d) {
        System.out.println("div: " + this.quantity + " " + d + " " + this.issuePrice + " owned by: " + this.owner.name);
        return (double)(100L * this.quantity) * d * (double)this.issuePrice / 100.0;
    }

    @Override
    public Shares split(long l) {
        if (this.quantity() < l || l == 0L) {
            throw new RuntimeException("Insufficient quantity for split: " + this.quantity() + "<" + l);
        }
        Shares shares = new Shares(this.name, l, this.issuePrice, this.issuer, this.masterList, this, l);
        shares.created = this.created;
        shares.owner = this.owner;
        return shares;
    }

    @Override
    public String toString() {
        return this.name + " : " + this.quantity + " Issuer : " + this.issuer + " Owner  : " + this.owner;
    }
}

