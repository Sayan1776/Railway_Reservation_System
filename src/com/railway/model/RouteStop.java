package com.railway.model;

import java.time.LocalTime;

/** One station on a train's path: a row of train_stops. */
public class RouteStop {

    private final int stopOrder;
    private final Station station;
    private final LocalTime arrivalTime;     // null at the origin
    private final LocalTime departureTime;   // null at the terminus
    private final int dayOffset;             // 0 = day of departure, 1 = next day
    private final int distanceKm;            // from the origin

    public RouteStop(int stopOrder, Station station, LocalTime arrivalTime,
                     LocalTime departureTime, int dayOffset, int distanceKm) {
        this.stopOrder = Require.atLeast(stopOrder, 1, "stopOrder");
        this.station = Require.notNull(station, "station");
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.dayOffset = Require.atLeast(dayOffset, 0, "dayOffset");
        this.distanceKm = Require.atLeast(distanceKm, 0, "distanceKm");
    }

    public int getStopOrder() { return stopOrder; }
    public Station getStation() { return station; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public LocalTime getDepartureTime() { return departureTime; }
    public int getDayOffset() { return dayOffset; }
    public int getDistanceKm() { return distanceKm; }

    @Override
    public String toString() {
        return stopOrder + ". " + station.getCode() + " @" + distanceKm + "km";
    }
}