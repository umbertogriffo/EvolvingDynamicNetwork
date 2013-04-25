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
package org.umberto.data_structure.interfaces;

import java.util.Map;

public interface ISortingMapValue {

    /**
     * Sort a Map by value in descendant order.
     *
     * @param map map to order
     * @return Ordered Map by Value in descendant order.
     */
    Map sortByValueAscending();

    /**
     * Sort a Map by value in ascendant order.
     *
     * @param map map to order
     * @return Ordered Map by Value in descendant order.
     */
    Map sortByValueDescending();
}