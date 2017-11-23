package io.github.agentsoz.dataInterface;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
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

// TODO:
// Add pull mechanism for clients

public class DataServer {
   
   private double                            time          = 0.0;
   private double                            timeStep      = 1.0;
   private Map< String, List< DataClient > > subscriptions = new LinkedHashMap< String, List< DataClient > >();
   private Map< String, List< DataSource > > sources       = new LinkedHashMap< String, List< DataSource > >();
   private SortedMap< Double, Map< String, List< DataSource > > > timedUpdates
      = new ConcurrentSkipListMap< Double, Map< String, List< DataSource > > >();
   private static Map< String, DataServer >  servers       = new LinkedHashMap< String, DataServer >();
   
   public DataServer( String name ) {
      
      servers.put( name, this );
   }
   
   public static DataServer getServer( String name ) {
      
      if (!servers.containsKey( name )) { new DataServer( name ); }
      
      return servers.get( name );
   }
   
   public static void cleanup() {
	   servers.clear(); 
   }

   // subscribe the given DataClient to data updates of a given type
   // updates are sent immediately on being published
   public void subscribe( DataClient client, String dataType ) {
      
      if (subscriptions.containsKey( dataType )) { subscriptions.get( dataType ).add( client ); }
      else {
         List< DataClient > subscribers = new ArrayList< DataClient >();
         subscribers.add( client );
         subscriptions.put( dataType, subscribers );
      }
   }
   public void subscribe( DataClient client, String[] dataTypes ) {
      
      for (String dataType: dataTypes) { subscribe( client, dataType ); }
   }
   
   public boolean setTimeStep( Double newTimeStep ) {
      
      if (newTimeStep < 0.0) { return false; }
      timeStep = newTimeStep;
      return true;
   }
   
   public double getTimeStep() {
      
      return timeStep;
   }
   
   // register a source for passive (timed) data updates
   // sources that actively publish data don't need to register
   public void registerSource( String dataType, DataSource source ) {

      if (sources.containsKey( dataType )) { sources.get( dataType ).add( source ); }
      else {
         List< DataSource > sourcesForType = new ArrayList< DataSource >();
         sourcesForType.add( source );
         sources.put( dataType, sourcesForType );
      }
   }
   
   // register a source for a future timed update
   // the data server will query this source at the specified time
   public void registerTimedUpdate( String dataType, DataSource source, double nextUpdate ) {

      if (!timedUpdates.containsKey( nextUpdate )) {
         timedUpdates.put( nextUpdate, new LinkedHashMap< String, List< DataSource > >() );
      }
      if (!timedUpdates.get( nextUpdate ).containsKey( dataType )) {
         timedUpdates.get( nextUpdate ).put( dataType, new ArrayList< DataSource >() );
      }
      timedUpdates.get( nextUpdate ).get( dataType ).add( source );
   }

   // when the data server's time moves forward, all data sources are polled for new updates
   // new updates are published immediately
   void updateTimedSources() {

      for (String dataType : sources.keySet()) {
         for (DataSource source : sources.get( dataType )) {
            Object data = source.getNewData( time, null );
            if (data != null) { publish( dataType, data ); }
         }
      }
      // check for new timed updates; publish and retrieve any that are found
      while (!timedUpdates.isEmpty() && timedUpdates.firstKey() < time) {
         
         double t = timedUpdates.firstKey();
         Iterator< Entry< String, List< DataSource > > > i = timedUpdates.get( t ).entrySet().iterator();
         
         while (i.hasNext()) {
            
            Entry< String, List< DataSource > > sourcesForType = i.next();
            String dataType = sourcesForType.getKey();
            
            for (DataSource source: sourcesForType.getValue()) {
               
               Object data = source.getNewData( time, null );
               if (data != null) { publish( dataType, data ); }
            }
         }
         timedUpdates.remove( t );
      }
   }
   
   public void stepTime() {
      
      time += timeStep;
      updateTimedSources();
   }
   
   // increase time by a given amount and poll for new timed updates
   public boolean stepTime( double t ) {
      
      if (t <= 0.0) { return false; }
      time += t;
      updateTimedSources();
      return true;
   }
   
   // manually set the data server's time to a given value and poll for new timed updates
   public boolean setTime( double t ) {
      
      if (t <= time) { return false; }
      
      time = t;
      updateTimedSources();
      return true;
   }
   
   public boolean setTimeStep( double t ) {
      
      if (t <= 0.0) { return false; }
      timeStep = t;
      return true;
   }
   
   public double getTime() { return time; }
   
   // send a new data update to all clients that have subscribed to the data type
   public boolean publish( String dataType, Object data ) {
      
      if (!subscriptions.containsKey( dataType )) { return false; }
      log(dataType, data );
      for (DataClient dc : subscriptions.get( dataType )) { dc.dataUpdate( time, dataType, data );}
      return true;
   }
   
   private void log (String dataType, Object data ){
	   //TODO it might be useful to log all message traffic for testing
//	      System.out.println("dataServer: " + dataType);
   }
}
