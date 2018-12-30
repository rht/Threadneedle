/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Person;
import core.Widget;

class Employee
extends Widget {
    Person person;

    public Employee(Person person, long l, int n, int n2) {
        super("Labour", n);
        this.person = person;
        this.person.desiredSalary = l;
    }

    public long compareTo(Employee employee) {
        return this.person.desiredSalary - employee.person.desiredSalary;
    }

    @Override
    public Employee split(long l) {
        throw new RuntimeException("Split called on Employee (Cannot splinch people, it's cruel)");
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Employee) {
            Employee employee = (Employee)object;
            return this.person == employee.person;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.person.toString();
    }
}

