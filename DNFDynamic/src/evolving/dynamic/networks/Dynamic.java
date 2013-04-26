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

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.type.LongList;
import org.gephi.graph.api.*;
import org.umberto.data_structure_utils.SortedMapInteger;

/**
 * Simulation of Evolving Dynamic Network.
 *
 * @author Umberto Griffo
 */
public abstract class Dynamic {

    public static final String TIMESTAMPS = "dnf_timestamps";
    private final static Logger LOGGER = Logger.getLogger("evolving.dynamic.networks");
    private static HashMap<Integer, Integer> map_edge_frequency;
    private static SortedMap<Integer, Integer> sorted_map_frequency_edge;
    //Fast manipulation data structure
    private static Int2ObjectLinkedOpenHashMap<LongLinkedOpenHashSet> map_node_time;
    private static Int2ObjectLinkedOpenHashMap<LongLinkedOpenHashSet> map_edge_time;
    //cols
    private static AttributeColumn col_timestamps;
    //graph model & view

    /**
     * Build fast data structure that represent the HierarchicalGraph.
     *
     * @param hgraph the graph.
     * @param attributeModel
     */
    public static void Preprocessing(HierarchicalGraph hgraph, AttributeModel attributeModel) {
        LOGGER.log(Level.INFO, "Preprocessing start...");
        map_node_time = new Int2ObjectLinkedOpenHashMap<LongLinkedOpenHashSet>();
        map_edge_time = new Int2ObjectLinkedOpenHashMap<LongLinkedOpenHashSet>();
        for (Node node : hgraph.getNodes()) {
            LongLinkedOpenHashSet timestamp = new LongLinkedOpenHashSet();
            AttributeTable nodeTable = attributeModel.getNodeTable();
            col_timestamps = nodeTable.getColumn(TIMESTAMPS);
            if (col_timestamps == null) {
                throw new IllegalFormatException("This metric is supported only on DNF graph.");
            } else {
                LongList timestamp_list = (LongList) node.getNodeData().getAttributes().getValue(col_timestamps.getIndex());
                for (int i = 0; i < timestamp_list.size(); i = i + 1) {
                    timestamp.add(timestamp_list.getItem(i));
                }
                map_node_time.put(node.getId(), timestamp);
            }
        }
        for (Edge edge : hgraph.getEdges()) {
            LongLinkedOpenHashSet timestamp = new LongLinkedOpenHashSet();
            AttributeTable edgeTable = attributeModel.getEdgeTable();
            col_timestamps = edgeTable.getColumn(TIMESTAMPS);
            if (col_timestamps == null) {
                throw new IllegalFormatException("This metric is supported only on DNF graph.");
            } else {
                LongList timestamp_list = (LongList) edge.getEdgeData().getAttributes().getValue(col_timestamps.getIndex());
                for (int i = 0; i < timestamp_list.size(); i = i + 1) {
                    timestamp.add(timestamp_list.getItem(i));
                }
                map_edge_time.put(edge.getId(), timestamp);
            }
        }
//        Set<Map.Entry<Integer, LongRBTreeSet>> set_edges = map_edge_time.entrySet();
//        for (Map.Entry<Integer, LongRBTreeSet> element : set_edges) {
//            System.out.println(element.getKey().toString() + element.getValue().toString());
//
//        }
        LOGGER.log(Level.INFO, "Preprocessing end...");

    }

    /**
     * Check the presence of node at time t.
     *
     * @param n node.
     * @param time time t.
     */
    public static boolean node_in_time(Node n, long time) {
        boolean in_time = false;
        if (map_node_time.get(n.getId()).contains(time)) {
            in_time = true;
        }
        return in_time;
    }

    /**
     * Check the presence of edge at time t.
     *
     * @param e edge.
     * @param time time t.
     */
    public static boolean edge_in_time(Edge e, long time) {
        boolean in_time = false;

        if (map_edge_time.get(e.getId()).contains(time)) {
            in_time = true;
        }

        return in_time;
    }

    /**
     * Return the edge's timestamp list
     *
     * @param e edge.
     * @return timestamp list
     */
    public static LongLinkedOpenHashSet getEdgeTimes(Edge e) {
        LongLinkedOpenHashSet timestamps = new LongLinkedOpenHashSet();
        if (map_edge_time.containsKey(e.getId())) {
            timestamps = map_edge_time.get(e.getId());
        }
        return timestamps;
    }

    /**
     * Return the node's timestamp list
     *
     * @param n node.
     * @return timestamp list | null
     */
    public static LongLinkedOpenHashSet getNodeTimes(Node n) {
        LongLinkedOpenHashSet timestamps = new LongLinkedOpenHashSet();
        if (map_node_time.containsKey(n.getId())) {
            timestamps = map_node_time.get(n.getId());
        }
        return timestamps;
    }

    /**
     * Calculate the edge's presence schedule P(e).
     *
     * @param e edge.
     * @return presenceschedule.
     */
    public static int[] getEdgePresenceSchedule(Edge e, long start_time, long end_time, long duration) {

        int[] presenceSchedule;
        LongLinkedOpenHashSet edge_time = Dynamic.getEdgeTimes(e);
        if (edge_time.isEmpty()) {
            throw new NullPointerException("The edge doesn't exist in no time");
        } else {
            presenceSchedule = new int[(int) (duration)];
            /**
             * For each edge calculate the array contain 1 if the edge exist at
             * time t, 0 otherwise.
             */
            int j = 0;
            for (long time = start_time; time <= end_time; time = time + 1) {
                if (!edge_time.contains(time)) {
                    presenceSchedule[j] = 0;
                } else {
                    presenceSchedule[j] = 1;
                }
                j += 1;
            }
            return presenceSchedule;
        }

    }

