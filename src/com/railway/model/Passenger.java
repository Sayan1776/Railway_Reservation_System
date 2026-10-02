package com.railway.model;

public class Passenger {

    public enum Gender {
        MALE("M"), FEMALE("F"), OTHER("O");

        private final String code;

        Gender(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static Gender fromCode(String code) {
            for (Gender g : values()) {
                if (g.code.equalsIgnoreCase(code)) {
                    return g;
                }
            }
            throw new IllegalArgumentException("Unknown gender code: " + code);
        }
    }

    /** Names match the values stored in the database. */
    public enum BerthPreference { NONE, LOWER, MIDDLE, UPPER, SIDE_LOWER, SIDE_UPPER }

    private final Long id;          // null until saved
    private String pnr;             // null until the ticket exists
    private final String fullName;
    private final int age;
    private final Gender gender;
    private final BerthPreference berthPreference;
    private String seatLabel;       // null while waiting

    public Passenger(Long id, String pnr, String fullName, int age, Gender gender,
                     BerthPreference berthPreference, String seatLabel) {
        this.id = id;
        this.pnr = pnr;
        this.fullName = Require.text(fullName, "fullName");
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("age must be between 1 and 120");
        }
        this.age = age;
        this.gender = Require.notNull(gender, "gender");
        this.berthPreference = berthPreference == null ? BerthPreference.NONE : berthPreference;
        this.seatLabel = seatLabel;
    }

    /** Convenience constructor for a passenger being entered at booking time. */
    public Passenger(String fullName, int age, Gender gender, BerthPreference berthPreference) {
        this(null, null, fullName, age, gender, berthPreference, null);
    }

    public Long getId() { return id; }
    public String getPnr() { return pnr; }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public Gender getGender() { return gender; }
    public BerthPreference getBerthPreference() { return berthPreference; }
    public String getSeatLabel() { return seatLabel; }

    public void setPnr(String pnr) { this.pnr = pnr; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }

    @Override
    public String toString() {
        return fullName + ", " + age + ", " + gender.getCode()
                + (seatLabel == null ? "" : ", seat " + seatLabel);
    }
}