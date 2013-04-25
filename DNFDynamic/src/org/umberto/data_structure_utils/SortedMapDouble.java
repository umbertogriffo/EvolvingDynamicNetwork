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
package org.umberto.data_structure_utils;

import java.util.*;
import org.umberto.data_structure.interfaces.ISortingMapValue;

/**
 *
 * @author gennysoacella
 */
public class SortedMapDouble implements ISortingMapValue {

    private Map map;

    public SortedMapDouble(Map map) {
        this.map = map;
    }

    @Override
    /**
     * Sort a Map by value in ascendant order.
     *
     * @param map map to order
     * @return Ordered Map by Value in descendant order.
     */
    public Map sortByValueAscending() {
        List list = new LinkedList(this.map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                if ((Double) ((Map.Entry) (o1)).getValue() > (Double) ((Map.Entry) (o2)).getValue()) {
                    return 1;
                } else if ((Double) ((Map.Entry) (o1)).getValue() == ((Map.Entry) (o2)).getValue()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    /**
     * Sort a Map by value in descendant order.
     *
     * @param map map to order
     * @return Ordered Map by Value in descendant order.
     */
    public Map sortByValueDescending() {
         List list = new LinkedList(this.map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                if ((Double) ((Map.Entry) (o1)).getValue() < (Double) ((Map.Entry) (o2)).getValue()) {
                    return 1;
                } else if ((Double) ((Map.Entry) (o1)).getValue() == ((Map.Entry) (o2)).getValue()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
