package com.railway;

import com.railway.model.Train;
import com.railway.model.TrainClass;
import com.railway.repository.TrainRepository;
import com.railway.repository.jdbc.JdbcTrainRepository;

import java.time.LocalDate;
import java.util.List;

public class SearchCheck {

    public static void main(String[] args) {
        TrainRepository repo = new JdbcTrainRepository();

        search(repo, "HWH", "GAYA", LocalDate.of(2026, 10, 5));   // Monday: both trains
        search(repo, "HWH", "GAYA", LocalDate.of(2026, 10, 6));   // Tuesday: only 10001
        search(repo, "GAYA", "HWH", LocalDate.of(2026, 10, 5));   // wrong direction: none
    }

    private static void search(TrainRepository repo, String from, String to, LocalDate date) {
        System.out.println("\n" + from + " -> " + to + " on " + date + " (" + date.getDayOfWeek() + ")");
        List<Train> trains = repo.searchTrains(from, to, date);
        if (trains.isEmpty()) {
            System.out.println("  no trains");
            return;
        }
        for (Train t : trains) {
            System.out.println("  " + t + " | " + t.getSource().getCode() + " -> "
                    + t.getDestination().getCode() + " | "
                    + t.getRoute().distanceBetween(from, to) + " km");
            for (TrainClass tc : t.getClasses()) {
                System.out.println("     " + tc + " | free: "
                        + repo.getAvailableSeats(t.getTrainNumber(), tc.getSeatClass(), date));
            }
        }
    }
}