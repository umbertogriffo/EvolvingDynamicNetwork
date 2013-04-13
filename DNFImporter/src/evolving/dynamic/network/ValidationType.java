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

public final class ValidationType {

    public static boolean isIntNumber(String num) {
        try {
            Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isFloatNumber(String num) {
        try {
            Float.parseFloat(num);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isLongNumber(String num) {
        try {
            Long.parseLong(num);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isDoubleNumber(String num) {
        try {
            Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
