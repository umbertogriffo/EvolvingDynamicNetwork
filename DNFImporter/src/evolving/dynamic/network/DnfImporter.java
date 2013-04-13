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

import java.io.LineNumberReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.data.attributes.type.LongList;
import org.gephi.dynamic.api.DynamicModel.TimeFormat;
import org.gephi.io.importer.api.*;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

/**
 * File importer which can import the DNF file format. This format is a
 * text-based representation of a time-resolved Graph.
 *
 * @author Umberto Griffo
 */
public class DnfImporter implements FileImporter, LongTask {

    public static final String TIMESTAMPS = "dnf_timestamps";
    public static final String WEIGTH = "peso";
    //Importer Architecture
    private Reader reader;
    private ContainerLoader container;
    private Report report;
    private ProgressTicket progressTicket;
    private boolean cancel = false;
    private final static Logger LOGGER = Logger.getLogger("org.umberto.dnf_importer");
    //Settings
    private int aggregate;
    //HashMap EdgeDraft->Timestamps ordered by edgedraft    
    HashMap<EdgeDraft, SortedSet<Long>> map_edgedraft_time = new HashMap<EdgeDraft, SortedSet<Long>>();
    HashMap<NodeDraft, SortedSet<Long>> map_nodedraft_time = new HashMap<NodeDraft, SortedSet<Long>>();
    /**
     * FOR HEADER
     */
    private boolean isDynamic = false;
    private boolean isDirected = false;
    //Timestamp
    private String[] splits_by_comma_time;
    private String[] split_by_equal_start;
    private String[] split_by_equal_end;
    private long start_time = 0;
    private long end_time = 0;
    private long total_time = 0;
    /**
     * FOR NODES
     */
    private boolean attributes_node = false;
    private String[] splits_by_comma_node_times;
    private String[] split_by_comma_node_attr_def;
    private String[] splits_by_comma_node_attr_value;
    /**
     * FOR EDGES
     */
    private boolean attributes_edge = false;
    private boolean fixed_weight = false;
    private int passable_empty_frame = 5; //Number of passable frame without contact.
    private String[] splits_by_comma_edges;
    private String[] splits_by_comma_edge_times;
    private String[] split_by_comma_edge_attr_def;
    private String[] splits_by_comma_edge_attr_value;
    //Cols
    private AttributeColumn col_timestamps;
    private AttributeColumn col_double;

    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    @Override
    public boolean execute(ContainerLoader container) {
        this.container = container;
        this.report = new Report();
        //A buffered character-input stream that keeps track of line numbers.
        LineNumberReader lineReader = ImportUtils.getTextReader(reader);
        try {
            importData(lineReader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return !cancel;
    }

    private void importData(LineNumberReader reader) throws Exception {
        Progress.setDisplayName(progressTicket, "DNF format importing running...");
        Progress.start(progressTicket);
        String line;
        //Read only first line
        if ((line = reader.readLine()) != null) {
            /*
             * 1) SET GRAPHTYPE & DEFAULTEDGETYPE OF THE GRAPH
             */
            /*
             * GRAPHTYPE
             */
            Pattern pattern_gtype = Pattern.compile("(.*?)(graphtype:\\{)(.*?)(\\})");
            Matcher matcher_gtype = pattern_gtype.matcher(line);
            if (matcher_gtype.find()) {
                report.log("gtype: " + matcher_gtype.group(3));
                if (matcher_gtype.group(3).equals("dynamic")) {
                    isDynamic = true;
                } else if (!matcher_gtype.group(3).equals("static")) {
                    throw new IllegalValueException("You must define graphtype.");
                }
            }
            /*
             * DEFAULTEDGETYPE
             */
            Pattern pattern_etype = Pattern.compile("(.*?)(defaultedgetype:\\{)(.*?)(\\})");
            Matcher matcher_etype = pattern_etype.matcher(line);
            if (matcher_etype.find()) {
                report.log("etype: " + matcher_etype.group(3));
                if (matcher_etype.group(3).equals("directed")) {
                    isDirected = true;
                } else if (!matcher_etype.group(3).equals("undirected")) {
                    throw new IllegalValueException("You must define default edge type.");
                }
            }
            /*
             * Set container edge default type
             */
            if (isDirected) {
                container.setEdgeDefault(EdgeDefault.DIRECTED);
            } else {
                container.setEdgeDefault(EdgeDefault.UNDIRECTED);
            }
            /*
             * 2) SET THE AGGREGATE OR NOT AGGREGATE VERSION
             */
            if (isDynamic) {
                Object[] options = {"Yes, please",
                    "No, thanks"};
                aggregate = JOptionPane.showOptionDialog(new JFrame(),
                        "If you want calculate only static metrics on DNF Graph you don't need to load times.\n "
                        + "The weight of edge is calculate as: nt/n where nt is the number of timestamp of the edge and n is the total number of timestamp.\n"
                        + "Would you like aggregate graph?",
                        "Question on DNF aggregation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                if (aggregate != 0) {
                    String str = "";
                    do {
                        str = JOptionPane.showInputDialog(null, "Enter the # of passable empty frame (between 0 to 10) : ",
                                "", 1);
                    } while (!str.matches(".*\\d"));
                    if (Integer.valueOf(str) < 1) {
                        passable_empty_frame = 0;
                    } else if (Integer.valueOf(str) > 10) {
                        passable_empty_frame = 10;
                    } else {
                        passable_empty_frame = Integer.valueOf(str);
                    }
                    report.log("passable_empty_frame: " + passable_empty_frame);
                }

            }
        }
        //Read only second line
        if ((line = reader.readLine()) != null) {
            if (isDynamic) {
                /*
                 * 2) SET DYNAMIC GRAPH START and END TIME
                 */
                Pattern pattern_dinamic = Pattern.compile("(.*?)(dynamics:\\{)(.*?)(\\})");
                Matcher matcher_dinamic = pattern_dinamic.matcher(line);
                if (matcher_dinamic.find()) {
                    if (!matcher_dinamic.group(3).isEmpty()) {
                        splits_by_comma_time = matcher_dinamic.group(3).split(",");
                        //Start Time
                        //if there is olny the start time, so don't have separation by comma
                        if (splits_by_comma_time.length == 0) {
                            split_by_equal_start = matcher_dinamic.group(3).split("=");
                        } else {
                            split_by_equal_start = splits_by_comma_time[0].split("=");
                        }
                        start_time = Long.parseLong(split_by_equal_start[1].trim());
                        report.log("start: " + split_by_equal_start[1].trim());
                        //End Time
                        if (splits_by_comma_time.length > 1) {//solo se l'end esiste, altrimenti viene messo in automatico ad infinito
                            split_by_equal_end = splits_by_comma_time[1].split("=");
                            end_time = Long.parseLong(split_by_equal_end[1].trim());
                            total_time = (end_time - start_time) + 1;
                            report.log("end: " + split_by_equal_end[1].trim());
                        }

                    } else {
                        throw new IllegalValueException("You must define start time.");
                    }
                }
                if (aggregate != 0) {
                    /*
                     * Set container TimeFormat
                     */
                    container.setTimeFormat(TimeFormat.DOUBLE);
                    container.setTimeIntervalMin(split_by_equal_start[1].trim());
                    container.setTimeIntervalMax(split_by_equal_end[1].trim());
                }
            }
        }
        //Read only third line(ATTRIBUTES)
        if ((line = reader.readLine()) != null) {
            /*
             * 3) SET DEFINITION OF NODE ATTRIBUTE & EDGE ATTRIBUTE TODO: USARE
             * UNA HASH TABLE.
             */
            Pattern pattern_node_attr = Pattern.compile("(.*?)(nodeattrs:\\{)(.*?)(\\})");
            Matcher matcher_node_attr = pattern_node_attr.matcher(line);
            if (matcher_node_attr.find()) {
                if (!matcher_node_attr.group(3).isEmpty()) {
                    attributes_node = true;
                    split_by_comma_node_attr_def = matcher_node_attr.group(3).split(",");
                    //if there is only one value, so no separation by comma
                    if (split_by_comma_node_attr_def.length == 0) {
                        split_by_comma_node_attr_def[0] = matcher_node_attr.group(3);
                        //Add Attribute definition to Node Table
                        addDefinitionNodeAttribute(split_by_comma_node_attr_def[0], split_by_comma_node_attr_def[0], "STRING");
                    } else {//if there is at least 2
                        for (int i = 0; i < split_by_comma_node_attr_def.length; i = i + 1) {
                            //Add Attribute definition to Node Table
                            addDefinitionNodeAttribute(split_by_comma_node_attr_def[i], split_by_comma_node_attr_def[i], "STRING");
                        }

                    }
                }
            }
            Pattern pattern_edge_attr = Pattern.compile("(.*?)(edgeattrs:\\{)(.*?)(\\})");
            Matcher matcher_edge_attr = pattern_edge_attr.matcher(line);
            if (matcher_edge_attr.find()) {
                if (!matcher_edge_attr.group(3).isEmpty()) {
                    attributes_edge = true;
                    split_by_comma_edge_attr_def = matcher_edge_attr.group(3).split(",");
                    //if there is only one value, so no separation by comma
                    if (split_by_comma_edge_attr_def.length == 0) {
                        split_by_comma_edge_attr_def[0] = matcher_edge_attr.group(3);
                        //Add Attribute definition to Node Table
                        addDefinitionEdgeAttribute(matcher_edge_attr.group(3), matcher_edge_attr.group(3), "STRING");
                    } else {//se ce ne sono almeno 2
                        for (int i = 0; i < split_by_comma_edge_attr_def.length; i = i + 1) {
                            //Add Attribute definition to Edge Table
                            addDefinitionEdgeAttribute(split_by_comma_edge_attr_def[i], split_by_comma_edge_attr_def[i], "STRING");
                        }

                    }
                }
            }
        }
        //Read the four blank line
        reader.readLine();
        long startTime = System.currentTimeMillis();
        // Read all NODES
        while (!(line = reader.readLine()).isEmpty()) {
            NodeDraft node;
            if (cancel) {
                return;
            }
            /*
             * 4) ADD NODES TO THE GRAPH
             */
            Pattern pattern_node = Pattern.compile("(\\[)(.*?)(\\])");
            Matcher matcher_node = pattern_node.matcher(line);
            if (matcher_node.find()) {
                if (matcher_node.group(2).equals("")) {
                    throw new IllegalValueException("Find an empty node.");
                } else {
//                    System.out.println("nodo: " + matcher_node.group(2));
                }
            }
            //Add node to the Graph
            node = addNode(matcher_node.group(2), matcher_node.group(2));
            if (attributes_node) {
                /*
                 * ATTRIBUTES
                 */
                Pattern pattern_attributes = Pattern.compile("(\\{)(.*?)(\\})");
                Matcher matcher_attributes = pattern_attributes.matcher(line);
                if (matcher_attributes.find()) {
//                    System.out.println("attributes: " + matcher_attributes.group(2));
                    if (matcher_attributes.group(2).isEmpty()) {
                        throw new IllegalValueException("Don't find node attributes.");
                    }
                    splits_by_comma_node_attr_value = matcher_attributes.group(2).split(",");
                    //if there is only one value, so no separation by comma
                    if (splits_by_comma_node_attr_value.length == 0) {

                        splits_by_comma_node_attr_value[0] = matcher_attributes.group(2);
                        if (split_by_comma_node_attr_def[0].equals("label")) {
                            node.setLabel(splits_by_comma_node_attr_value[0]);
                        } else {
                            setNodeStringAttribute(node, split_by_comma_node_attr_def[0], splits_by_comma_node_attr_value[0]);
                        }
                    } else {//there are at least 2
                        for (int i = 0; i < splits_by_comma_node_attr_value.length; i = i + 1) {
                            if (split_by_comma_node_attr_def[i].equals("label")) {
                                node.setLabel(splits_by_comma_node_attr_value[i]);
                            } else {
                                setNodeStringAttribute(node, split_by_comma_node_attr_def[i], splits_by_comma_node_attr_value[i]);
                            }
                        }
                    }

                }
            }
            if (isDynamic && aggregate != 0) {
                /*
                 * TIMES
                 */
                Pattern pattern_times = Pattern.compile("(\\()(.*?)(\\))");
                Matcher matcher_times = pattern_times.matcher(line);
                if (matcher_times.find()) {
                    //System.out.println("times: " + matcher_times.group(2));
                    if (matcher_times.group(2).isEmpty()) {
                        throw new IllegalValueException("Don't find node times.");
                    }
                    SortedSet<Long> times = new TreeSet<Long>();
                    splits_by_comma_node_times = matcher_times.group(2).split(",");
                    //if there is only one value, so no separation by comma
                    if (splits_by_comma_node_times.length == 0) {
                        splits_by_comma_node_times[0] = matcher_times.group(2);
                        times.add(start_time + (Long.parseLong(splits_by_comma_node_times[0])));
                    } else {//There are at least 2
                        times.add(start_time + (Long.parseLong(splits_by_comma_node_times[0])));
                        for (int i = 1; i < splits_by_comma_node_times.length; i = i + 1) {
                            //Continuous timestamp?
                            if (splits_by_comma_node_times[i].matches("^\\+.*?")) //System.out.println(splits_by_comma_node_times[i]);
                            {
                                int k = Integer.parseInt(splits_by_comma_node_times[i].substring(1));
                                int j = 0;
                                while (j < k) {
                                    times.add(times.last() + 1);
                                    j = j + 1;
                                }
                            } else {
                                times.add(times.last() + (Long.parseLong(splits_by_comma_node_times[i])));
                            }
                        }
                    }
                    map_nodedraft_time.put(node, times);
//                  System.out.println(times.toString());
//                    if (aggregate != 0) {
////                        setNodeTimeIntervals(node, times, time_conversion_choose);
//                        setNodeTimeIntervalsLite(node, times);
//                    }

                }
            }
        }
        // Read all edges
        while ((line = reader.readLine()) != null) {
            EdgeDraft edge;
            if (cancel) {
                return;
            }
            /*
             * 5) BUILDING HashMap EdgeDraft->Timestamps ordered by edgedraft
             */
            Pattern pattern_edge = Pattern.compile("(\\[)(.*?)(\\])");
            Matcher matcher_edge = pattern_edge.matcher(line);
            if (matcher_edge.find()) {
                if (matcher_edge.group(2).isEmpty()) {
                    throw new IllegalValueException("Find an Empty edge.");
                } else {
                    splits_by_comma_edges = matcher_edge.group(2).split(",");
//                    System.out.println("source: " + splits_by_comma_edges[0] + " target: " + splits_by_comma_edges[1]);
                }
            }
            //Add edge to the Graph
            edge = addEdge(splits_by_comma_edges[0], splits_by_comma_edges[1]);
            /*
             * ATTRIBUTES
             */
            if (attributes_edge) {

                Pattern pattern_attributes = Pattern.compile("(\\{)(.*?)(\\})");
                Matcher matcher_attributes = pattern_attributes.matcher(line);
                if (matcher_attributes.find()) {
//                    System.out.println("attributes: " + matcher_attributes.group(2));
                    if (matcher_attributes.group(2).equals("")) {
                        throw new IllegalValueException("Don't find edge attributes.");
                    }
                    splits_by_comma_edge_attr_value = matcher_attributes.group(2).split(",");
                    if (splits_by_comma_edge_attr_value.length == 0) {//only one
                        splits_by_comma_edge_attr_value[0] = matcher_attributes.group(2);
                        if (split_by_comma_edge_attr_def[0].equals("label")) {
                            edge.setLabel(splits_by_comma_edge_attr_value[0]);
                        } else if (split_by_comma_edge_attr_def[0].equals("weight")) {
                            edge.setWeight(Float.parseFloat(splits_by_comma_edge_attr_value[0]));
                            fixed_weight = true;
                        } else {
                            setEdgeStringAttribute(edge, split_by_comma_edge_attr_def[0], splits_by_comma_edge_attr_value[0]);
                        }

                    } else {//at least 2
                        for (int i = 0; i < splits_by_comma_edge_attr_value.length; i = i + 1) {
                            if (split_by_comma_edge_attr_def[i].equals("label")) {
                                edge.setLabel(splits_by_comma_edge_attr_value[i]);
                            } else if (split_by_comma_edge_attr_def[i].equals("weight")) {

                                if (ValidationType.isFloatNumber(splits_by_comma_edge_attr_value[i])) {
                                    edge.setWeight(Float.parseFloat(splits_by_comma_edge_attr_value[i]));
                                } else {
                                    throw new IllegalValueException("In DNF file you write the edge's weight in wrong position.");
                                }
                                fixed_weight = true;
                            } else {
                                setEdgeStringAttribute(edge, split_by_comma_edge_attr_def[i], splits_by_comma_edge_attr_value[i]);
                            }
                        }
                    }
                }
            }
            /*
             * TIMES
             */
            if (isDynamic) {
                Pattern pattern_times = Pattern.compile("(\\()(.*?)(\\))");
                Matcher matcher_times = pattern_times.matcher(line);
                if (matcher_times.find()) {
//                    System.out.println("times: " + matcher_times.group(2));
                    if (matcher_times.group(2).isEmpty()) {
                        throw new IllegalValueException("Don't find edge times.");
                    }
                    SortedSet<Long> times = new TreeSet<Long>();
                    splits_by_comma_edge_times = matcher_times.group(2).split(",");
                    //if there is only one value, so no separation by comma
                    if (splits_by_comma_edge_times.length == 0) {
                        splits_by_comma_edge_times[0] = matcher_times.group(2);
                        times.add(start_time + (Long.parseLong(splits_by_comma_edge_times[0])));
                    } else {// if there are at least 2

                        times.add(start_time + (Long.parseLong(splits_by_comma_edge_times[0])));
                        for (int i = 1; i < splits_by_comma_edge_times.length; i = i + 1) {
                            //Continuous timestamp?
                            if (splits_by_comma_edge_times[i].matches("^\\+.*?")) //System.out.println(splits_by_comma_node_times[i]);
                            {
                                int k = Integer.parseInt(splits_by_comma_edge_times[i].substring(1));
                                int j = 0;
                                while (j < k) {
                                    times.add(times.last() + 1);
                                    j = j + 1;
                                }
                            } else {
                                //Se un arco appare ad un certo frame(o istante temporale) e riappare dopo un numero di frame inferiore o uguale a "passable_empty_frame" 
                                //considero l'arco apparso anche in quei frame in cui non esisteva
                                //in questo modo attenuo l'errore dovuto a disconnessioni dei dispotitivi, causati dal mal posizionamento
                                if ((((Long.parseLong(splits_by_comma_edge_times[i]) + times.last()) - times.last()) != 1) && (((Long.parseLong(splits_by_comma_edge_times[i]) + times.last()) - times.last()) <= passable_empty_frame)) {
                                    int j = 0;
                                    while (j < ((Long.parseLong(splits_by_comma_edge_times[i]) + times.last()) - times.last())) {
                                        long time = times.last();
                                        times.add(time + 1);
                                        //Devo aggiungerli anche ai due nodi altrimenti potrei perdere 
                                        //la consistenza del grafo
                                        if (aggregate != 0) {
                                            add_respective_nodes_time(map_nodedraft_time, splits_by_comma_edges[0], splits_by_comma_edges[1], time + 1);
                                        }
                                        j = j + 1;
                                    }
                                } else {
                                    times.add(times.last() + (Long.parseLong(splits_by_comma_edge_times[i])));
                                }
                            }
                        }
                    }
                    map_edgedraft_time.put(edge, times);
//                    System.out.println(times.toString());
                }
            }
        }
        /*
         * 6) ADD NODE TIMES
         */
        if (!map_nodedraft_time.isEmpty()) {
            if (aggregate != 0) {
                Set<Map.Entry<NodeDraft, SortedSet<Long>>> set_nodes = map_nodedraft_time.entrySet();
                for (Map.Entry<NodeDraft, SortedSet<Long>> element : set_nodes) {
                    setNodeTimeIntervalsLite(element.getKey(), element.getValue());
                    LOGGER.log(Level.INFO, "Ho calcolato il nodo: {0}", element.getKey());
                }
            }
        }
        /*
         * 7) ADD EDGES TIMES
         */
        LOGGER.log(Level.INFO, "total number of timestamp : {0}", total_time);
        if (!map_edgedraft_time.isEmpty()) {
            if (aggregate != 0) {
                Set<Map.Entry<EdgeDraft, SortedSet<Long>>> set_edges = map_edgedraft_time.entrySet();
                for (Map.Entry<EdgeDraft, SortedSet<Long>> element : set_edges) {
//                    setEdgeTimeIntervals(element.getKey(), element.getValue(), time_conversion_choose);
                    setEdgeTimeIntervalsLite(element.getKey(), element.getValue());
                    if (!fixed_weight && total_time != 0) {//set the edge weight if and only if the user don't set a fixed weight or the user don't define an end time.
                        element.getKey().setWeight(calculate_weight(element.getKey(), element.getValue(), total_time));
                    }
                    LOGGER.log(Level.INFO, "Ho calcolato l''arco : {0}", element.getKey());

                }
            } else {
                if (!fixed_weight && total_time != 0) {//set the edge weight if and only if the user don't set a fixed weight or the user don't define an end time.
                    Set<Map.Entry<EdgeDraft, SortedSet<Long>>> set_edges = map_edgedraft_time.entrySet();
                    for (Map.Entry<EdgeDraft, SortedSet<Long>> element : set_edges) {
                        element.getKey().setWeight(calculate_weight(element.getKey(), element.getValue(), total_time));
                        LOGGER.log(Level.INFO, "Ho calcolato l''arco : {0}", element.getKey());
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();

        LOGGER.log(Level.INFO, "That took {0} milliseconds", (endTime - startTime));

        Progress.finish(progressTicket);
    }

    @Override
    //Returns the import container. The container is the import "result", all data found during import are being pushed to the container.
    public ContainerLoader getContainer() {
        return container;
    }

    @Override
    //Returns the import report, filled with logs and potential issues.
    public Report getReport() {
        return report;
    }

    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    /**
     * Method that define an attribute to all nodes.
     *
     * @param id attribute id.
     * @param title attribute title.
     * @param Type BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, CHAR, STRING,
     * BIGINTEGER, BIGDECIMAL, DYNAMIC_BYTE, DYNAMIC_SHORT, DYNAMIC_INT,
     * DYNAMIC_LONG, DYNAMIC_FLOAT, DYNAMIC_DOUBLE, DYNAMIC_BOOLEAN,
     * DYNAMIC_CHAR, DYNAMIC_STRING, DYNAMIC_BIGINTEGER, DYNAMIC_BIGDECIMAL,
     * TIME_INTERVAL, LIST_BYTE, LIST_SHORT, LIST_INTEGER, LIST_LONG,
     * LIST_FLOAT, LIST_DOUBLE, LIST_BOOLEAN, LIST_CHARACTER, LIST_STRING,
     * LIST_BIGINTEGER, LIST_BIGDECIMAL
     */
    private void addDefinitionNodeAttribute(String id, String title, String TYPE) {
        AttributeColumn column = container.getAttributeModel().getNodeTable().getColumn(title);
        if (column == null) {
            //Add a column at nodetable
            container.getAttributeModel().getNodeTable().addColumn(id, title, AttributeType.valueOf(TYPE), AttributeOrigin.DATA, "");
            report.log("Node attribute definition column correctly loaded into NodeTable.");
        }
    }

    /**
     * Method that define an attribute to all edges.
     *
     * @param id attribute id.
     * @param title attribute title.
     * @param Type BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, CHAR, STRING,
     * BIGINTEGER, BIGDECIMAL, DYNAMIC_BYTE, DYNAMIC_SHORT, DYNAMIC_INT,
     * DYNAMIC_LONG, DYNAMIC_FLOAT, DYNAMIC_DOUBLE, DYNAMIC_BOOLEAN,
     * DYNAMIC_CHAR, DYNAMIC_STRING, DYNAMIC_BIGINTEGER, DYNAMIC_BIGDECIMAL,
     * TIME_INTERVAL, LIST_BYTE, LIST_SHORT, LIST_INTEGER, LIST_LONG,
     * LIST_FLOAT, LIST_DOUBLE, LIST_BOOLEAN, LIST_CHARACTER, LIST_STRING,
     * LIST_BIGINTEGER, LIST_BIGDECIMAL
     *
     */
    private void addDefinitionEdgeAttribute(String id, String title, String TYPE) {
        AttributeColumn column = container.getAttributeModel().getEdgeTable().getColumn(title);
        if (column == null) {
            //Add a column at nodetable
            container.getAttributeModel().getEdgeTable().addColumn(id, title, AttributeType.valueOf(TYPE), AttributeOrigin.DATA, "");
            report.log("Edge attribute definition column correctly loaded into EdgeTable.");
        }
    }

    /**
     * Method that set a string attribute of a node.
     *
     * @param node node to set attribute.
     * @param name name of the attribute which must be setted.
     * @param value value of attribute.
     */
    private void setNodeStringAttribute(NodeDraft node, String name, String value) {
        //Set attribute
        //final IntegerList timestamp_list = new IntegerList((Integer[]) timestamps.toArray(new Integer[timestamps.size()]));
        AttributeColumn column = container.getAttributeModel().getNodeTable().getColumn(name);
        if (column != null) {
            node.addAttributeValue(column, value);
        }
    }

    /**
     * Method that set a string attribute of an edge.
     *
     * @param edge edge to set attribute.
     * @param name name of the attribute which must be setted.
     * @param value value of attribute.
     */
    private void setEdgeStringAttribute(EdgeDraft edge, String name, String value) {
        //Set attribute
        //final IntegerList timestamp_list = new IntegerList((Integer[]) timestamps.toArray(new Integer[timestamps.size()]));
        AttributeColumn column = container.getAttributeModel().getEdgeTable().getColumn(name);
        if (column != null) {
            edge.addAttributeValue(column, value);
        }
    }

    /**
     * Method that set a node's time intervals.
     *
     * @param node node to set attribute.
     * @param timestamps Sorted hash map of node's timestamps.
     * @param choose 0 if convert timestamp in datetime.
     */
    private void setNodeTimeIntervals(NodeDraft node, SortedSet<Long> timestamps, int conversion) {
        try {

            //Popolazione Intervalli temporali
            final Long[] pippi = (Long[]) timestamps.toArray(new Long[timestamps.size()]);
            if (pippi.length > 1) {
                int i = 0;
                //Scorro tutti i timestamps
                while (i < pippi.length) {
                    if (i % 10000 == 0) {
                        LOGGER.log(Level.INFO, "timestamp:{0} of: {1}", new Object[]{i, pippi.length});
                    }
                    if ((i + 1) < pippi.length) {
                        int j = 0;
                        while (pippi[i + 1] == (pippi[i]) + 1) {
                            //Verifico di non aver superato il bound dell'array
                            //altrimenti il while giustamente và in eccezione
                            if ((i + 1) == pippi.length - 1) {
                                i = i + 1;
                                j = j + 1;
                                break;
                            } else {
                                i = i + 1;
                                j = j + 1;
                            }
                        }

                        if (j != 0) {
                            node.addTimeInterval(pippi[i - j].toString(), pippi[i].toString());
                        } else {
                            node.addTimeInterval(pippi[i].toString(), pippi[i].toString(), true, false);
                        }

                    } else {
                        node.addTimeInterval(pippi[i].toString(), pippi[i].toString(), true, false);

                    }
                    i = i + 1;
                }
            } else {//Se ho un solo timestamp genero un solo intervallo

                node.addTimeInterval(pippi[0].toString(), pippi[0].toString(), true, false);
            }

        } catch (IllegalArgumentException e) {
            report.logIssue(new Issue("DNFImporter_error_nodeattribute_timestamp_parseerror", Issue.Level.SEVERE));
        } catch (Exception e) {
            report.logIssue(new Issue("DNFFileImporter_error_datavalue", Issue.Level.SEVERE));
        }
    }

    /**
     * Method that set a node's time intervals.(Lite version)
     *
     * @param node node to set attribute.
     * @param timestamps Sorted hash map of node's timestamps.
     */
    private void setNodeTimeIntervalsLite(NodeDraft node, SortedSet<Long> timestamps) {
        node.addTimeInterval(split_by_equal_start[1].trim(), split_by_equal_end[1].trim());
        AttributeTable nodeTable = container.getAttributeModel().getNodeTable();
        col_timestamps = nodeTable.getColumn(TIMESTAMPS);
        if (col_timestamps == null) {
            col_timestamps = nodeTable.addColumn(TIMESTAMPS, "Timestamps", AttributeType.LIST_LONG, AttributeOrigin.DATA, "");
        }
        //Set attribute
        if (col_timestamps != null) {
            final LongList timestamp_list = new LongList(timestamps.toArray(new Long[timestamps.size()]));
            node.addAttributeValue(col_timestamps, timestamp_list);
        } else {
            report.logIssue(new Issue("DNFFileImporter_error_column_already_exist", Issue.Level.SEVERE));
        }
    }

    /**
     * Method that set a edge's time intervals.
     *
     * @param edge edge to set attribute.
     * @param timestamps Sorted hash map of edge's timestamps.
     * @param choose 0 if convert timestamp in datetime.
     */
    private void setEdgeTimeIntervals(EdgeDraft edge, SortedSet<Long> timestamps, int conversion) {
        try {

            //Popolazione Intervalli temporali
            final Long[] pippi = (Long[]) timestamps.toArray(new Long[timestamps.size()]);
            if (pippi.length > 1) {
                int i = 0;
                //Scorro tutti i timestamps
                while (i < pippi.length) {
                    if (i % 10000 == 0) {
                        LOGGER.log(Level.INFO, "timestamp:{0} of: {1}", new Object[]{i, pippi.length});
                    }
                    if ((i + 1) < pippi.length) {
                        int j = 0;
                        while (pippi[i + 1] == (pippi[i]) + 1) {
                            //Verifico di non aver superato il bound dell'array
                            //altrimenti il while giustamente và in eccezione
                            if ((i + 1) == pippi.length - 1) {
                                i = i + 1;
                                j = j + 1;
                                break;
                            } else {
                                i = i + 1;
                                j = j + 1;
                            }
                        }

                        if (j != 0) {
                            edge.addTimeInterval(pippi[i - j].toString(), pippi[i].toString());
                        } else {
                            edge.addTimeInterval(pippi[i].toString(), pippi[i].toString(), false, true);
                        }

                    } else {

                        edge.addTimeInterval(pippi[i].toString(), pippi[i].toString(), false, true);

                    }
                    i = i + 1;
                }
            } else {//Se ho un solo timestamp genero un solo intervallo

                edge.addTimeInterval(pippi[0].toString(), pippi[0].toString(), false, true);

            }

        } catch (IllegalArgumentException e) {
            report.logIssue(new Issue("DNFImporter_error_edgeattribute_timestamp_parseerror", Issue.Level.SEVERE));
        } catch (Exception e) {
            report.logIssue(new Issue("DNFFileImporter_error_datavalue", Issue.Level.SEVERE));
        }
    }

    /**
     * Method that set a edge's time intervals.(Lite Version)
     *
     * @param edge edge to set attribute.
     * @param timestamps Sorted hash map of edge's timestamps.
     */
    private void setEdgeTimeIntervalsLite(EdgeDraft edge, SortedSet<Long> timestamps) {
        edge.addTimeInterval(split_by_equal_start[1].trim(), split_by_equal_end[1].trim());
        AttributeTable edgeTable = container.getAttributeModel().getEdgeTable();
        col_timestamps = edgeTable.getColumn(TIMESTAMPS);
        if (col_timestamps == null) {
            col_timestamps = edgeTable.addColumn(TIMESTAMPS, "Timestamps", AttributeType.LIST_LONG, AttributeOrigin.DATA, "");
        }
        //Set attribute
        if (col_timestamps != null) {
            final LongList timestamp_list = new LongList(timestamps.toArray(new Long[timestamps.size()]));
            edge.addAttributeValue(col_timestamps, timestamp_list);
        } else {
            report.logIssue(new Issue("DNFFileImporter_error_column_already_exist", Issue.Level.SEVERE));
        }
    }

    /**
     * Method that add a node to the graph
     *
     * @param id the node's id.
     * @param label the node's label.
     */
    private NodeDraft addNode(String id, String label) {
        NodeDraft node = null;
        if (!container.nodeExists(id)) {
            node = container.factory().newNodeDraft();
            node.setId(id);
            node.setLabel(label);
            container.addNode(node);
        }
        return node;
    }

    /**
     * Method that add weighted edges to the graph.
     *
     * @param source source node.
     * @param target target node.
     *
     */
    //Calculate total number of timestamps
    private EdgeDraft addEdge(String source, String target) {
        EdgeDraft edge;
        NodeDraft sourceNode;
        if (!container.nodeExists(source)) {
            sourceNode = container.factory().newNodeDraft();
            sourceNode.setId(source);
            container.addNode(sourceNode);
        } else {
            sourceNode = container.getNode(source);
        }
        NodeDraft targetNode;
        if (!container.nodeExists(target)) {
            targetNode = container.factory().newNodeDraft();
            targetNode.setId(target);
            container.addNode(targetNode);
        } else {
            targetNode = container.getNode(target);
        }

        edge = container.getEdge(sourceNode, targetNode);

        if (edge == null) {
            edge = container.factory().newEdgeDraft();
            edge.setSource(sourceNode);
            edge.setTarget(targetNode);
        }
        container.addEdge(edge);
        return edge;
    }

    /**
     * Method that calculate the weight of an edge as: ne/n where ne is the
     * number of timestamp of the edge and n is the max number of timestamp.
     *
     * @param
     */
    private float calculate_weight(EdgeDraft edge, Set<Long> timestamps, long n) {
        //Aggiungo una nuova colonna con i pesi a massima precisione
        AttributeTable edgeTable = container.getAttributeModel().getEdgeTable();
        col_double = edgeTable.getColumn(WEIGTH);
        if (col_double == null) {
            col_double = edgeTable.addColumn(WEIGTH, "WeightPrec", AttributeType.BIGDECIMAL, AttributeOrigin.DATA, new BigDecimal(0));
        }
        //Set attribute
        if (col_double != null) {
            BigDecimal time_presente = new BigDecimal(timestamps.size());
            BigDecimal totali = new BigDecimal(n);
            BigDecimal peso = time_presente.divide(totali, 6, RoundingMode.HALF_UP);
            edge.addAttributeValue(col_double, peso);
        } else {
            report.logIssue(new Issue("DNFFileImporter_error_column_already_exist", Issue.Level.SEVERE));
        }
        //Aggiunta dei pesi a bassa precisione
        float weight;
        weight = (float) timestamps.size() / (float) n;
        return weight;
    }
//    //mi serve per dare un valore di default nella GUI 
//
//    public boolean isAggregate() {
//        return aggregate;
//    }
//    //Setta il valore della var.booleana in base al radio button checked
//
//    public void setAggregate(boolean aggregate) {
//        this.aggregate = aggregate;
//    }

    /**
     * Method that set a node's time intervals.(Lite version)
     *
     * @param map_nodedraft_time hashmap that has node like key and SortedSet of
     * timestamp like value.
     * @param source edge's source node.
     * @param target edge's target node.
     * @param time time to add.
     *
     */
    private void add_respective_nodes_time(HashMap<NodeDraft, SortedSet<Long>> map_nodedraft_time, String source, String target, long time) {
        NodeDraft nodo1 = container.getNode(source);
        NodeDraft node2 = container.getNode(target);
        SortedSet<Long> time_nodo1 = map_nodedraft_time.get(nodo1);
        SortedSet<Long> time_nodo2 = map_nodedraft_time.get(node2);
        time_nodo1.add(time);
        time_nodo2.add(time);
    }
}
