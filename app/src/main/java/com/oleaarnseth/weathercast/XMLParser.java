package com.oleaarnseth.weathercast;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


// Parser-klasse som parser all XML-data fra WeatherAPI.

public class XMLParser {
    public static final String NAMESPACE = null;

    // Start-tag for XML-data:
    public static final String START_TAG_WEATHERDATA = "weatherdata";

    public static final String TAG_PRODUCT = "product";
    public static final String TAG_FORECAST = "time";
    public static final String TAG_TEMPERATURE = "temperature";
    public static final String TAG_WINDSPEED = "windSpeed";
    public static final String TAG_PRECIPITATION = "precipitation";
    public static final String TAG_SYMBOL = "symbol";

    public static final String ATTRIBUTE_TIME_FROM = "from";
    public static final String ATTRIBUTE_TIME_TO = "to";
    public static final String ATTRIBUTE_TEMPERATURE = "value";
    public static final String ATTRIBUTE_WINDSPEED = "mps";
    public static final String ATTRIBUTE_PRECIPITATION_VALUE = "value";
    public static final String ATTRIBUTE_SYMBOL_NUMBER = "number";

    public ArrayList<Forecast> parse(InputStream in) throws XmlPullParserException, IOException {
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

    private ArrayList<Forecast> readXmlFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, START_TAG_WEATHERDATA);
        ArrayList<Forecast> forecasts = new ArrayList<Forecast>();

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals(TAG_FORECAST)) {
                forecasts.add(readForecast(parser));
            }
        }

        return forecasts;
    }

    private Forecast readForecast(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_FORECAST);

        String timeFrom = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TIME_FROM);
        String timeTo = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TIME_TO);

        double temperature = Double.MIN_VALUE;
        double windspeed = Double.MIN_VALUE;
        double precipitation = Double.MIN_VALUE;
        int iconNumber = -1;

        int depth = 1;

        while (depth > 0) {
            int event = parser.next();

            if (event != XmlPullParser.START_TAG) {
                if (event == XmlPullParser.END_TAG) {
                    depth--;
                }
                continue;
            }
            else {
                depth++;
            }

            String name = parser.getName();

            if (name.equals(TAG_TEMPERATURE)) {
                temperature = readTemperature(parser);
            }
            else if (name.equals(TAG_WINDSPEED)) {
                windspeed = readWindSpeed(parser);
            }
            else if (name.equals(TAG_PRECIPITATION)) {
                precipitation = readPrecipitation(parser);
            }
            else if (name.equals(TAG_SYMBOL)) {
                iconNumber = readIconNumber(parser);
            }
        }

        return new Forecast(timeFrom, timeTo, temperature, windspeed, precipitation, iconNumber);
    }

    private double readTemperature(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_TEMPERATURE);

        String temperatureStr = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_TEMPERATURE);

        double temperature = Double.parseDouble(temperatureStr);

        return temperature;
    }

    private double readWindSpeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_WINDSPEED);

        String windspeedStr = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_WINDSPEED);

        double windspeed = Double.parseDouble(windspeedStr);

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

    private double readPrecipitation(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_PRECIPITATION);

        String precipitationStr = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_PRECIPITATION_VALUE);

        double precipitation = Double.parseDouble(precipitationStr);

        return precipitation;
    }

    private int readIconNumber(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_SYMBOL);

        String iconNumberStr = parser.getAttributeValue(NAMESPACE, ATTRIBUTE_SYMBOL_NUMBER);
        int iconNumber = Integer.parseInt(iconNumberStr);

        return iconNumber;
    }
}
