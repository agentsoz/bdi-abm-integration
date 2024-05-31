package io.github.agentsoz.bdimatsim;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
//import org.matsim.core.api.internal.HasPersonId;
//import org.matsim.vehicles.Vehicle;

import java.util.Map;

public final class TotalLinkLengthTraveledEvent extends Event {
    public  static final String EVENT_TYPE = "TotalDistanceTraveled";
    private final Id<Person> driverId;
    private final Id<Link> currentLinkId;
    private final double totalLinkLengthTraveled;

    public TotalLinkLengthTraveledEvent(double time, Id<Person> driverId, Id<Link> currentLinkId, double totalLinkLengthTraveled) {
        super(time);
        this.driverId = driverId;
        this.currentLinkId = currentLinkId;
        this.totalLinkLengthTraveled = totalLinkLengthTraveled;
    }

    public Id<Person> getPersonId() {
        return driverId;
    }

    public double getTotalLinkLengthTraveled() {
        return totalLinkLengthTraveled;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public Id<Link> currentLinkId() {
        return this.currentLinkId ;
    }


    public Map<String, String> getAttributes() {
        Map<String, String> attrs = super.getAttributes();
        attrs.put("person", this.driverId.toString());
        attrs.put("link", this.currentLinkId.toString());
        attrs.put("totalLinkLengthTraveled",  String.valueOf(this.totalLinkLengthTraveled));
        return attrs;
    }


}