package com.techreviewsandhelp.goodthings;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MaterialCalendarFragment extends Fragment {
    ArrayList<TextView> goodCoins;
    public MaterialCalendarView materialCalendarView;
    private final String LOG_TAG = this.getClass().getSimpleName();
    static MaterialCalendarFragment calendarFragment;

    public MaterialCalendarFragment() {
        // Required empty public constructor
    }

    public static MaterialCalendarFragment newInstance() {
        if (calendarFragment==null) calendarFragment = new MaterialCalendarFragment();
        return calendarFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG,"onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_material_calendar,container,false);
        materialCalendarView = (MaterialCalendarView)rootview.findViewById(R.id.calendarView);
        materialCalendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
                Intent intent = new Intent(getContext(), OneDayActivity.class);
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                String dateString = dateFormat.format(date.getDate());
                Log.v("datetosql", dateString);
                intent.putExtra("date", dateString);
                startActivityForResult(intent, 101);
                materialCalendarView.clearSelection();
            }
        });
        materialCalendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
                GoodThingsTableHelper.refreshCalendarMarkers(getContext(), materialCalendarView);
            }
        });
        TextView todayText = (TextView)rootview.findViewById(R.id.todayText);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        if (todayText != null) todayText.setText(dateFormat.format(new Date()));    //todayText is null in landscape mode

        //--------------Coins-----------------------
        goodCoins = new ArrayList<>();
        goodCoins.add((TextView)rootview.findViewById(R.id.good_count));
        goodCoins.add((TextView)rootview.findViewById(R.id.deals_count));
        goodCoins.add((TextView) rootview.findViewById(R.id.better_count));
        GoodThingsTableHelper.refreshAchievements(getContext(), this);

        ImageButton shareButton = (ImageButton)rootview.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String link = getResources().getString(R.string.link);
                String stringToSend = String.format(getString(R.string.sharing_string),goodCoins.get(0).getText(),goodCoins.get(1).getText(),goodCoins.get(2).getText(),link);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, stringToSend);
                startActivity(intent);
            }
        });
        //-----------------------------------------------

        return rootview;
    }

    public ArrayList<TextView> getGoodCoins(){
        return goodCoins;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG,"OnResume");
        if (materialCalendarView != null) {
            GoodThingsTableHelper.refreshCalendarMarkers(getContext(), materialCalendarView);
            GoodThingsTableHelper.refreshAchievements(getContext(),this);
        }
        else Toast.makeText(getContext(),"materialCalendarView is null on Resume",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(LOG_TAG, "onSave");
    }


}