    /**
     * Calculate the node's presence schedule P(n).
     *
     * @param n node.
     * @return presenceschedule.
     */
    public static int[] getNodePresenceSchedule(Node n, long start_time, long end_time, long duration) {
        int[] presenceSchedule;
        LongLinkedOpenHashSet node_time = Dynamic.getNodeTimes(n);
        if (node_time.isEmpty()) {
            throw new NullPointerException("The node doesn't exist in no time");
        } else {
            presenceSchedule = new int[(int) (duration)];
            /**
             * For each edge calculate the array contain 1 if the edge exist at
             * time t, 0 otherwise.
             */
            int j = 0;
            for (long time = start_time; time <= end_time; time = time + 1) {
                if (!node_time.contains(time)) {
                    presenceSchedule[j] = 0;
                } else {
                    presenceSchedule[j] = 1;
                }
                j += 1;
            }
            return presenceSchedule;
        }

    }

    /**
     * Return a graph view at time t.
     *
     * @param graph istance of Graph.
     * @param attributeModel
     * @param time time t.
     */
    public static Graph getSnapshotGraph(Graph graph, long time) {
        graph.readUnlockAll();
        GraphModel model = graph.getGraphModel();
        GraphView sourceView = graph.getView();
        GraphView currentView = model.copyView(sourceView);
        Graph sgraph = model.getGraph(sourceView);
        Graph vgraph = model.getGraph(currentView);
        for (Node n : sgraph.getNodes().toArray()) {
            LongLinkedOpenHashSet timestamp = Dynamic.getNodeTimes(n);
            if (!timestamp.isEmpty() && !timestamp.contains(time)) {
//                    LOGGER.log(Level.INFO, "nodo:{0} time: {1}", new Object[]{n.getNodeData().getId(), time});
                vgraph.removeNode(n);
            }
        }
        for (Edge e : sgraph.getEdges().toArray()) {
            LongLinkedOpenHashSet timestamp = Dynamic.getEdgeTimes(e);
            //                LOGGER.log(Level.INFO, "edge:{0},{1} time: {2}", new Object[]{e.getSource().getNodeData().getId(), e.getTarget().getNodeData().getId(), timestamp.isEmpty()});
            if (vgraph.contains(e) && !timestamp.isEmpty() && !timestamp.contains(time)) {
                vgraph.removeEdge(e);
            }
        }
        graph.readLock();
        return vgraph;
    }

    /**
     * Return the id of most frequency edge.
     *
     * @return edgeID|-1. -1 if the graph structure don't exist.
     */
    public static int getMostFrequencyEdgeID() {
        if (!map_edge_time.isEmpty()) {
            sorted_map_frequency_edge = new TreeMap<Integer, Integer>();
            Set<SortedMap.Entry<Integer, LongLinkedOpenHashSet>> set_frequency = map_edge_time.entrySet();
            for (SortedMap.Entry<Integer, LongLinkedOpenHashSet> element : set_frequency) {
                sorted_map_frequency_edge.put(element.getValue().size(), element.getKey());
            }
            return sorted_map_frequency_edge.get(sorted_map_frequency_edge.lastKey());
        } else {
            return -1;
        }
    }

    /**
     * Return the set of first N most frequency edge's id.
     *
     * @param n set's size.
     * @param arr_edgesID ID list of a sample of Edges.
     * @return array with edgeID|null. null if the graph structure doesn't exist.
     */
    public static IntLinkedOpenHashSet getNMostFrequencyEdgeID(int n, IntLinkedOpenHashSet arr_edgesID) {
        IntLinkedOpenHashSet arr_NMostFrequencyEdge = null;
        if (!map_edge_time.isEmpty() && map_edge_time.size() >= n && arr_edgesID.size() >= n) {
            arr_NMostFrequencyEdge = new IntLinkedOpenHashSet();
            //edges ordered by edges id
            map_edge_frequency = new HashMap<Integer, Integer>();
            Set<SortedMap.Entry<Integer, LongLinkedOpenHashSet>> set_frequency = map_edge_time.entrySet();
            for (SortedMap.Entry<Integer, LongLinkedOpenHashSet> element : set_frequency) {
                if (arr_edgesID != null) {
                    if (arr_edgesID.contains(element.getKey())) {
                        map_edge_frequency.put(element.getKey(), element.getValue().size());
                    }

                } else {
                    map_edge_frequency.put(element.getKey(), element.getValue().size());
                }
            }
            //edges descending ordered by frequency
            //Sorted map contains the edges as key, ordered by edge's frequency value
            SortedMapInteger map = new SortedMapInteger(map_edge_frequency);
            Map<Integer, Integer> sorted_map = map.sortByValueDescending();
//            System.out.println("results");
            //take only first n
            int i = 0;
            for (Integer key : sorted_map.keySet()) {
                if (i == n) {
                    break;
                }
//                System.out.println("key/value: " + key + "/" + sorted_map.get(key));
                arr_NMostFrequencyEdge.add(key);
                i += 1;
            }
            System.out.println("arr_NMostFrequencyEdge size: " + arr_NMostFrequencyEdge.size());
            return arr_NMostFrequencyEdge;
        } else {
            return arr_NMostFrequencyEdge;
        }
    }
}
