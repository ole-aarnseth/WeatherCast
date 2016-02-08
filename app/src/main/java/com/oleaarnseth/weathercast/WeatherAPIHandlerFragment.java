package com.oleaarnseth.weathercast;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/*
    WeatherAPIHandlerFragment er et hodeløst fragment som står for henting og behandling
    av all data fra yr sitt WeatherAPI.
 */
public class WeatherAPIHandlerFragment extends Fragment {
    public static final String TAG_FRAGMENT = "WeatherAPIHandler";

    public static final String WEATHER_URL = "http://api.yr.no/weatherapi/locationforecast/";
    public static final String WEATHER_VERSION = "1.9";
    public static final String WEATHER_ATTRIBUTE_LAT = "/?lat=";
    public static final String WEATHER_ATTRIBUTE_LON = ";lon=";

    public static final String WEATHER_ICON_URL = "http://api.yr.no/weatherapi/weathericon/";
    public static final String WEATHER_ICON_VERSION = "1.1";
    public static final String WEATHER_ICON_ATTRIBUTE_ICON_NUMBER = "/?symbol=";
    public static final String WEATHER_ICON_ATTRIBUTE_CONTENT_TYPE = ";content_type=image/png";

    public static final int HTTP_OK = 200, HTTP_DEPRECATED = 203;
    public static final int READ_TIMEOUT = 10000, CONNECT_TIMEOUT = 15000;

    /* Angir for hvor mange dager varsel skal gis for (weatherAPI gir
    varsler for maks 9 dager fram i tid): */
    public static final int NUM_DAYS = 8;

    // Formateringsstreng for SimpleDateFormat tilpasset datoformatet i XML-dataene:
    public static final String XML_DATE_FORMAT = "yyyy-MM-dd";

    // Siste halvdel av en dato-oppføring der klokken er 12:00:
    public static final String XML_DATE_STRING_END = "T12:00:00Z";

    private FetchForecastTask fetchForeCastTask = new FetchForecastTask();
    private FetchLocalityTask fetchLocalityTask = new FetchLocalityTask();

    // Lokasjon som værvarsel skal hentes for:
    private Location location;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setLocation(Location location) { this.location = location; }

    public static WeatherAPIHandlerFragment newInstance() {
        WeatherAPIHandlerFragment fragment = new WeatherAPIHandlerFragment();
        fragment.setRetainInstance(true);

        return fragment;
    }

