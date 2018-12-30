/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  base.Base$Time
 */
package core;

import base.Base;
import core.Agent;
import core.Employee;

public class Widget
implements Comparable<Widget> {
    String name;
    protected int wid;
    boolean consumable;
    int created;
    int ttl;
    public long quantity;
    public long lastSoldPrice;
    public int lastSoldWhen;
    long price;
    Agent owner;

    public Widget(String string, int n, long l) {
        this.name = string;
        this.wid = Base.assignWidgetID();
        this.created = Base.step;
        this.ttl = n * Base.Time.MONTH.period();
        this.quantity = l;
        if (this.quantity <= 0L) {
            throw new RuntimeException("Incorrect quantity for widget " + this.quantity);
        }
    }

    public Widget(String string, int n) {
        this(string, n, 1L);
    }

    public Widget(String string, int n, Widget widget, long l) {
        this(string, n, l);
        widget.quantity -= l;
        if (widget.quantity <= 0L || l <= 0L) {
            throw new RuntimeException("Incorrect quantity for widget " + widget.quantity + " newQ: " + l);
        }
    }

    @Override
    public int compareTo(Widget widget) {
        return Long.compare(this.price, widget.price);
    }

    public Widget split(long l) {
        if (this.quantity < l) {
            throw new RuntimeException("Insufficient quantity for split: " + this.quantity + " < " + l);
        }
        Widget widget = new Widget(this.name, this.ttl, l);
        widget.created = this.created;
        this.quantity -= l;
        return widget;
    }

    public boolean equals(Object object) {
        if (object instanceof Widget) {
            Widget widget = (Widget)object;
            return this.name.equals(widget.name) && this.ttl == widget.ttl && this.wid == widget.wid && this.created == widget.created && this.quantity == widget.quantity;
        }
        return false;
    }

    public boolean hasTTL() {
        return this.ttl != -1;
    }

    public void merge(Widget widget) {
        if (widget instanceof Employee) {
            throw new RuntimeException("People can't be merged");
        }
        if (this == widget) {
            throw new RuntimeException("Merging derselbe widget: " + widget + " " + this);
        }
        if (this.name.equals(widget.name) && this.ttl == widget.ttl) {
            this.quantity += widget.quantity;
        } else {
            throw new RuntimeException("Attempt to merge un-identical widgets " + (this.name.equals(widget.name) ? "" : new StringBuilder().append(this.name).append("!=").append(widget.name).toString()) + (this.ttl == widget.ttl ? "" : new StringBuilder().append(this.ttl).append("!=").append(widget.ttl).toString()));
        }
        widget.quantity = 0L;
    }

    public long quantity() {
        return this.quantity;
    }

    public int hashCode() {
        int n = 1;
        n = n * 31 + this.name.hashCode();
        n = n * 31 + this.wid;
        n = n * 31 + this.created;
        n = n * 31 * this.ttl;
        return n;
    }

    public String toString() {
        return this.name + " : " + this.quantity + " (Created: " + this.created + " / Lifetime: " + this.ttl + ")";
    }
}

