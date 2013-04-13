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

import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

/**
 * File importer JPanel.
 *
 * @author Umberto Griffo
 */
public class DnfImporterPanel extends JPanel {

    private JRadioButton aggregate;
    private JRadioButton not_aggregate;
    private ButtonGroup buttonGroup;

    //Imposta il valore di default
    public void setAggregate(boolean aggregate) {
        this.aggregate.setSelected(aggregate);
        this.not_aggregate.setSelected(!aggregate);
    }

    //Ritorna i valori dei radio button
    public boolean isAggregate() {
        //Se uno è true l'altro è false
        return aggregate.isSelected();
    }

    /**
     * Creates new form DnfImporterPanel
     */
    public DnfImporterPanel() {
        initComponents();
    }

    private void initComponents() {
        aggregate = new JRadioButton();
        not_aggregate = new JRadioButton();
        buttonGroup = new ButtonGroup();
        buttonGroup.add(aggregate);
        aggregate.setText("DNF Aggregate version without time intervals. The weight of edge is calculate as: nt/n where nt is the number of timestamp of the edge and n is the total number of timestamp.");
        buttonGroup.add(not_aggregate);
        not_aggregate.setText("DNF Not Aggregate version with time intervals");
        Border border = BorderFactory.createTitledBorder("Select DNF Version:");
        this.setBorder(border);
        this.add(aggregate);
        this.add(not_aggregate);
        GridLayout experimentLayout = new GridLayout(0, 1);
        this.setLayout(experimentLayout);
    }
}
