package com.oleaarnseth.weathercast;

import android.Manifest;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

/*************************************************************************************************
 Dette er hovedaktiviteten for appen, som henter lokasjon gjennom GoogleAPIClient og deretter
 laster ned værdata fra WeatherAPI gjennom WeatherAPIHandlerFragment. Så vises værvarsler fra
 forecasts-arrayen i en enkel GUI.
 *************************************************************************************************/

public class WeatherActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    // String-tag som identifiserer handlerfragmentet i fragmentmanager:
    public static final String HANDLER_FRAGMENT_TAG = "weatherapi_handler_fragment";

    public static final String ACTIVITY_TAG = "WeatherActivity";

    public static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    // Bundle-tagger:
    public static final String BUNDLE_TAG_FORECASTS = "forecasts",
            BUNDLE_TAG_LOCATION = "location",
            BUNDLE_TAG_LOCALITY = "locality";

    // Værvarsler og sted:
    private Forecast[] forecasts;
    private Location location;
    private String locality;

    // Liste med ForecastFragmenter:
    private ArrayList<ForecastFragment> forecastFragments;

    // Google API client:
    private GoogleApiClient googleApiClient;

    private ProgressDialog progressDialog;
    private WeatherAPIHandlerFragment handlerFragment;
    private TextView localityDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Sett opp hodeløst fragment med AsyncTask som henter XML-data fra WeatherAPI:
        FragmentManager fm = getFragmentManager();
        handlerFragment = (WeatherAPIHandlerFragment) fm.findFragmentByTag(HANDLER_FRAGMENT_TAG);

        if (handlerFragment == null) {
            handlerFragment = WeatherAPIHandlerFragment.newInstance();
            fm.beginTransaction().add(handlerFragment, HANDLER_FRAGMENT_TAG).commit();
        }

        // ProgressDialog som vises mens AsyncTask-en kjører:
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progressdialog_hdr));
        progressDialog.setMessage(getString(R.string.progressdialog_text));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        // Konstruer GoogleApiClient:
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Sett opp ForecastFragment-liste:
        forecastFragments = new ArrayList<ForecastFragment>();

        // Sett opp TextView som viser sted:
        localityDisplay = (TextView) findViewById(R.id.locality);

        // Hvis aktiviteten er rekonstruert hentes verdier fra Bundle:
        if (savedInstanceState != null) {
            location = savedInstanceState.getParcelable(BUNDLE_TAG_LOCATION);
            forecasts = (Forecast[]) savedInstanceState.getSerializable(BUNDLE_TAG_FORECASTS);
            locality = savedInstanceState.getString(BUNDLE_TAG_LOCALITY);

            // Finn fragmenter:
            if (forecasts != null) {
                for (Forecast forecast : forecasts) {
                    forecastFragments.add((ForecastFragment) fm.findFragmentByTag(forecast.getDisplayDate()));
                }
            }

            if (locality != null) {
                localityDisplay.setText(locality);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(ACTIVITY_TAG, "Connecting GoogleAPI...");
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Hvis AsyncTask kjører må progressdialog vises:
        if (handlerFragment.getFetchForecastTaskStatus() == AsyncTask.Status.RUNNING
                || handlerFragment.getFetchLocalityStatus() == AsyncTask.Status.RUNNING) {
            progressDialog.show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(BUNDLE_TAG_FORECASTS, forecasts);
        outState.putParcelable(BUNDLE_TAG_LOCATION, location);
        outState.putString(BUNDLE_TAG_LOCALITY, locality);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressDialog = null;
        handlerFragment = null;
        forecasts = null;
        location = null;
        googleApiClient = null;
    }

    // Callback fra WeatherAPIHandlerFragment setter værvarsler:
    public void setForecasts(Forecast[] forecasts) {
        this.forecasts = forecasts;

        // Avslutt progressdialog hvis andre AsyncTask ikke kjører:
        if (handlerFragment.getFetchLocalityStatus() != AsyncTask.Status.RUNNING) {
            progressDialog.dismiss();
        }

        drawForecastFragments();
    }

    // Metode som lager ForecastFragmenter og legger dem i ScrollView:
    private void drawForecastFragments() {
        if (forecasts == null) {
            return;
        }

        if (forecastFragments.size() > 0) {
            destroyForecastFragments();
        }

        FragmentManager fm = getFragmentManager();

        for (Forecast forecast : forecasts) {
            ForecastFragment frag = ForecastFragment.newInstance(forecast);
            // DisplayDate er unik for hver forecast og brukes derfor som Fragment Tag:
            fm.beginTransaction().add(R.id.forecastContainer, frag, forecast.getDisplayDate()).commit();
            forecastFragments.add(frag);
        }
    }

    // Metode som fjerner ForecastFragmenter:
    private void destroyForecastFragments() {
        FragmentManager fm = getFragmentManager();

        for (int i = 0; i < forecastFragments.size(); i++) {
            fm.beginTransaction().remove(forecastFragments.get(i)).commit();
        }
    }

    // Callback fra WeatherAPIHandlerFragment setter locality (by):
    public void setLocality(String locality) {
        // AsyncTasken returnerer "" hvis den ikke fant by:
        if (!locality.equals("")) {
            this.locality = locality;
        }
        else {
            this.locality = getResources().getString(R.string.unknown_locality);
        }

        localityDisplay.setText(locality);

        // Avslutt progressdialog hvis andre AsyncTask ikke kjører:
        if (handlerFragment.getFetchForecastTaskStatus() != AsyncTask.Status.RUNNING) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Henter lokasjonsdata, skal kun kalles når GoogleApiClient er oppkoblet:
    private void fetchLocation() {
        if (googleApiClient == null || handlerFragment == null || !googleApiClient.isConnected()) {
            return;
        }

        // Sjekk om Android-enheten har permission for henting av lokasjonsdata:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(ACTIVITY_TAG, "Request permission ACCESS_COARSE_LOCATION.");
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);

            return;
        }

        if (forecasts == null) {
            if (location == null) {
                Log.i(ACTIVITY_TAG, "Fetching location...");
                Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                if (lastLocation != null) {
                    location = lastLocation;
                }
                else {
                    // Display error dialog
                    return;
                }
            }

            // Starter AsyncTask som henter værvarsel til gjeldende lokasjon:
            handlerFragment.setLocation(location);
            progressDialog.show();
            handlerFragment.startFetchForecastTask();
        }
    }

    // Callback når brukeren har angitt om appen får nødvendige permissions:
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        /* Denne metoden og fetchLocation går i evig løkke inntil brukeren
           gir nødvendige permissions: */
        fetchLocation();
    }

    /***************************************
     **** Overrides for GoogleApiClient ****
     ***************************************/
    @Override
    public void onConnected(Bundle bundle) {
        // Hent lokasjon når den er oppkoblet:
        fetchLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(ACTIVITY_TAG, "Connection suspended.");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(ACTIVITY_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }
}
