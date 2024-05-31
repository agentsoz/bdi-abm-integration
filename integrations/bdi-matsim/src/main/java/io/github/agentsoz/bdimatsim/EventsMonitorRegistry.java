package io.github.agentsoz.bdimatsim;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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

import io.github.agentsoz.nonmatsim.BDIPerceptHandler;
import io.github.agentsoz.nonmatsim.EventData;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
		AgentInCongestionEvent,
		ActivityEndEvent,
		ActivityStartEvent,
		LinkEnterEvent,
		LinkLeaveEvent,
		NextLinkBlockedEvent,
		PersonArrivalEvent,
		PersonDepartureEvent,
		PersonStuckEvent,
		TotalLinkLengthTraveledEvent,

		// a reason for having this here is that one can work on objects of type MonitoredEventType rather than Class<? extends Event>, since the first is
		// conceptually simpler.   There is also no way around handing each event type separately, since the syntax to get driverId and linkId out of
		// them is not uniform across events, see below.  kai, may'19
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
			for (MonitoredEventType eventType : toAdd.keySet()) {
				if (!monitors.containsKey(eventType)) {
					monitors.put(eventType, new ConcurrentHashMap<>());
				}
				Map<Id<Person>, Monitor> map = toAdd.get(eventType);
				for (Id<Person> agentId : map.keySet()) {
					Monitor monitor = map.get(agentId);
					monitors.get(eventType).put(agentId, monitor);
				}
			}
			toAdd.clear();
		}

		EventData eventData = new EventData(ev.getTime(), ev.getEventType(), ev.getAttributes());

		if (ev instanceof AgentInCongestionEvent && monitors.containsKey(MonitoredEventType.AgentInCongestionEvent)) {
			final io.github.agentsoz.bdimatsim.AgentInCongestionEvent event = (AgentInCongestionEvent) ev;
			handleEventAndRemoveMonitor(this.getDriverOfVehicle(event.getVehicleId()), MonitoredEventType.AgentInCongestionEvent, event.getCurrentLinkId(), eventData);

		} else if (ev instanceof NextLinkBlockedEvent && monitors.containsKey(MonitoredEventType.NextLinkBlockedEvent)) {
			final io.github.agentsoz.bdimatsim.NextLinkBlockedEvent event = (NextLinkBlockedEvent) ev;
			handleEventAndRemoveMonitor(event.getDriverId(), MonitoredEventType.NextLinkBlockedEvent, event.currentLinkId(), eventData);

		} else if (ev instanceof LinkEnterEvent && monitors.containsKey(MonitoredEventType.LinkEnterEvent)) {
			final org.matsim.api.core.v01.events.LinkEnterEvent event = (LinkEnterEvent) ev;
			handleEventAndRemoveMonitor(this.getDriverOfVehicle(event.getVehicleId()), MonitoredEventType.LinkEnterEvent, event.getLinkId(), eventData);

		} else if (ev instanceof LinkLeaveEvent && monitors.containsKey(MonitoredEventType.LinkLeaveEvent)) {
			final org.matsim.api.core.v01.events.LinkLeaveEvent event = (LinkLeaveEvent) ev;
			handleEventAndRemoveMonitor(this.getDriverOfVehicle(event.getVehicleId()), MonitoredEventType.LinkLeaveEvent, event.getLinkId(), eventData);

		} else if (ev instanceof PersonArrivalEvent && monitors.containsKey(MonitoredEventType.PersonArrivalEvent)) {
			final org.matsim.api.core.v01.events.PersonArrivalEvent event = (PersonArrivalEvent) ev;
			handleEventAndRemoveMonitor(event.getPersonId(), MonitoredEventType.PersonArrivalEvent, event.getLinkId(), eventData);
			//added -oemer
	    }else if (ev instanceof TotalLinkLengthTraveledEvent && monitors.containsKey(MonitoredEventType.TotalLinkLengthTraveledEvent)) {
			final TotalLinkLengthTraveledEvent event = (TotalLinkLengthTraveledEvent) ev;
			handleEventAndRemoveMonitor(event.getPersonId(), MonitoredEventType.TotalLinkLengthTraveledEvent, event.currentLinkId(), eventData);

		}else if (ev instanceof PersonDepartureEvent && monitors.containsKey(MonitoredEventType.PersonDepartureEvent)) {
			final org.matsim.api.core.v01.events.PersonDepartureEvent event = (PersonDepartureEvent) ev;
			handleEventAndRemoveMonitor( event.getPersonId(), MonitoredEventType.PersonDepartureEvent, event.getLinkId(), eventData );

		} else if (ev instanceof ActivityEndEvent && monitors.containsKey(MonitoredEventType.ActivityEndEvent)) {
			final org.matsim.api.core.v01.events.ActivityEndEvent event = (ActivityEndEvent) ev;
			handleEventAndRemoveMonitor( event.getPersonId(), MonitoredEventType.ActivityEndEvent, event.getLinkId(), eventData );

		} else if (ev instanceof ActivityStartEvent && monitors.containsKey(MonitoredEventType.ActivityStartEvent)) {
			final org.matsim.api.core.v01.events.ActivityStartEvent event = (ActivityStartEvent) ev;
			handleEventAndRemoveMonitor( event.getPersonId(), MonitoredEventType.ActivityStartEvent, event.getLinkId(), eventData );

		} else if (ev instanceof VehicleEntersTrafficEvent) {
			vehicle2Driver.handleEvent((VehicleEntersTrafficEvent)ev) ;

		} else if (ev instanceof VehicleLeavesTrafficEvent) {
			vehicle2Driver.handleEvent((VehicleLeavesTrafficEvent) ev);

		} else if (ev instanceof PersonStuckEvent) {
			final org.matsim.api.core.v01.events.PersonStuckEvent event = (PersonStuckEvent) ev;
			handleEventAndRemoveMonitor( event.getPersonId(), MonitoredEventType.PersonStuckEvent, event.getLinkId(), eventData );

		} else {
			// Not throwing all else, which includes also cases of all events above
			// but without the monitor, as well as other events like "time"; dhi 21/may/19
			// throw new RuntimeException( "Handler for event not implemented:" + ev ) ;
		}

	}

	public boolean hasPersonArrivalEventMonitorFor(String agentId) {
		Id<Person> driverId = Id.createPersonId(agentId);
		Gbl.assertNotNull(driverId);
		return monitors.get(MonitoredEventType.PersonArrivalEvent) != null && monitors.get(MonitoredEventType.PersonArrivalEvent).containsKey(driverId);
	}


	public void removePersonArrivalEventMonitorFor(String agentId) {
		if (hasPersonArrivalEventMonitorFor(agentId)){
			Id<Person> driverId = Id.createPersonId(agentId);
			synchronized (monitors.get(MonitoredEventType.PersonArrivalEvent)) {
//				monitors.get(PersonArrivalEvent).entrySet().remove(driverId);
				monitors.get(MonitoredEventType.PersonArrivalEvent).remove(driverId);

				// yyyyyy I am somewhat certain that the commented out syntax did not do as expected.  kai, may'19

				// Oops, dhi 21/may/19
			}
		}

		// yy maybe this could be the same as handleEventAndRemoveMonitor below, but it has a slightly different semantics.  kai, may'19

		// I have renamed the function below to make the distinction obvious,
		// i.e., here we do not handle the event but just remove the monitor; dhi 21/may/19
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

	/**
	 * Handles and removes a monitored event for a given person on a given link.
	 * @param personId the person the event is related to
	 * @param monitoredEventType the monitored event to handle and remove
	 * @param linkId the link associated with the event
	 */
	private void handleEventAndRemoveMonitor(Id<Person> personId, MonitoredEventType monitoredEventType, Id<Link> linkId, EventData event ){
		if (personId != null) {
			Monitor monitor = monitors.get(monitoredEventType).get(personId);
			if (monitor != null) {
				// match personId and (optionally) linkId if the monitor has an associated link id
				if (monitor.getAgentId().equals(personId) && (monitor.getLinkId() == null || monitor.getLinkId().equals(linkId))) {
					// always pass the linkId of this event to the handler
					String link = (linkId == null) ? null : linkId.toString();

					if (monitor.getHandler().handle(monitor.getAgentId().toString(), link, monitor.getEvent(), event)) {
						synchronized (monitors.get(monitoredEventType)) {
							monitors.get(monitoredEventType).remove(personId);
						}
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
