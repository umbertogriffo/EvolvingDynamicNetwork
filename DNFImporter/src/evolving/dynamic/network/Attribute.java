/*
 Authors : Umberto Griffo <umberto.griffo@gmail.com>
 Linkedin : it.linkedin.com/pub/umberto-griffo/31/768/99
 Twitter : @UmbertoGriffo
 
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. 
 You can obtain a copy of the License at http://www.gnu.org/licenses/gpl-3.0.txt.

 */
package evolving.dynamic.network;

/**
 * Class that represent the node's or edge's attributes
 *
 * @author Umberto Griffo
 */
public class Attribute<T> implements Cloneable {

    private String name;
    private T value;

    public Attribute() {
    }

    public Attribute(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Attribute name: " + name + " value: " + value;
    }

    @Override
    public boolean equals(Object ogg) {
        if (ogg != null && getClass().equals(ogg.getClass())) {
            Attribute p = (Attribute) ogg;
            if ((name.equals(p.name)) && (value.equals(p.value))) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 59 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

    @Override
    public Object clone() {
        Attribute<T> att_clone = new Attribute(this.name, this.value);
        return att_clone;
    }

    public static void main(String[] args) {
        Attribute uno = new Attribute("franco", "gino");
        Attribute due = new Attribute("paolo", 11);
        Attribute tre = new Attribute("cacca", 1.2);
        Attribute quattro = (Attribute) due.clone();

        System.out.println(uno.toString());
        System.out.println(due.toString());
        System.out.println(tre.toString());
        System.out.println(quattro.toString());


    }
}
