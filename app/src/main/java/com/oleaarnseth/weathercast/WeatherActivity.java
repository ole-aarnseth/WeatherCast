package com.oleaarnseth.weathercast;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;

public class WeatherActivity extends AppCompatActivity {
    // String-tag som identifiserer handlerfragmentet i fragmentmanager:
    public static final String HANDLER_FRAGMENT_TAG = "weatherapi_handler_fragment";

    private ProgressDialog progressDialog;
    private WeatherAPIHandlerFragment handlerFragment;

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
        progressDialog.setTitle(getResources().getString(R.string.progressdialog_hdr));
        progressDialog.setMessage(getResources().getString(R.string.progressdialog_text));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        // Hvis dette er første gangen aktiviteten kjører må FetchTask-en startes:
        if (savedInstanceState == null) {
            handlerFragment.startFetchForecastTask();
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
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        /*
        outState.putSerializable("session", sess);
        outState.putInt("time_left", timeLeft);
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (progressDialog != null) {
            progressDialog = null;
        }

        if (handlerFragment != null) {
            handlerFragment = null;
        }
    }

    public void addForecast(Forecast[] forecasts) {
        TextView out = (TextView) findViewById(R.id.textView);
        out.setText(forecasts[0].toString());
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
}
