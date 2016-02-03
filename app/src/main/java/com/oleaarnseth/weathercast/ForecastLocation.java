package com.oleaarnseth.weathercast;

/*
    Denne klassen inneholder lokasjonsdataene bredde (latitude) og lengde (longitude) fra brukeren,
    og gis som parameter til AsyncTasken i WeatherAPIHandlerFragment når den sender http-request
    til WeatherAPI:
 */
public class ForecastLocation {
    private double lat, lon;

    // Konstruktør:
    public ForecastLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() { return lat; }

    public double getLon() { return lon; }

    @Override
    public String toString() {
        return "Latitude: "
                + lat
                + "\nLongitutde: "
                + lon;
    }
}
