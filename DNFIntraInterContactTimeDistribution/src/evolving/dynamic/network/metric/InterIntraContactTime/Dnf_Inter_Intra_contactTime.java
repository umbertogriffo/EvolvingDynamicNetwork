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

import evolving.dynamic.networks.Dynamic;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.StatisticsUtils;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.util.Lookup;

/**
 * Calculates the distribution of intra/inter-contact time.
 *
 * @see Dnf_Inter_Intra_contactTimeBuilder
 * @see Dnf_Inter_Intra_contactTimeUI
 * @author Umberto Griffo
 */
public class Dnf_Inter_Intra_contactTime implements Statistics, LongTask {

    /**
     * Remembers if the Cancel function has been called.
     */
    private boolean cancel;
    private final static Logger LOGGER = Logger.getLogger("org.umberto.dnf_inter_intra_contact_time");
    /**
     * Statistic variable
     */
    private String report = "";
    private ProgressTicket progressTicket;
    private boolean preprocessingSuccess;
    //Manipulation data
    private long start_time = 0;
    private long end_time = 0;
    private int total_contact = 0;//Numero totale di contatti
    private int total_interval = 0;//Numero totale d'intervalli
    private ArrayList<Number> arr_durate = new ArrayList<Number>();//per calcolare l'average
    private ArrayList<Number> arr_durate_interval = new ArrayList<Number>();//per calcolare l'average
    private SortedMap<Integer, Integer> duration_to_count = new TreeMap<Integer, Integer>();//Per ogni durata so' quante volte e' successo
    private SortedMap<Integer, Integer> interval_duration_to_count = new TreeMap<Integer, Integer>();//Per ogni durata di intervallo so' quante volte e' successo
    private SortedMap<Integer, Float> cumulated_durate_distribution = new TreeMap<Integer, Float>();// to plot cumulate distribution
    private SortedMap<Integer, Float> cumulated_interval_distribution = new TreeMap<Integer, Float>();// to plot cumulate distribution

    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        cancel = false;
        /**
         * EXTRACT START & END TIME
         */
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        LOGGER.log(Level.INFO, "Start_time:{0} End_time: {1}", new Object[]{dynamicModel.getMin(), dynamicModel.getMax()});
        start_time = (long) dynamicModel.getMin();
        end_time = (long) dynamicModel.getMax();
        LOGGER.log(Level.INFO, "Start_time:{0} End_time: {1}", new Object[]{start_time, end_time});
        /**
         * Retrive the graph
         */
        HierarchicalGraph hgraph = graphModel.getHierarchicalUndirectedGraphVisible();
        /**
         * PREPROCESSING
         */
        preprocessingSuccess = true;
        try {
            Dynamic.Preprocessing(hgraph, attributeModel);
        } catch (IllegalFormatException e) {
            preprocessingSuccess = false;
        }

