package com.railway.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class Train {

    private final int trainNumber;
    private String name;
    private String runsOn;      // 7 chars, Monday..Sunday, e.g. "YNYNYNY"
    private boolean active;     // false = soft-deleted
    private Route route;        // filled in by the repository; may be null
    private final Map<SeatClass, TrainClass> classes = new EnumMap<>(SeatClass.class);

    public Train(int trainNumber, String name, String runsOn, boolean active) {
        this.trainNumber = trainNumber;
        this.name = Require.text(name, "name");
        this.runsOn = requireRunsOn(runsOn);
        this.active = active;
    }

    public int getTrainNumber() { return trainNumber; }
    public String getName() { return name; }
    public String getRunsOn() { return runsOn; }
    public boolean isActive() { return active; }
    public Route getRoute() { return route; }

    public void setName(String name) { this.name = Require.text(name, "name"); }
    public void setRunsOn(String runsOn) { this.runsOn = requireRunsOn(runsOn); }
    public void setActive(boolean active) { this.active = active; }
    public void setRoute(Route route) { this.route = route; }

    public void addClass(TrainClass trainClass) {
        Require.notNull(trainClass, "trainClass");
        classes.put(trainClass.getSeatClass(), trainClass);
    }

    public Optional<TrainClass> getTrainClass(SeatClass seatClass) {
        return Optional.ofNullable(classes.get(seatClass));
    }

    public Collection<TrainClass> getClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /** Does this train run on the given calendar date (day of departure from origin)? */
    public boolean runsOn(LocalDate date) {
        return runsOn.charAt(date.getDayOfWeek().getValue() - 1) == 'Y';
    }

    public Station getSource() {
        return route == null ? null : route.getOrigin().getStation();
    }

    public Station getDestination() {
        return route == null ? null : route.getDestination().getStation();
    }

    private static String requireRunsOn(String runsOn) {
        String r = Require.text(runsOn, "runsOn").toUpperCase();
        if (!r.matches("[YN]{7}")) {
            throw new IllegalArgumentException("runsOn must be 7 characters of Y/N, Monday to Sunday");
        }
        return r;
    }

    @Override
    public String toString() {
        return trainNumber + " " + name;
    }
}