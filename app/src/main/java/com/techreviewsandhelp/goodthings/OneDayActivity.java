package com.techreviewsandhelp.goodthings;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OneDayActivity extends AppCompatActivity {
    private Cursor userCursor;
    protected String date;
    private int goodCount;
    private int dealsCount;
    private int betterCount;
    private TextView gCount;
    private TextView dCount;
    private TextView bCount;
    private final String LOG_TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.activity_list);
        final Intent intent = getIntent();
        date = intent.getStringExtra("date");
        Log.i(LOG_TAG, date);
        String[] dateSplit = date.split("/");
        int year = Integer.parseInt(dateSplit[2]) - 1900;
        int month = Integer.parseInt(dateSplit[0]) - 1;
        int day = Integer.parseInt(dateSplit[1]);

        Date dateDate = new Date(year,month,day);
        DateFormat dateFormat = DateFormat.getDateInstance();
        String dateLabel = dateFormat.format(dateDate);

        TextView dateOnListView = (TextView) findViewById(R.id.date_on_list_view);
        dateOnListView.setText(dateLabel);

        List<NoteModel> notes = new ArrayList<>();

        populateNotes(notes);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(notes,this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();

        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(itemAnimator);

        gCount = (TextView)findViewById(R.id.good_count);
        dCount = (TextView)findViewById(R.id.deeds_count);
        bCount = (TextView)findViewById(R.id.better_count);
        refreshCoinsAmount();
    }

    public void refreshCoinsAmount(){
        Log.d(LOG_TAG,"refreshing coins");
        int[] count = GoodThingsTableHelper.getCount(date,DbHelper.Deeds);

        gCount.setText(Integer.toString(count[0]));
        dCount.setText(Integer.toString(count[1]));
        bCount.setText(Integer.toString(count[2]));
    }

    private void populateNotes(List<NoteModel> notes){

        userCursor =  GoodThingsTableHelper.selectWhere(DbHelper.DATE, date);
        try {
                userCursor.moveToFirst();
                while (!userCursor.isAfterLast()) {
                    Log.i(LOG_TAG +".Cursor", "Count " + Integer.toString(userCursor.getCount()));
                    Log.i(LOG_TAG +".Cursor", "Position " + Integer.toString(userCursor.getPosition()));
                    NoteModel note = new NoteModel();
                    note.setGoodDeals(userCursor.getString(3));
                    note.setId(userCursor.getString(0));
                    note.setBetter(userCursor.getString(4));
                    note.setGoodThings(userCursor.getString(2));
                    notes.add(note);
                    userCursor.moveToNext();
                } ;

        }
        catch (NullPointerException e){
            Log.i(LOG_TAG +".Cursor","Nothing to show");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        userCursor.close();
        super.onDestroy();
    }
}
