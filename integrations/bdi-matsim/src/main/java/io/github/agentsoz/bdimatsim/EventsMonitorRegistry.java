package io.github.agentsoz.bdimatsim;

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
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.agentsoz.bdimatsim.EventsMonitorRegistry.MonitoredEventType.*;

/**<p>
 * Acts as a simple listener for Matsim agent events then
 *         passes to MATSimAgentManager
 *         </p>
 *
 * @author Edmund Kemsley
 */
public final class EventsMonitorRegistry implements BasicEventHandler
{
	
	public enum MonitoredEventType {
		// yyyy I am resurrecting this comment, sorry: I still can't see a reason why not to use matsim events directly here: I think that this "adapter"
		// approach just makes it less general, without buying anything in return.  kai, may'19
		AgentInCongestionEvent,
		ActivityEndEvent,
		LinkEnterEvent,
		LinkLeaveEvent,
		NextLinkBlockedEvent,
		PersonArrivalEvent,
		PersonDepartureEvent,
	}
	
	private static final Logger log = LoggerFactory.getLogger(EventsMonitorRegistry.class ) ;

	private final Map<MonitoredEventType, Map<Id<Person>,Monitor>> monitors = Collections.synchronizedMap(new LinkedHashMap<>());

	private final Map<MonitoredEventType, Map<Id<Person>,Monitor>> toAdd = Collections.synchronizedMap(new LinkedHashMap<>());
    
	public EventsMonitorRegistry() {
    	// supposedly works like this:
//		((ch.qos.logback.classic.Logger) log).setLevel(ch.qos.logback.classic.Level.DEBUG);
		// so the slf4j interface does not have these commands, but the logback implementation does.
		// kai, based on pointer by dhirendra
	}

	private Id<Person> getDriverOfVehicle( Id<Vehicle> vehicleId ) {
		return vehicle2Driver.getDriverOfVehicle(vehicleId);
	}
	
	private Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler() ;

	@Override public final void reset(int iteration) { }


	@Override public void handleEvent( Event event ) {
		callRegisteredHandlers(event);
	}

	
	private void callRegisteredHandlers(Event ev) {
		// Register any new monitors waiting to be added
		// Synchronise on toAdd which is allowed to be updated by other threads
		synchronized (toAdd) {
			for(MonitoredEventType eventType : toAdd.keySet()) {
				if (!monitors.containsKey(eventType)) {
					monitors.put(eventType, new ConcurrentHashMap<>());
				}
				Map<Id<Person>, Monitor> map = toAdd.get(eventType);
				for (Id<Person> agentId : map.keySet()) {
					Monitor monitor = map.get(agentId);
					monitors.get(eventType).put(agentId,monitor);
				}
			}
			toAdd.clear();
		}

		if (ev instanceof AgentInCongestionEvent && monitors.containsKey(AgentInCongestionEvent)) {
			handleAgentInCongestionEvent((AgentInCongestionEvent) ev);

		} else if (ev instanceof NextLinkBlockedEvent && monitors.containsKey(NextLinkBlockedEvent)) {
			handleNextLinkBlockedEvent((NextLinkBlockedEvent) ev);

		} else if (ev instanceof LinkEnterEvent && monitors.containsKey(LinkEnterEvent)) {
			handleLinkEnterEvent((LinkEnterEvent) ev);

		} else if (ev instanceof LinkLeaveEvent && monitors.containsKey(LinkLeaveEvent)) {
			handleLinkLeaveEvent((LinkLeaveEvent) ev);

		} else if (ev instanceof PersonArrivalEvent && monitors.containsKey(PersonArrivalEvent)) {
			handlePersonArrivalEvent((PersonArrivalEvent) ev);

		} else if (ev instanceof PersonDepartureEvent && monitors.containsKey(PersonDepartureEvent)) {
			handlePersonDepartureEvent((PersonDepartureEvent) ev);

		} else if (ev instanceof ActivityEndEvent && monitors.containsKey(ActivityEndEvent)) {
			handleActivityEndEvent((ActivityEndEvent) ev);

		} else if (ev instanceof VehicleEntersTrafficEvent) {
			vehicle2Driver.handleEvent((VehicleEntersTrafficEvent)ev) ;

		} else if (ev instanceof VehicleLeavesTrafficEvent) {
			vehicle2Driver.handleEvent((VehicleLeavesTrafficEvent)ev) ;
		}

	}

	private void handleAgentInCongestionEvent( AgentInCongestionEvent ev) {
		removeMonitor( this.getDriverOfVehicle( ev.getVehicleId() ), MonitoredEventType.AgentInCongestionEvent, ev.getCurrentLinkId() );
	}