    // Starter AsyncTask i hodeløst fragment, kalles fra WeatherActivity:
    public void startFetchForecastTask() {
        if (location == null || fetchForeCastTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        if (fetchForeCastTask.getStatus() == AsyncTask.Status.FINISHED) {
            fetchForeCastTask = new FetchForecastTask();
        }

        fetchForeCastTask.execute(location);
    }

    public AsyncTask.Status getFetchForecastTaskStatus() {
        return fetchForeCastTask.getStatus();
    }

    public AsyncTask.Status getFetchLocalityStatus() { return fetchLocalityTask.getStatus(); }

    private class FetchForecastTask extends AsyncTask<Location, Void, Forecast[]> {
        @Override
        protected Forecast[] doInBackground(Location... params) {
            URL url;
            HttpURLConnection connection = null;
            LinkedList<Forecast> rawData = null;

            try {
                url = new URL(WEATHER_URL
                        + WEATHER_VERSION
                        + WEATHER_ATTRIBUTE_LAT
                        + params[0].getLatitude()
                        + WEATHER_ATTRIBUTE_LON
                        + params[0].getLongitude());
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                int responseCode = connection.getResponseCode();

                if (responseCode == HTTP_OK || responseCode == HTTP_DEPRECATED) {
                    XMLParser parser = new XMLParser();
                    rawData = parser.parse(connection.getInputStream());
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (rawData != null) {
                Forecast[] result = organizeData(rawData);
                return result;
            } else {
                // Bad connection:
                return null;
            }
        }

        @Override
        protected void onPostExecute(Forecast[] result) {
            // Hvis result == null har det skjedd en feil, og feildialog vises i WeatherActivity:
            WeatherActivity activity = (WeatherActivity) getActivity();
            activity.setForecasts(result);
            startFetchLocalityTask();
        }
    }

    private void startFetchLocalityTask() {
        if (location == null || fetchLocalityTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        if (fetchLocalityTask.getStatus() == AsyncTask.Status.FINISHED) {
            fetchLocalityTask = new FetchLocalityTask();
        }

        fetchLocalityTask.execute(location);
    }

    private class FetchLocalityTask extends AsyncTask<Location, Void, String> {
        @Override
        protected String doInBackground(Location... params) {
            Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
            List<Address> addresses = null;

            // Utfør reverse geocoding og lever resultat:
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            }
            catch (IOException e) {
                Log.wtf(TAG_FRAGMENT, "IOExcpetion when reverse geocoding.");
            }
            catch (IllegalArgumentException e) {
                Log.wtf(TAG_FRAGMENT, "IllegalArgumentException when reverse geocoding.");
            }

            // Sjekk at adresse ble funnet:
            if (addresses == null || addresses.size() == 0) {
                return "";
            }

            Address address = addresses.get(0);
            return address.getLocality();
        }

        @Override
        protected void onPostExecute(String result) {
            WeatherActivity activity = (WeatherActivity) getActivity();
            activity.setLocality(result);
        }
    }


    /* Denne metoden organiserer rådataene fra XML-parseren slik at første XML-oppføring
    blir "dagens" værvarsel, mens værvarsel for de neste dagene blir representert av
    varselet for klokka 12:00 for den dagen. Nedbør og ikon-id hentes fra oppføringen
    som kommer rett etter det gjeldende varselet, siden den oppføringen vil gjelde for
    samme tidsrom. */
    private Forecast[] organizeData(LinkedList<Forecast> rawData) {
        if (rawData.size() < (NUM_DAYS * 2)) {
            throw new IllegalStateException("Not enough data from XML-feed.");
        }

        Forecast[] forecasts = new Forecast[NUM_DAYS];
        Iterator<Forecast> iterator = rawData.iterator();

        // Sett sammen første varsel basert på første og andre oppføring i listen:
        forecasts[0] = iterator.next();
        Forecast extra = iterator.next();

        if (!forecasts[0].getTimeTo().equals(extra.getTimeTo())) {
            throw new IllegalStateException("Inconsistent dates from XML-feed.");
        }

        forecasts[0].setPrecipitation(extra.getPrecipitation());
        forecasts[0].setForecastWeatherIcon(extra.getWeatherIcon());
        forecasts[0].setDisplayDate(getResources().getString(R.string.displaydate_today));
        downloadWeatherIcon(forecasts[0]);

        // Initialiser kalender, SimpleDateFormat og arrayer med månedsnavn og dager før løkke:
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat xmlFormat = new SimpleDateFormat(XML_DATE_FORMAT);
        String[] months = getResources().getStringArray(R.array.month_list);
        String[] days = getResources().getStringArray(R.array.days_of_month_list);

        if (months.length < 12 || days.length < 31) {
            throw new IllegalStateException("Incomplete string array resources.");
        }

        for (int i = 1; i < forecasts.length; i++) {
            cal.add(Calendar.DATE, 1);
            forecasts[i] = assembleForecast(iterator, xmlFormat.format(cal.getTime()) + XML_DATE_STRING_END);

            String displayDate = days[Integer.parseInt(forecasts[i].getTimeTo().substring(8, 10)) - 1]
                    + System.getProperty("line.separator")
                    + months[Integer.parseInt(forecasts[i].getTimeTo().substring(5, 7)) - 1];

            forecasts[i].setDisplayDate(displayDate);
        }

        return forecasts;
    }

    /* Hjelpemetode som setter sammen et værvarsel for dato angitt i
       i Stringen date: */
    private Forecast assembleForecast(Iterator<Forecast> iterator, String dateString) {
        /* Flytt iterator til neste værvarsel for klokka 12:00 den gjeldende datoen,
           og hent ut forecast der timeFrom og timeTo er like: */
        Forecast forecast = null;
        while (iterator.hasNext()) {
            forecast = iterator.next();
            if (forecast.getTimeFrom().equals(dateString) && forecast.getTimeTo().equals(dateString)) {
                break;
            }
        }

        if (forecast == null || !iterator.hasNext()) {
            throw new IllegalStateException("Incomplete XML-data in iterator.");
        }

        Forecast extra = iterator.next();

        if (!forecast.getTimeTo().equals(extra.getTimeTo())) {
            throw new IllegalStateException("Inconsistent dates from XML-feed.");
        }

        forecast.setPrecipitation(extra.getPrecipitation());
        forecast.setForecastWeatherIcon(extra.getWeatherIcon());
        downloadWeatherIcon(forecast);

        return forecast;
    }

    // Hjelpemetode som laster ned værvarselsikon fra yr sitt WeatherAPI:
    private void downloadWeatherIcon(Forecast forecast) {
        if (forecast.getWeatherIcon() == null || forecast.getWeatherIcon().getIconNumber() == -1) {
            throw new IllegalStateException("Weather icon not set.");
        }

        URL url;
        HttpURLConnection connection = null;

        try {
            url = new URL(WEATHER_ICON_URL
                    + WEATHER_ICON_VERSION
                    + WEATHER_ICON_ATTRIBUTE_ICON_NUMBER
                    + forecast.getWeatherIcon().getIconNumber()
                    + WEATHER_ICON_ATTRIBUTE_CONTENT_TYPE);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            int responseCode = connection.getResponseCode();

            if (responseCode == HTTP_OK || responseCode == HTTP_DEPRECATED) {
                InputStream in = connection.getInputStream();
                Bitmap bm = BitmapFactory.decodeStream(in);

                // Setter ikonfilen i objektet til nedlastet png-fil:
                FileHandler fh = new FileHandler();
                WeatherIcon weatherIcon = forecast.getWeatherIcon();
                weatherIcon.setWeatherIconFile(fh.saveToFile(bm, forecast.getWeatherIcon().getIconNumber(), getActivity().getExternalCacheDir()));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
