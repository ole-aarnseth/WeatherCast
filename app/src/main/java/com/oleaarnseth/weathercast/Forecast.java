package com.oleaarnseth.weathercast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Denne klassen rommer værvarsel-data for et gjeldende tidsrom.
 */
public class Forecast {
    // XML-oppføringens tid lagres som String for enkelthetens skyld:
    private String timeFrom, timeTo;

    // Dato som vises i Spinner i WeatherActivity:
    private String displayDate;

    private double temperature, windspeed, precipitation;

    /* Denne variablelen rommer id-en for værvarselets værikon, som lastes ned fra:
           http://api.yr.no/weatherapi/weathericon/1.1/documentation */
    private int iconNumber;

    // Konstruktør
    public Forecast(String timeFrom, String timeTo, double temperature, double windspeed, double precipitation, int iconNumber) {
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.temperature = temperature;
        this.windspeed = windspeed;
        this.precipitation = precipitation;
        this.iconNumber = iconNumber;
        displayDate = "";
    }


    public String getTimeFrom() { return timeFrom; }

    public String getTimeTo() { return timeTo; }

    public void setDisplayDate(String displayDate) { this.displayDate = displayDate; }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }

    public void setIconNumber(int iconNumber) {
        this.iconNumber = iconNumber;
    }

    public double getPrecipitation() { return precipitation; }

    public int getIconNumber() {
        return iconNumber;
    }

    public String toString() {
        return "Time from: "
                + timeFrom
                + "\nTime to: "
                + timeTo
                + "\nTemperature: "
                + temperature
                + "\nWindspeed: "
                + windspeed
                + "\nPrecipitation: "
                + precipitation
                + "\nIcon number: "
                + iconNumber
                + "\nDisplay date: "
                + displayDate;
    }
}
