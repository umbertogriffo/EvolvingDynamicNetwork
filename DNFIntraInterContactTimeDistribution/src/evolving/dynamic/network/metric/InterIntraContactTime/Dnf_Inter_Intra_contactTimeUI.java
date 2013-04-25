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

import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

/**
 * User interface for the {@link Dnf_Inter_Intra_contactTime} statistic. <p>
 * It's responsible for retrieving the settings from the panel and set it to the
 * statistics instance. <p>
 * <code>StatisticsUI</code> implementations are singleton (as they have a
 * <code>@ServiceProvider</code> annotation) so the panel and statistic are
 * unset after
 * <code>unsetup()</code> is called so they can be GCed.
 *
 * @author Umberto Griffo.
 */
@ServiceProvider(service = StatisticsUI.class)
public class Dnf_Inter_Intra_contactTimeUI implements StatisticsUI {

    private Dnf_Inter_Intra_contactTime IntraContactTimeMetric;
    private JPanel panel;

    @Override
    public JPanel getSettingsPanel() {
        panel = new Dnf_Inter_intra_contactTimePanel();
        return panel;
    }

    @Override
    public void setup(Statistics statistic) {
        this.IntraContactTimeMetric = (Dnf_Inter_Intra_contactTime) statistic;
    }

    @Override
    public void unsetup() {
        this.IntraContactTimeMetric = null;
        this.panel = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return Dnf_Inter_Intra_contactTime.class;
    }

    @Override
    public String getValue() {
        return "";
    }

    @Override
    public String getDisplayName() {
        return "DNF Intra/Inter-Contact Time";
    }

    @Override
    public String getCategory() {
        return StatisticsUI.CATEGORY_DYNAMIC;
    }

    @Override
    public int getPosition() {
        return 600;
    }

    @Override
    public String getShortDescription() {
       return "DNF Intra/Inter-Contact Time";
    }
}
