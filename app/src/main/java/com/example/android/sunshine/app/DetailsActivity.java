package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class DetailsActivity extends AppCompatActivity {
    // Constants
    private static final String LOG_TAG = DetailsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);



        Intent intent = getIntent();
        Uri dateUri = null;
        if (intent != null || intent.getData() != null) {
            dateUri = intent.getData();
        }

        Bundle args = new Bundle();
        args.putParcelable(DetailsFragment.DETAIL_URI, dateUri);

        DetailsFragment detailsFragment = new DetailsFragment();
        detailsFragment.setArguments(args);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.weather_detail_container, detailsFragment)
                    .commit();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.detailfragment, menu);
//        // Get share menu item then attach the ShareActionProvider to set the intent
//        MenuItem menuItem = menu.findItem(R.id.action_share);
//        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
//
//        // Attach an intent to the ShareActionProvider
//        if (shareActionProvider != null) {
//            shareActionProvider.setShareIntent(createShareIntent());
//        } else {
//            Log.d(LOG_TAG, "Share Action Provider is null?");
//        }
//
//        return true;
//    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
