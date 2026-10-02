package com.railway.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** A train's full path: an ordered list of stops. */
public class Route {

    private final int trainNumber;
    private final List<RouteStop> stops;

    public Route(int trainNumber, List<RouteStop> stops) {
        Require.notNull(stops, "stops");
        if (stops.size() < 2) {
            throw new IllegalArgumentException("A route needs at least 2 stops");
        }
        List<RouteStop> sorted = new ArrayList<>(stops);
        sorted.sort(Comparator.comparingInt(RouteStop::getStopOrder));
        this.trainNumber = trainNumber;
        this.stops = Collections.unmodifiableList(sorted);
    }

    public int getTrainNumber() { return trainNumber; }
    public List<RouteStop> getStops() { return stops; }

    public RouteStop getOrigin() { return stops.get(0); }
    public RouteStop getDestination() { return stops.get(stops.size() - 1); }

    public Optional<RouteStop> findStop(String stationCode) {
        for (RouteStop s : stops) {
            if (s.getStation().getCode().equalsIgnoreCase(stationCode)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /** True if both stations are on this route and 'from' comes before 'to'. */
    public boolean connects(String fromCode, String toCode) {
        Optional<RouteStop> from = findStop(fromCode);
        Optional<RouteStop> to = findStop(toCode);
        return from.isPresent() && to.isPresent()
                && from.get().getStopOrder() < to.get().getStopOrder();
    }

    /** Distance in km between two stops of this route. */
    public int distanceBetween(String fromCode, String toCode) {
        if (!connects(fromCode, toCode)) {
            throw new IllegalArgumentException(
                    "Train " + trainNumber + " does not run from " + fromCode + " to " + toCode);
        }
        return findStop(toCode).get().getDistanceKm() - findStop(fromCode).get().getDistanceKm();
    }
}