package io.github.agentsoz.dataInterface;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

public class DataServer {

    private double time = 0.0;
    private double timeStep = 1.0;
    private final String DATATYPE_TIME = "time";
    private Map<String, List<DataClient>> subscriptions = new LinkedHashMap<>();
    private SortedMap<Double, Map<String, List<DataSource>>> timedUpdates = new ConcurrentSkipListMap<>();
    private static Map<String, DataServer> servers = new LinkedHashMap<>();

    private DataServer(String name) {
    }

    public static DataServer getInstance(String name) {
        if (!servers.containsKey(name)) {
            servers.put(name, new DataServer(name));
        }
        return servers.get(name);
    }

    public static void cleanup() {
        servers.clear();
    }

    // subscribe the given DataClient to data updates of a given type
    // updates are sent immediately on being published
    public void subscribe(DataClient client, String dataType) {
        if (subscriptions.containsKey(dataType)) {
            subscriptions.get(dataType).add(client);
        } else {
            List<DataClient> subscribers = new ArrayList<>();
            subscribers.add(client);
            subscriptions.put(dataType, subscribers);
        }
    }

    // register a source for a future timed update
    // the data server will query this source at the specified time
    public void registerTimedUpdate(String dataType, DataSource source, double nextUpdate) {
        if (!timedUpdates.containsKey(nextUpdate)) {
            timedUpdates.put(nextUpdate, new LinkedHashMap<>());
        }
        if (!timedUpdates.get(nextUpdate).containsKey(dataType)) {
            timedUpdates.get(nextUpdate).put(dataType, new ArrayList<>());
        }
        timedUpdates.get(nextUpdate).get(dataType).add(source);
    }

    // when the data server's time moves forward, all data sources are polled for new updates
    // new updates are published immediately
    private void updateTimedSources() {
        // check for new timed updates; publish and retrieve any that are found
        while (!timedUpdates.isEmpty() && timedUpdates.firstKey() < time) {
            double t = timedUpdates.firstKey();
            Iterator<Entry<String, List<DataSource>>> i = timedUpdates.get(t).entrySet().iterator();
            while (i.hasNext()) {
                Entry<String, List<DataSource>> sourcesForType = i.next();
                String dataType = sourcesForType.getKey();
                for (DataSource source : sourcesForType.getValue()) {
                    Object data = source.sendData(time, dataType);
                    if (data != null) {
                        publish(dataType, data);
                    }
                }
            }
            timedUpdates.remove(t);
        }
    }

    public void stepTime() {
        // update the internal clock
        time += timeStep;
        // publish the time to all interested parties
        publish(DATATYPE_TIME, getTime());
        // move date from publishers to subscribers
        updateTimedSources();
    }

    // manually set the data server's time to a given value and poll for new timed updates
    public void setTime(double t) {
        if (t < 0) {
            throw new RuntimeException("Attempt to set time to negative value ("+t+")");
        }
        time = t;
    }

    public void setTimeStep(double t) {
        if (t <= 0.0) {
            throw new RuntimeException("Attempt to set time step to zero or less ("+t+")");
        }
        timeStep = t;
    }

    public double getTime() {
        return time;
    }

    // send a new data update to all clients that have subscribed to the data type
    public void publish(String dataType, Object data) {
        if (!subscriptions.containsKey(dataType)) {
            return;
        }
        for (DataClient dc : subscriptions.get(dataType)) {
            dc.receiveData(time, dataType, data);
        }
    }
}
