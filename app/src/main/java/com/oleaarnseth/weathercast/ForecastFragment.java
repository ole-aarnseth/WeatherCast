package com.oleaarnseth.weathercast;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Dette fragmentet utgjør et listeelement i en ScrollView, og viser et værvarsel:
 */

public class ForecastFragment extends Fragment {
    // Værvarsel som fragmenetet skal vise:
    private Forecast forecast;

    // TextView-elementer:
    private TextView tvDate, tvTemp, tvWind, tvPrecipitation;

    // Værikon:
    private ImageView weatherIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rView = (ViewGroup) inflater.inflate(
                R.layout.fragment_forecast_list_element, container, false
        );

        // Sett opp TextView-elementer:
        tvDate = (TextView) rView.findViewById(R.id.date);
        tvTemp = (TextView) rView.findViewById(R.id.temperature);
        tvWind = (TextView) rView.findViewById(R.id.windspeed);
        tvPrecipitation = (TextView) rView.findViewById(R.id.precipitation);

        // Sett opp værikon:
        weatherIcon = (ImageView) rView.findViewById(R.id.forecastIcon);

        if (forecast != null) {
            printForecastToView();
        }

        return rView;
    }

    public void setForecast(Forecast forecast) { this.forecast = forecast; }

    // Skriver værvarselsdata til Viewene:
    public void printForecastToView() {
        if (forecast == null) {
            return;
        }

        tvDate.setText(forecast.getDisplayDate());
        tvTemp.setText(forecast.getTemperature().toString());
        tvWind.setText(Double.toString(forecast.getWindspeed()));
        tvPrecipitation.setText(forecast.getPrecipitation().toString());
        weatherIcon.setImageBitmap(forecast.getWeatherIcon().getIconBitmap());
    }

    public static ForecastFragment newInstance(Forecast forecast) {
        ForecastFragment frag = new ForecastFragment();
        frag.setForecast(forecast);
        frag.setRetainInstance(true);

        return frag;
    }
}
