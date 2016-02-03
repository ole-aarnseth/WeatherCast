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
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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

    // Værvarsler og sted:
    private Forecast[] forecasts;
    private ForecastLocation location;

    // Google API client:
    private GoogleApiClient googleApiClient;

    private ProgressDialog progressDialog;
    private WeatherAPIHandlerFragment handlerFragment;
    private ImageView forecastIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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

        // Sett opp værikon:
        forecastIcon = (ImageView) findViewById(R.id.weatherIcon);

        // Konstruer GoogleApiClient:
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Hvis dette er første gangen aktiviteten kjører må FetchTasken startes:
        if (savedInstanceState == null) {
            if (location == null) {

            }
            //handlerFragment.startFetchForecastTask(new ForecastLocation(60.10, 9.58));
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
        if (handlerFragment.getFetchTaskStatus() == AsyncTask.Status.RUNNING) {
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

        /*
        outState.putSerializable("session", sess);
        outState.putInt("time_left", timeLeft);
        */
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

    public void addForecast(Forecast[] forecasts) {
        this.forecasts = forecasts;
        TextView out = (TextView) findViewById(R.id.textView);
        out.setText(forecasts[0].toString());
        FileHandler fh = new FileHandler();
        forecastIcon.setImageBitmap(forecasts[0].getWeatherIconBitmap());
        progressDialog.dismiss();
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

    /***************************************
     **** Overrides for GoogleApiClient ****
     ***************************************/
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(ACTIVITY_TAG, "Error, insufficient permissions!");
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (location != null) {
            Log.i(ACTIVITY_TAG, "Fetching location...");
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                Log.i(ACTIVITY_TAG, "Success, got location.");
                location = new ForecastLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
                TextView out = (TextView) findViewById(R.id.textView);
                out.setText(location.toString());
            }
            else {
                // Error Dialog
            }
        }
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
