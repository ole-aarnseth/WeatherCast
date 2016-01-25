package com.oleaarnseth.weathercast;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherAPIHandlerFragment extends Fragment {
    public static final String WEATHER_URL = "http://api.yr.no/weatherapi/locationforecast/1.9/?lat=60.10;lon=9.58";
    public static final int HTTP_OK = 200, HTTP_DEPRECATED = 203;
    public static final int READ_TIMEOUT = 10000, CONNECT_TIMEOUT = 15000;

    private FetchForecastTask fetchForeCastTask = new FetchForecastTask();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static WeatherAPIHandlerFragment newInstance() {
        WeatherAPIHandlerFragment fragment = new WeatherAPIHandlerFragment();
        fragment.setRetainInstance(true);

        return fragment;
    }

    public void startFetchForecastTask() {
        if (fetchForeCastTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        if (fetchForeCastTask.getStatus() == AsyncTask.Status.FINISHED) {
            fetchForeCastTask = new FetchForecastTask();
        }

        fetchForeCastTask.execute();
    }

    public AsyncTask.Status getFetchTaskStatus() {
        return fetchForeCastTask.getStatus();
    }

    private class FetchForecastTask extends AsyncTask<Void, Void, Forecast> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Forecast doInBackground(Void... params) {
            URL url;
            HttpURLConnection connection = null;
            Forecast result = null;

            try {
                url = new URL(WEATHER_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                int responseCode = connection.getResponseCode();

                if (responseCode == HTTP_OK || responseCode == HTTP_DEPRECATED) {
                    XMLParser parser = new XMLParser();
                    result = parser.parse(connection.getInputStream());
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

            return result;
        }

        @Override
        protected void onPostExecute(Forecast result) {
            if (result == null) {
                // Error result
            }
            else {
                WeatherActivity activity = (WeatherActivity) getActivity();
                activity.addForecast(result);
            }
        }
    }

    private String readStreamToString(InputStream in) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();

        try {
            reader = new BufferedReader(new InputStreamReader(in));

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                builder.append(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return builder.toString();
    }
}