        if (preprocessingSuccess) {
            /**
             * CALCULATE METRIC
             */
            hgraph.readLock();
            try {
                long startTime = System.currentTimeMillis();
                Progress.setDisplayName(progressTicket, "Metric running...");
                Progress.start(progressTicket, hgraph.getEdgeCount());
                int count_ticket = 0;
                for (Edge edge : hgraph.getEdges()) {
                    if (cancel) {
                        break;
                    }
                    LongLinkedOpenHashSet timestamps = Dynamic.getEdgeTimes(edge);
                    if (!timestamps.isEmpty()) {

                        /*
                         * Ora che ho gli intervalli in cui esiste devo contare
                         * le durate degli intervalli contigui e di quelli non
                         * contigui.
                         */
                        int count;//contatore delle occorrenze delle durate
                        int count_interval;//contatore delle occorrenze delle durate degli intervalli
                        final Long[] arr_timestamps = timestamps.toArray(new Long[timestamps.size()]);
                        //Scorro tutti gli intervalli del nodo i-esimo
                        int i = 0;
                        while (i < arr_timestamps.length) {
                            int j = 0;//contatore della durata
                            int k = 0;//contatore della durata intervallo
                            if ((i + 1) < arr_timestamps.length) {
                                //verifico se i due istanti sono contigui
                                if (arr_timestamps[i + 1] == (arr_timestamps[i]) + 1) {
                                    while (arr_timestamps[i + 1] == (arr_timestamps[i]) + 1) {
                                        //Verifico di non aver superato il bound dell'array
                                        //altrimenti il while giustamente và in eccezione
                                        if ((i + 1) == arr_timestamps.length - 1) {
                                            i = i + 1;
                                            j = j + 1;
                                            break;
                                        } else {
                                            i = i + 1;
                                            j = j + 1;
                                        }
                                    }
                                } else {
                                    //se non entra nel while significa che non è contiguo
                                    k = (int) (arr_timestamps[i + 1] - arr_timestamps[i]);
                                    //nota se incremento qui nell'else e non dopo insieme a count, non conto gli interval durati 0
                                    //ma solo da 1 in poi
                                    count_interval = interval_duration_to_count.containsKey(k) ? interval_duration_to_count.get(k) : 0;
                                    interval_duration_to_count.put(k, count_interval + 1);
                                    //for probability
                                    total_interval += 1;
                                    //for average
                                    arr_durate_interval.add(k);
                                }
                                //Se voglio fare un counter con un hash map devo fare cosi'
                                //j+1 perchè j parte da 0 ma i contatti sono durati minimo 1
                                //ho solo schiftato 
                                count = duration_to_count.containsKey(j + 1) ? duration_to_count.get(j + 1) : 0;
                                duration_to_count.put(j + 1, count + 1);
                                //for probability
                                total_contact += 1;
                                //for average
                                arr_durate.add(j + 1);
                            } else {// se un nodo appare solo in un timestamp
                                count = duration_to_count.containsKey(1) ? duration_to_count.get(1) : 0;
                                duration_to_count.put(1, count + 1);
                                //for probability
                                total_contact += 1;
                                //for average
                                arr_durate.add(1);
                            }
                            i = i + 1;
                        }
                    }

                    count_ticket = count_ticket + 1;
                    Progress.progress(progressTicket, count_ticket);
                }
                LOGGER.log(Level.INFO, "total contact: {0}", total_contact);
                LOGGER.log(Level.INFO, "total interval: {0}", total_interval);
//                Set<SortedMap.Entry<Integer, Integer>> set_durate = duration_to_count.entrySet();
//                for (SortedMap.Entry<Integer, Integer> element : set_durate) {
//                    Integer num_occorrenze = element.getValue();
//                    Integer durata = element.getKey();
//                    LOGGER.log(Level.INFO, "durata_contatto: {0}", durata);
//                    LOGGER.log(Level.INFO, "occorrenze : {0}", num_occorrenze);
//                }
//                Set<SortedMap.Entry<Integer, Integer>> set_durate_intervalli = interval_duration_to_count.entrySet();
//                for (SortedMap.Entry<Integer, Integer> element : set_durate_intervalli) {
//                    Integer num_occorrenze = element.getValue();
//                    Integer durata = element.getKey();
//                    LOGGER.log(Level.INFO, "durata_intervallo: {0}", durata);
//                    LOGGER.log(Level.INFO, "occorrenze : {0}", num_occorrenze);
//                }
//                long endTime = System.currentTimeMillis();
//                LOGGER.log(Level.INFO, "Metric calculated in {0} milliseconds", (endTime - startTime));
                //Unlock graph
                hgraph.readUnlockAll();

            } catch (Exception e) {
                e.printStackTrace();
                //Unlock graph
                hgraph.readUnlockAll();
            }
        } else {
            //Unlock graph
            hgraph.readUnlockAll();
            JOptionPane.showMessageDialog(new JFrame(),
                    "This metric is supported only on DNF graph.\n "
                    + "More info on http://wiserver.dis.uniroma1.it/cms/index.php/projects/12-dnf.\n",
                    "Format error",
                    JOptionPane.ERROR_MESSAGE);
        }

    }

    @Override
    public String getReport() {
        Progress.setDisplayName(progressTicket, "Generating charts...");
        /*
         * SAVE IN FILE FOR MULTIPLE PLOT
         */
        //current directory
        boolean success;
        String currentDir = System.getProperty("user.dir");
        String saveDir = currentDir + "\\savedata";
        boolean exist = (new File(saveDir)).exists();
        if (!exist) {
            success = (new File(saveDir)).mkdir();
        } else {
            success = true;
        }
        //Transform to Map
        Map<Integer, Double> map_intra = new HashMap<Integer, Double>();

        Set<SortedMap.Entry<Integer, Integer>> set_durate = duration_to_count.entrySet();
        for (SortedMap.Entry<Integer, Integer> element : set_durate) {
            float cumulated = 0;
            for (SortedMap.Entry<Integer, Integer> element2 : set_durate) {
                //F(s) = nodes fraction with (duration/duration max) >= s
                if (element2.getKey() >= element.getKey()) {
                    cumulated += element2.getValue();
                }
            }
            cumulated_durate_distribution.put(element.getKey(), (cumulated / duration_to_count.lastKey()));
        }
        Set<SortedMap.Entry<Integer, Float>> set_durate_cumulated = cumulated_durate_distribution.entrySet();
        for (SortedMap.Entry<Integer, Float> element : set_durate_cumulated) {
            Float num_occorrenze = element.getValue();
            double prob = (double) num_occorrenze / total_contact;
            Integer durata = element.getKey();
            map_intra.put(durata, prob);
        }
        //Save file
        if (success) {
            String completeDirIntra = saveDir + "\\IntraContactDistribution.txt";
            String finalDir = completeDirIntra.replace("\\", "\\\\");
            LOGGER.log(Level.INFO, "Intra path: {0}", new Object[]{finalDir});
            generateSaveFile(finalDir, map_intra);
        } else {
            LOGGER.log(Level.INFO, "Don't create a new directory");
        }

        //Transform to Map
        Map<Integer, Double> map_inter = new HashMap<Integer, Double>();

        Set<SortedMap.Entry<Integer, Integer>> set_durate_interval = interval_duration_to_count.entrySet();
        for (SortedMap.Entry<Integer, Integer> element : set_durate_interval) {
            float cumulated = 0;
            for (SortedMap.Entry<Integer, Integer> element2 : set_durate_interval) {
                //F(s) = nodes fraction with (duration/duration max) >= s
                if (element2.getKey() >= element.getKey()) {
                    cumulated += element2.getValue();
                }
            }
            cumulated_interval_distribution.put(element.getKey(), (cumulated / interval_duration_to_count.lastKey()));
        }

        Set<SortedMap.Entry<Integer, Float>> set_durate_interval_cumulated = cumulated_interval_distribution.entrySet();
        for (SortedMap.Entry<Integer, Float> element : set_durate_interval_cumulated) {
            Float num_occorrenze = element.getValue();
            double prob = (double) num_occorrenze / total_interval;
            Integer durata = element.getKey();
            map_inter.put(durata, prob);
        }
        //Save file
        if (success) {
            String completeDirIntra = saveDir + "\\InterContactDistribution.txt";
            String finalDir = completeDirIntra.replace("\\", "\\\\");
            LOGGER.log(Level.INFO, "Inter path: {0}", new Object[]{finalDir});
            generateSaveFile(finalDir, map_inter);
        } else {
            LOGGER.log(Level.INFO, "Don't create a new directory");
        }
        //Distribution series
        XYSeries intraSeries = ChartUtils.createXYSeries(map_intra, "Intra Contact Time - Distribution");
        XYSeries interSeries = ChartUtils.createXYSeries(map_inter, "Inter Contact Time - Distribution");

        XYSeriesCollection dataset1 = new XYSeriesCollection();
        dataset1.addSeries(intraSeries);

        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(interSeries);

        JFreeChart chart1 = ChartFactory.createXYLineChart(
                "Intra Contact Time - Distribution",
                "contact duration t",
                "P(t)",
                dataset1,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);
        ChartUtils.decorateChart(chart1);
        ChartUtils.scaleLogChart(chart1, intraSeries, false);
        String intraImageFile = ChartUtils.renderChart(chart1, "intra-distribution.png");

        JFreeChart chart2 = ChartFactory.createXYLineChart(
                "Inter Contact Time - Distribution",
                "interval t between contacts",
                "P(t)",
                dataset2,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);


        ChartUtils.decorateChart(chart2);
        ChartUtils.scaleLogChart(chart2, interSeries, false);
        String interImageFile = ChartUtils.renderChart(chart2, "inter-distribution.png");

        NumberFormat f = new DecimalFormat("#0.0");
        //average
        BigDecimal average_intra = new BigDecimal(0);
        if (!arr_durate.isEmpty()) {
            average_intra = StatisticsUtils.average(arr_durate);
        }
        //average
        BigDecimal average_inter = new BigDecimal(0);
        if (!arr_durate_interval.isEmpty()) {
            average_inter = StatisticsUtils.average(arr_durate_interval);
        }
        if (preprocessingSuccess) {
            report = "<HTML> <BODY> <h1>Intra/Inter-Contact Time Report </h1> "
                    + "<hr>"
                    + "<br> Bounds: from " + f.format(start_time) + " to " + f.format(end_time)
                    + "<br /><br />"
                    + "<h2> Results: </h2>"
                    + "<p><b>Average Intra-contact Time: </b>" + f.format(average_intra.doubleValue()) + " t</p><br />"
                    + "The Average Intra-contact Time is the mean value of a single contact duration.<br /><br />"
                    + "<br /><br />" + intraImageFile
                    + "<br /><br />"
                    + "<p><b>Average Inter-contact Time: </b>" + f.format(average_inter.doubleValue()) + " t</p><br />"
                    + "The Average Inter-contact Time is the mean value of elapsed time between the same contact.<br /><br />"
                    + "<br /><br />" + interImageFile;


            /*
             * for (Interval<Double> averages : averages) { report +=
             * averages.toString(dynamicModel.getTimeFormat().equals(DynamicModel.TimeFormat.DOUBLE))
             * + "<br />"; }
             */
            report += "<br /><br /></BODY></HTML>";
        } else {
            report += "<h3>This metric is supported only on DNF graph.</h3><br />More info <a href=\"http://wiserver.dis.uniroma1.it/cms/index.php/projects/12-dnf\">http://wiserver.dis.uniroma1.it/cms/index.php/projects/12-dnf</a>.";

        }
        return report;
    }

    @Override
    public boolean cancel() {
        this.cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    /**
     * Generate Intra Contact time distribution file.
     *
     * @param sFileName file path.
     */
    public void generateSaveFile(String sFileName, Map<Integer, Double> map) {
        try {
            FileWriter writer = new FileWriter(sFileName);
            Set<Map.Entry<Integer, Double>> set = map.entrySet();
            int k = 0;
            for (Map.Entry<Integer, Double> element : set) {
                writer.append(element.getKey().toString());
                writer.append(' ');
                writer.append(element.getValue().toString());
                k = k + 1;
                if (k != map.size()) {
                    writer.append('\n');
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
