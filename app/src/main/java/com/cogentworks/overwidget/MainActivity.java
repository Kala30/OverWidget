package com.cogentworks.overwidget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.woxthebox.draglistview.DragListView;
import com.woxthebox.draglistview.swipe.ListSwipeHelper;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    public SQLHelper dbHelper;
    ProfileAdapter adapter;
    ListView listView;

    public DragListView mDragListView;
    ArrayList<Profile> mItemArray;

    boolean isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean useDarkTheme = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getBoolean(SettingsActivity.PREF_DARK_THEME, false);
        if (useDarkTheme)
            setTheme(R.style.Blackwatch);

        setContentView(R.layout.activity_main);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (useDarkTheme) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimaryBlackwatch));
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorTextBlackwatch));
            toolbar.setPopupTheme(R.style.Blackwatch);

        }*/

        // Set swipe to refresh behavior
        final MainActivity activityContext = this;
        OWSwipeRefreshLayout mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        isBusy = true;
                        UpdateListTask updateListTask = new UpdateListTask(activityContext, dbHelper.getList());
                        updateListTask.execute();
                    }
                }
        );

        dbHelper = new SQLHelper(this);
        mItemArray = dbHelper.getList();

        final OWSwipeRefreshLayout mRefreshLayout = mSwipeRefreshLayout;

        mDragListView = (DragListView) findViewById(R.id.list);
        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
        mDragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
                mRefreshLayout.setEnabled(false);
                //Toast.makeText(mDragListView.getContext(), "Start - position: " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (!isBusy) {
                    mRefreshLayout.setEnabled(true);
                    if (fromPosition != toPosition) {
                        dbHelper.setList(mItemArray);
                        //Toast.makeText(mDragListView.getContext(), "End - position: " + toPosition, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mRefreshLayout.setScrollingView(mDragListView.getRecyclerView());
        if (useDarkTheme)
            mRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccentDark));
        else
            mRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));

        mDragListView.setSwipeListener(new ListSwipeHelper.OnSwipeListenerAdapter() {
            @Override
            public void onItemSwipeStarted(ListSwipeItem item) {
                mRefreshLayout.setEnabled(false);
            }

            @Override
            public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {
                mRefreshLayout.setEnabled(true);
                // Swipe to delete on left
                if (swipedDirection == ListSwipeItem.SwipeDirection.LEFT || swipedDirection == ListSwipeItem.SwipeDirection.RIGHT) {
                    if (!isBusy) {
                        final Profile adapterItem = (Profile) item.getTag();
                        final int pos = mDragListView.getAdapter().getPositionForItem(adapterItem);

                        isBusy = true;
                        dbHelper.deleteItem(adapterItem.BattleTag);
                        mDragListView.getAdapter().removeItem(pos);
                        isBusy = false;
                    } else {
                        Toast.makeText(MainActivity.this, "List is busy", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        setupListRecyclerView();

        isBusy = true;
        mSwipeRefreshLayout.setRefreshing(true);
        UpdateListTask updateListTask = new UpdateListTask(this, mItemArray);
        updateListTask.execute();

    }

    private void setupListRecyclerView() {
        mDragListView.setLayoutManager(new LinearLayoutManager(this));
        ItemAdapter listAdapter = new ItemAdapter(mItemArray, R.layout.list_item, R.id.layout_main, true);
        mDragListView.setAdapter(listAdapter, true);
        mDragListView.setCanDragHorizontally(false);
        //mDragListView.setCustomDragItem(new MyDragItem(getContext(), R.layout.list_item));
    }

    /*public void showItemList() {
        isBusy = true;

        if (adapter == null) {
            ((SwipeRefreshLayout) findViewById(R.id.swiperefresh)).setRefreshing(true);
            adapter = new ProfileAdapter(this, dbHelper.getList());
            listView.setAdapter(adapter);

            UpdateListTask updateListTask = new UpdateListTask(this, dbHelper.getList());
            updateListTask.execute();

        } else {
            adapter.clear();
            adapter.addAll(dbHelper.getList());
            adapter.notifyDataSetChanged();
        }

        isBusy = false;
    }*/

    public void onFabClick(View view) {
        final Context context = this;
        final View dialogView = this.getLayoutInflater().inflate(R.layout.configure_dialog, null);

        if (!isBusy) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Add a new player")
                    .setView(dialogView)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText editText = dialogView.findViewById(R.id.config_battletag);
                            String battleTag = String.valueOf(editText.getText());
                            Spinner platformSpinner = dialogView.findViewById(R.id.platform_spinner);
                            String platform = platformSpinner.getSelectedItem().toString();
                            Spinner regionSpinner = dialogView.findViewById(R.id.region_spinner);
                            String region = regionSpinner.getSelectedItem().toString();

                            if (!dbHelper.getList("BattleTag").contains(battleTag)) {
                                Toast.makeText(findViewById(R.id.swiperefresh).getContext(), "Adding player...", Toast.LENGTH_SHORT).show();
                                AddProfileTask addTask = new AddProfileTask(context, battleTag, platform, region);
                                addTask.execute();
                            } else {
                                Snackbar.make(findViewById(R.id.layout_main), "Player already added to list", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();

            Spinner regionSpinner = dialogView.findViewById(R.id.region_spinner);
            ArrayAdapter<CharSequence> regionAdapter = ArrayAdapter.createFromResource(this, R.array.region_array, android.R.layout.simple_spinner_item);
            regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            regionSpinner.setAdapter(regionAdapter);

            Spinner platformSpinner = dialogView.findViewById(R.id.platform_spinner);
            ArrayAdapter<CharSequence> platformAdapter = ArrayAdapter.createFromResource(this, R.array.platform_array, android.R.layout.simple_spinner_item);
            platformAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            platformSpinner.setAdapter(platformAdapter);
        } else {
            Toast.makeText(this, "List is busy", Toast.LENGTH_LONG).show();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public static final int REQUEST_EXIT = 2;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkable = menu.findItem(R.id.dark_check);
        boolean isDark = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getBoolean(SettingsActivity.PREF_DARK_THEME, false);
        checkable.setChecked(isDark);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.dark_check:
                boolean isChecked = !item.isChecked();
                item.setChecked(isChecked);

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(SettingsActivity.PREF_DARK_THEME, isChecked);
                editor.commit();

                Intent intent = getIntent();
                finish();
                startActivity(intent);

                return true;
            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_EXIT);
                return true;
            default:
                return false;
        }
    }

    /*private void sortItemsDialog() {
        if (!isBusy) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Sort by")
                    .setSingleChoiceItems(R.)
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText editText = dialogView.findViewById(R.id.config_battletag);
                            String battleTag = String.valueOf(editText.getText());
                            Spinner platformSpinner = dialogView.findViewById(R.id.platform_spinner);
                            String platform = platformSpinner.getSelectedItem().toString();
                            Spinner regionSpinner = dialogView.findViewById(R.id.region_spinner);
                            String region = regionSpinner.getSelectedItem().toString();

                            if (!dbHelper.getList("BattleTag").contains(battleTag)) {
                                Toast.makeText(findViewById(R.id.swiperefresh).getContext(), "Adding player...", Toast.LENGTH_SHORT).show();
                                AddProfileTask addTask = new AddProfileTask(context, battleTag, platform, region);
                                addTask.execute();
                            } else {
                                Snackbar.make(findViewById(R.id.layout_main), "Player already added to list", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, MainActivity.class);
                this.finish();
                startActivity(intent);
            }
        }
    }

}
