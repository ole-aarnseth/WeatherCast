package com.oleaarnseth.weathercast;

/*
    Denne klassen inneholder lokasjonsdataene bredde (latitude) og lengde (longitude) fra brukeren,
    og gis som parameter til AsyncTasken i WeatherAPIHandlerFragment når den sender http-request
    til WeatherAPI:
 */
public class Location {
    private double lat, lon;

    // Konstruktør:
    public Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() { return lat; }

    public double getLon() { return lon; }
}
