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

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXHeader;

/**
 * Settings panel for the {@link Dnf_Inter_intra_contactTime} statistic. It uses
 * a nice
 * <code>JXHeader</code> at the top of the panel.
 *
 * @author Umberto Griffo.
 */
public class Dnf_Inter_intra_contactTimePanel extends JPanel {

    // Variables declaration - do not modify                     
    private JXHeader jXHeader1;

    /**
     * Creates new form ProvaMetricPanel
     */
    public Dnf_Inter_intra_contactTimePanel() {
        initComponents();
    }

    private void initComponents() {

        jXHeader1 = new JXHeader();
        jXHeader1.setDescription("Calculate the Inter and Intra(Contact Duration) Contact Time distribution.");
        jXHeader1.setTitle("Inter/Intra-Contact Time");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jXHeader1, GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jXHeader1, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }// </editor-fold>    
}
