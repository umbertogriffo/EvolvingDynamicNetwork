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

import org.gephi.io.importer.api.FileType;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.importer.spi.FileImporterBuilder;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * File Importer builder implementation for the DNF file format. The builder is
 * responsible for creating the importer's instances.
 *
 * @author Umberto Griffo
 */
@ServiceProvider(service = FileImporterBuilder.class)
public class DnfImporterBuilder implements FileImporterBuilder {

    public static final String IDENTIFER = "dnf";

    @Override
    //Builds a new file importer instance, ready to be used.
    public FileImporter buildImporter() {
        return new DnfImporter();
    }

    @Override
    //Get default file types this importer can deal with
    public FileType[] getFileTypes() {
        FileType ft = new FileType(".dnf", "DNF format");
        return new FileType[]{ft};
    }

    @Override
    //Returns true if this importer can import fileObject
    public boolean isMatchingImporter(FileObject fileObject) {
        return fileObject.getExt().equalsIgnoreCase(IDENTIFER);
    }

    @Override
    public String getName() {
        return IDENTIFER;
    }
}
