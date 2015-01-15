package com.ggstudios.lolcraft;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ggstudios.dialogs.AboutDialogFragment;
import com.ggstudios.dialogs.ChangelogDialogFragment;
import com.ggstudios.utils.Utils;

import timber.log.Timber;

public class MainActivity extends ActionBarActivity {

    private static final String PREF_LAST_VERSION = "lastVer";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = StateManager.getInstance().getPreferences();

        if (savedInstanceState == null) {
            try {
                int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

                if (prefs.getBoolean(getString(R.string.key_show_changelog), true) &&
                        prefs.getInt(PREF_LAST_VERSION, 0) < versionCode) {
                    // patch notes dialog...
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(PREF_LAST_VERSION, versionCode);
                    Utils.applyPreferences(editor);
                    ChangelogDialogFragment.newInstance().show(getSupportFragmentManager(), "dialog");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Timber.e("Unable to fetch version code. This should never happen!", e);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_report_bug:
        	    Utils.startBugReportIntent(this);
                return true;
            case R.id.action_about:
                AboutDialogFragment.newInstance().show(getSupportFragmentManager(), "dialog");
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
