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
package evolving.dynamic.network.metric.InterIntraContactTime;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builder for the {@link Dnf_Inter_Intra_contactTime} statistic.
 *
 * @author Umberto Griffo
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class Dnf_Inter_Intra_contactTimeBuilder implements StatisticsBuilder {
     public static final String NAME = "DNF Inter/Intra-contact time";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Statistics getStatistics() {
       return new Dnf_Inter_Intra_contactTime();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
         return Dnf_Inter_Intra_contactTime.class;
    }
}
