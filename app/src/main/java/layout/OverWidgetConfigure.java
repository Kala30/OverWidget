package layout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.cogentworks.overwidget.CreateWidget;
import com.cogentworks.overwidget.Profile;
import com.cogentworks.overwidget.R;
import com.cogentworks.overwidget.SQLHelper;
import com.cogentworks.overwidget.SettingsActivity;
import com.cogentworks.overwidget.WidgetPrefFragment;
import com.cogentworks.overwidget.WidgetUtils;

import java.util.ArrayList;

/**
 * The configuration screen for the {@link OverWidgetProvider OverWidgetProvider} AppWidget.
 */

public class OverWidgetConfigure extends AppCompatActivity implements OnPreferenceChangeListener {

    private static final String TAG = "OverWidgetConfigure";
    public static final String PREFS_NAME = "layout.OverWidgetProvider";
    public static final String PREF_PREFIX_KEY = "overwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    ProgressBar progressBar;
    LinearLayout mainContent;
    FloatingActionButton fab;
    WidgetPrefFragment prefFragment;

    public OverWidgetConfigure() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sp.edit().remove("username").apply();
        sp.edit().remove("platform").apply();
        sp.edit().remove("region").apply();
        sp.edit().remove("theme").apply();
        sp.edit().remove("interval").apply();

        boolean useDarkTheme = sp.getBoolean(SettingsActivity.PREF_DARK_THEME, false);
        if (useDarkTheme)
            setTheme(R.style.Blackwatch);

        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
        prefFragment = new WidgetPrefFragment();
        mFragmentTransaction.replace(R.id.layout_main, prefFragment);
        mFragmentTransaction.commit();

        setContentView(R.layout.activity_configure);
        mainContent = findViewById(R.id.layout_main);
        progressBar = findViewById(R.id.progress_bar);
        fab = findViewById(R.id.fab);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Invalid appwidget ID
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isDark = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getBoolean(SettingsActivity.PREF_DARK_THEME, false);
        if (isDark) {
            MenuItem settings = menu.findItem(R.id.add_list);
            settings.setIcon(R.drawable.ic_person);
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_configure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.add_list:
                final OverWidgetConfigure activity = this;
                SQLHelper dbHelper = new SQLHelper(this);
                final ArrayList<Profile> profiles = dbHelper.getList();
                CharSequence[] names = new CharSequence[profiles.size()];
                for (int i = 0; i < profiles.size(); i++)
                    names[i] = profiles.get(i).BattleTag;
                final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Add from List")
                        .setItems(names, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("username", profiles.get(which).BattleTag);
                                editor.putString("platform", profiles.get(which).Platform);
                                editor.putString("region", profiles.get(which).Region);
                                prefFragment.findPreference("username").setSummary(profiles.get(which).BattleTag);
                                prefFragment.findPreference("platform").setSummary(profiles.get(which).Platform);
                                prefFragment.findPreference("region").setSummary(profiles.get(which).Region);
                                editor.apply();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();

                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary(newValue.toString());
        return true;
    }

    public void onFabClick(View view) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        fab.hide();
        crossfade(this, progressBar, mainContent);

        final Context context = OverWidgetConfigure.this;

        // When the button is clicked, store the string locally
        String battleTag = sp.getString("username", "None");
        String platform = sp.getString("platform", "PC");
        String region = sp.getString("region", "US");
        String interval = sp.getString("interval", "1");
        String theme = sp.getString("theme", "Dark");
        WidgetUtils.savePrefs(context, mAppWidgetId, battleTag, platform, region, theme, interval);

        // Check if user exists
        CreateWidget createWidget = new CreateWidget(context, mAppWidgetId);
        createWidget.execute(battleTag, platform, region);
    }

    public static void crossfade(Context context, View viewIn, View viewOut) {
        int mShortAnimationDuration = context.getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        viewIn.setAlpha(0f);
        viewIn.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        viewIn.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        final View mViewOut = viewOut;
        viewOut.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mViewOut.setVisibility(View.GONE);
                    }
                });
    }

}