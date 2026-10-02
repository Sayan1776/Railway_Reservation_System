package com.railway.model;

/** Coach classes. The code is what the database stores ('SL', '3A', ...). */
public enum SeatClass {
    SL("SL", "Sleeper"),
    AC3("3A", "AC 3 Tier"),
    AC2("2A", "AC 2 Tier"),
    AC1("1A", "AC First Class");

    private final String code;
    private final String displayName;

    SeatClass(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SeatClass fromCode(String code) {
        for (SeatClass sc : values()) {
            if (sc.code.equalsIgnoreCase(code)) {
                return sc;
            }
        }
        throw new IllegalArgumentException("Unknown seat class: " + code);
    }

    @Override
    public String toString() {
        return code + " (" + displayName + ")";
    }
}