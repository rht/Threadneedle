/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Widget;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class Inventory {
    public LinkedList<Widget> inventory = new LinkedList();
    private LinkedList<Long> totalItemsHistory = new LinkedList();
    private int checkCount = 0;
    private int growthBound = 1;
    public String product;
    public boolean lifetime;
    public boolean unique;

    public Inventory(String string, boolean bl, boolean bl2) {
        this.product = string;
        this.lifetime = bl;
        this.unique = bl2;
    }

    public Inventory() {
        this.lifetime = true;
        this.unique = true;
    }

    public void audit() {
        System.out.println("===  Audit " + this.product + " size: " + this.getTotalItems() + "===");
        for (Widget widget : this.inventory) {
            System.out.println(widget + " " + widget.getClass().getName() + " " + widget.quantity());
        }
        System.out.println("");
    }

    private void adjustTotalItems() {
        this.totalItemsHistory.addFirst(this.getTotalItems());
        if (this.totalItemsHistory.size() > 10) {
            this.totalItemsHistory.removeLast();
        }
    }

    public long getTotalItems() {
        long l = 0L;
        for (Widget widget : this.inventory) {
            l += widget.quantity();
        }
        return l;
    }

    public long size() {
        return this.inventory.size();
    }

    public boolean growing() {
        System.out.println("\n" + this.product + ": Growing - history: " + this.getTotalItems());
        for (int i = 0; i < this.totalItemsHistory.size(); ++i) {
            System.out.print(this.totalItemsHistory.get(i) + " ");
        }
        System.out.println("getFirst(): " + this.totalItemsHistory.getFirst() + " > " + this.totalItemsHistory.get(1));
        if (this.totalItemsHistory.size() <= 3) {
            return false;
        }
        System.out.println("-------- ");
        return this.totalItemsHistory.getFirst() > this.totalItemsHistory.get(1);
    }

    public boolean shrinking() {
        System.out.println(this.product + ": shrinking: " + this.getTotalItems());
        for (int i = 0; i < this.totalItemsHistory.size(); ++i) {
            System.out.print(this.totalItemsHistory.get(i) + " ");
        }
        System.out.println("getFirst(): " + this.totalItemsHistory.getFirst() + " > " + this.totalItemsHistory.get(1));
        if (this.totalItemsHistory.size() <= 3) {
            return false;
        }
        System.out.println("-------- ");
        return this.totalItemsHistory.size() == 0 || this.totalItemsHistory.getFirst() < this.totalItemsHistory.get(1);
    }

    public void add(Widget widget) {
        if (!widget.name.equals(this.product)) {
            throw new RuntimeException("Invalid Widget type in add " + widget.name + " != " + this.product);
        }
        if (!this.lifetime && widget.ttl != -1) {
            throw new RuntimeException("TTL for Widget in non-ttl inventory");
        }
        if (this.unique || this.lifetime || this.inventory.size() == 0) {
            this.inventory.add(widget);
        } else {
            this.inventory.getFirst().merge(widget);
            if (this.inventory.getFirst().lastSoldWhen < widget.lastSoldWhen) {
                this.inventory.getFirst().lastSoldWhen = widget.lastSoldWhen;
            }
        }
        this.adjustTotalItems();
    }

    public void sort() {
        Collections.sort(this.inventory, Widget::compareTo);
    }

    public void merge(Inventory inventory) {
        if (!this.product.equals(inventory.product)) {
            throw new RuntimeException("Invalid Widget type in add " + inventory.product + " != " + this.product);
        }
        this.inventory.addAll(inventory.inventory);
        this.adjustTotalItems();
    }

    public boolean consume(int n) {
        return this.remove(n, null);
    }

    public Widget getFirst() {
        return this.inventory.getFirst();
    }

    public Widget getLast() {
        return this.inventory.getLast();
    }

    public Iterator<Widget> getIterator() {
        return this.inventory.listIterator();
    }

    public void setTTL(int n) {
        for (Widget widget : this.inventory) {
            widget.ttl = n;
        }
    }

    public void expire() {
        Iterator<Widget> iterator = this.inventory.iterator();
        while (iterator.hasNext()) {
            Widget widget = iterator.next();
            if (--widget.ttl > 0) continue;
            iterator.remove();
        }
    }

    public Widget remove() {
        return this.remove(1L);
    }

    public Widget remove(Widget widget) {
        if (this.inventory.remove(widget)) {
            return widget;
        }
        return null;
    }

    public Widget remove(long l) {
        Inventory inventory = new Inventory(this.product, this.lifetime, this.unique);
        assert (!this.lifetime);
        if (!this.remove(l, inventory)) {
            return null;
        }
        return inventory.getFirst();
    }

    public boolean remove(long l, Inventory inventory) {
        if (l > this.getTotalItems()) {
            System.out.println("Error: Request to remove more than in inventory:" + this.getTotalItems());
            System.out.println("      " + this.product + "(" + l + ")");
            return false;
        }
        if (l == 0L) {
            System.out.println("Error: request to remove 0 items");
            return false;
        }
        if (inventory == this) {
            System.out.println("newinv must be different inventory collection");
        } else {
            Widget widget = this.inventory.removeFirst();
            if (widget.quantity() == l) {
                if (inventory != null) {
                    inventory.add(widget);
                }
                this.adjustTotalItems();
                return true;
            }
            if (widget.quantity() > l) {
                if (inventory != null) {
                    inventory.add(widget.split(l));
                } else {
                    widget.split(l);
                }
                this.inventory.addFirst(widget);
                this.adjustTotalItems();
                return true;
            }
            if (widget.quantity() < l) {
                if (inventory != null) {
                    inventory.add(widget);
                }
                this.remove(l - widget.quantity(), inventory);
                this.adjustTotalItems();
            }
        }
        return true;
    }

    private int checkTotalSize() {
        int n = 0;
        for (Widget widget : this.inventory) {
            n = (int)((long)n + widget.quantity());
        }
        return n;
    }
}

