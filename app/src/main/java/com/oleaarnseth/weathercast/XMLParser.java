package com.oleaarnseth.weathercast;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/*
Parser-klasse som parser all XML-data fra WeatherAPI.
 */
public class XMLParser {
    public static final String NAMESPACE = null;

    // Start-tag for XML-data:
    public static final String START_TAG_WEATHERDATA = "weatherdata";

    // Alle værvarselsoppføringer starter med en "time"-tag:
    public static final String TAG_FORECAST = "time";

    // "to"-attributt for time-elementet:
    public static final String ATTRIBUTE_TIME_TO = "to";

    public static final String TAG_TEMPERATURE = "temperature";
    public static final String TAG_WINDSPEED = "windSpeed";

    public static final String ATTRIBUTE_TEMPERATURE = "value";
    public static final String ATTRIBUTE_WINDSPEED = "mps";

    public Forecast parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            return readXmlFeed(parser);
        }
        finally {
            in.close();
        }
    }

    private Forecast readXmlFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        Forecast forecast = null;
        parser.require(XmlPullParser.START_TAG, NAMESPACE, START_TAG_WEATHERDATA);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(TAG_FORECAST)) {

                // Les inn ny forecast
                if (forecast == null) {
                    forecast = readForecast(parser);
                }

                /* Etter at værvarselet har blitt lest inn vil neste "time"-tag
                   i XML-feeden inneholde nedbør (precipitation) og værikon for
                   gjeldende tidsrom: */
                else if (parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TIME_TO).equals(forecast.getTime())) {
                    readExtras(parser, forecast);
                }
            }

            // Avbryt løkke når forecast-objektet er komplettert fra XML-data:
            if (forecast != null && forecast.getIconNumber() != -1) {
                break;
            }
        }

        return forecast;
    }

    private Forecast readForecast(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_FORECAST);

        String time = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TIME_TO);
        double temperature = 0.0;
        double windspeed = 0.0;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals(TAG_TEMPERATURE)) {
                temperature = readTemperature(parser);
            }
            else if (name.equals(TAG_WINDSPEED)) {
                windspeed = readWindSpeed(parser);
            }
            else {
                skip(parser);
            }
        }

        return new Forecast(time, temperature, windspeed);
    }

    private double readTemperature(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_TEMPERATURE);

        String temperatureStr = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TEMPERATURE);
        // If this goes wrong add Double.parseDouble(temperatureStr.substring(1, temperatureStr.length() - 1));
        double temperature = Double.parseDouble(temperatureStr);
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, NAMESPACE, TAG_TEMPERATURE);
        return temperature;
    }

    private double readWindSpeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_WINDSPEED);

        String windspeedStr = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_WINDSPEED);
        // If this goes wrong add Double.parseDouble(windspeedStr.substring(1, windspeedStr.length() - 1));
        double windspeed = Double.parseDouble(windspeedStr);
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, NAMESPACE, TAG_WINDSPEED);
        return windspeed;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;

        while (depth > 0) {
            int event = parser.next();

            if (event == XmlPullParser.END_TAG) {
                depth--;
            }
            else if (event == XmlPullParser.START_TAG) {
                depth++;
            }
        }
    }

    private void readExtras(XmlPullParser parser, Forecast forecast) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_FORECAST);

        if (!parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TIME_TO).equals(forecast.getTime())) {
            throw new IllegalStateException("Forecast time does not match time for selected entry!");
        }

    }
}