	private void handleNextLinkBlockedEvent(NextLinkBlockedEvent ev) {
		removeMonitor( ev.getDriverId(), MonitoredEventType.NextLinkBlockedEvent, ev.currentLinkId() );
	}

	private void handleLinkEnterEvent(LinkEnterEvent ev) {
		removeMonitor( this.getDriverOfVehicle( ev.getVehicleId() ), MonitoredEventType.LinkEnterEvent, ev.getLinkId() );
	}

	private void handleLinkLeaveEvent(LinkLeaveEvent ev) {
		removeMonitor( this.getDriverOfVehicle( ev.getVehicleId() ), MonitoredEventType.LinkLeaveEvent, ev.getLinkId() );
	}

	private void handlePersonArrivalEvent(PersonArrivalEvent ev) {
		removeMonitor( ev.getPersonId(), PersonArrivalEvent, ev.getLinkId() );
	}

	public boolean hasPersonArrivalEventMonitorFor(String agentId) {
		Id<Person> driverId = Id.createPersonId(agentId);
		Gbl.assertNotNull(driverId);
		return monitors.get(PersonArrivalEvent) != null && monitors.get(PersonArrivalEvent).containsKey(driverId);
	}


	public void removePersonArrivalEventMonitorFor(String agentId) {
		if (hasPersonArrivalEventMonitorFor(agentId)){
			Id<Person> driverId = Id.createPersonId(agentId);
			synchronized (monitors.get(PersonArrivalEvent)) {
//				monitors.get(PersonArrivalEvent).entrySet().remove(driverId);
				monitors.get(PersonArrivalEvent).remove(driverId);
			}
		}
	}

	private void handlePersonDepartureEvent(PersonDepartureEvent ev) {
		removeMonitor( ev.getPersonId(), PersonDepartureEvent, ev.getLinkId() );
	}

	private void handleActivityEndEvent(ActivityEndEvent ev) {
		removeMonitor( ev.getPersonId(), ActivityEndEvent, ev.getLinkId() );
	}

	/**
	 * For a given agent, registers a {@link BDIPerceptHandler} to be called
	 * whenever an event of type {@link MonitoredEventType} is triggered
	 * for {@code linkId}.
	 * @param agentId
	 * @param linkId
	 * @param event
	 * @param handler
	 * @return
	 */
	public int registerMonitor(String agentId, MonitoredEventType event,String linkId, BDIPerceptHandler handler) {
		synchronized (toAdd) {
			if (!toAdd.containsKey(event)) {
				toAdd.put(event, new ConcurrentHashMap<>());
			}
			Map<Id<Person>, Monitor> map = toAdd.get(event);

			map.put(Id.createPersonId(agentId), new Monitor(agentId, linkId, event, handler));
			toAdd.put(event, map);
			return toAdd.size();
		}
	}

	private void removeMonitor( Id<Person> personId, MonitoredEventType monitoredEventType, Id<Link> linkId ){
		Gbl.assertNotNull( personId );
		Monitor monitor = monitors.get( monitoredEventType ).get( personId );
		if (monitor != null) {
			if (monitor.getAgentId().equals( personId ) && monitor.getLinkId().equals( linkId )) {
				if (monitor.getHandler().handle(monitor.getAgentId(), monitor.getLinkId(), monitor.getEvent())) {
					synchronized (monitors.get( monitoredEventType )) {
//						monitors.get( monitoredEventType ).entrySet().remove( personId );
						monitors.get( monitoredEventType ).remove( personId ); // !!
					}
				}
			}
		}
	}

	/**
	 * Internal structure used to store information about MATSim events to monitor
	 * @author dsingh
	 *
	 */
	private class Monitor {

		private Id<Person> agentId;
		private Id<Link> linkId;
		private MonitoredEventType event;
		private BDIPerceptHandler handler;
		
		Monitor( String agentId2, String linkId, MonitoredEventType event, BDIPerceptHandler handler ) {
			super();

			this.agentId = Id.createPersonId(agentId2);
			// (this is now one of the places where the bdi ids (= Strings) get translated into matsim ids)
			
			if ( linkId!=null ) {
				this.linkId = Id.createLinkId(linkId);
			}
			this.event = event;
			this.handler = handler;
		}
		
		public Id<Person> getAgentId() {
			return agentId;
		}
		public Id<Link> getLinkId() {
			return linkId;
		}
		public MonitoredEventType getEvent() {
			return event;
		}
		public BDIPerceptHandler getHandler() {
			return handler;
		}
	}
}
