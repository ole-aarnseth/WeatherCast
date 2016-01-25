package com.oleaarnseth.weathercast;

/**
 * Denne klassen rommer værvarsel-data for et gjeldende tidsrom.
 */
public class Forecast {
    // XML-oppføringens tid lagres som String for enkelthetens skyld:
    private String time;

    private double temperature, windspeed, precipitation;

    /* Denne variablelen rommer id-en for værvarselets værikon, som lastes ned fra:
           http://api.yr.no/weatherapi/weathericon/1.1/documentation */
    private int iconNumber;

    public Forecast(String time, double temperature, double windspeed) {
        this.time = time;
        this.temperature = temperature;
        this.windspeed = windspeed;

        precipitation = 0.0;
        iconNumber = -1;
    }

    public String getTime() {
        return time;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }

    public void setIconNumber(int iconNumber) {
        this.iconNumber = iconNumber;
    }

    public int getIconNumber() {
        return iconNumber;
    }
}
