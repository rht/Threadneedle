/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.Expose
 */
package core;

import com.google.gson.annotations.Expose;
import core.Inventory;
import core.Widget;
import java.util.HashMap;
import java.util.LinkedList;

public class Need {
    @Expose
    public String product;
    @Expose
    public int quantity;
    @Expose
    public int storeQ;
    @Expose
    public int consumption;
    @Expose
    public int stepFrequency;
    @Expose
    public boolean useLoan;
    @Expose
    public boolean consumable;
    public long lastPricePaid;
    public Inventory store;

    public Need(String string, int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
        this.product = string;
        this.quantity = n;
        this.storeQ = n2;
        this.stepFrequency = n3;
        this.consumption = n4;
        this.consumable = bl;
        this.useLoan = bl2;
        this.store = new Inventory(this.product, true, false);
    }

    public void init() {
        if (this.store == null) {
            this.store = new Inventory(this.product, true, false);
        }
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this.getClass() != object.getClass()) {
            return false;
        }
        Need need = (Need)object;
        return need.product.equals(this.product) && need.storeQ == this.storeQ && need.quantity == this.quantity && need.consumption == this.consumption && need.consumable == this.consumable && need.useLoan == this.useLoan && need.stepFrequency == this.stepFrequency;
    }

    public String toString() {
        return this.product + " : " + this.quantity + " | " + this.storeQ + " | " + this.consumption;
    }

    public void reset() {
        this.lastPricePaid = 0L;
        this.store = new Inventory(this.product, true, false);
    }

    public long getRequired() {
        long l = (long)this.storeQ - this.store.getTotalItems();
        if (l > (long)this.quantity) {
            return this.quantity;
        }
        return l;
    }

    public long consume() {
        long l;
        long l2 = 0L;
        l2 = l > this.store.getTotalItems() ? -1L * (l - this.store.getTotalItems()) : l;
        if (this.consumable) {
            for (l = (long)this.consumption; l > 0L && this.store.getTotalItems() > 0L; --l) {
                this.store.consume(1);
            }
        }
        this.updateTTL();
        return l2;
    }

    private void updateTTL() {
        for (int i = 0; i < this.store.inventory.size(); ++i) {
            Widget widget = this.store.inventory.get(i);
            if (widget.ttl > 0) {
                --widget.ttl;
            }
            if (widget.ttl != 0) continue;
            this.store.remove(widget);
        }
    }

    public Double getAvgQuantity() {
        return this.quantity / this.stepFrequency;
    }

    public void addAvgQuantity(HashMap<String, Double> hashMap) {
        if (hashMap.containsKey(this.product)) {
            hashMap.put(this.product, hashMap.get(this.product) + this.getAvgQuantity());
        } else {
            hashMap.put(this.product, this.getAvgQuantity());
        }
    }
}

