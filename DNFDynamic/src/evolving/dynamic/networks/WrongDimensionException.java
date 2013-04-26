/*
 Authors : Umberto Griffo <umberto.griffo@gmail.com>
 Linkedin : it.linkedin.com/in/umbertogriffo
 Twitter : @UmbertoGriffo
 
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. 
 You can obtain a copy of the License at http://www.gnu.org/licenses/gpl-3.0.txt.

 */
package evolving.dynamic.networks;

public class WrongDimensionException extends RuntimeException {

    public WrongDimensionException() {
        super();
    }

    public WrongDimensionException(String message) {
        super(message);
    }

    public WrongDimensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongDimensionException(Throwable cause) {
        super(cause);
    }
}
