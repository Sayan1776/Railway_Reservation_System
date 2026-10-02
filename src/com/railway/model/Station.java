package com.railway.model;

import java.util.Objects;

public class Station {

    private final String code;   // e.g. "HWH", always uppercase
    private String name;
    private String city;

    public Station(String code, String name, String city) {
        this.code = Require.text(code, "code").toUpperCase();
        this.name = Require.text(name, "name");
        this.city = Require.text(city, "city");
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCity() { return city; }

    public void setName(String name) { this.name = Require.text(name, "name"); }
    public void setCity(String city) { this.city = Require.text(city, "city"); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Station)) return false;
        return code.equals(((Station) o).code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}