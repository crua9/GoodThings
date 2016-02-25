package com.techreviewsandhelp.goodthings;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class GoodThingsTableHelper {
    private static SQLiteDatabase database;
    private static DbHelper dbHelper;
    private static final String LOG_TAG = GoodThingsTableHelper.class.getSimpleName();

    /**
     * Returns opened datebase
     * @param context Context
     * @return SQLiteDatebase
     */
    public static SQLiteDatabase getDbInstance(Context context)    {
        if (database == null) {
            dbHelper = new DbHelper(context);
            database = dbHelper.getWritableDatabase();
            Log.d(LOG_TAG,"getting new DB "+database.hashCode());
            return database;
        }
        Log.d(LOG_TAG,"returning DB "+database.hashCode());
        return database;
    }

    public static boolean isDbOpen(){
        if (database != null) return database.isOpen();
        return false;
    }

    public static Cursor selectWhere(String whereHead, String whereValue){
        if (database != null) {
            return database.rawQuery("SELECT * FROM " + DbHelper.TABLE_NAME + " WHERE " + whereHead + "='" + whereValue + "'", null);
        }
        return null;
    }

    public static int[] getCount(String date, String header){
        //Cursor countCur = database.rawQuery("SELECT COUNT " + header + " FROM " + DbHelper.TABLE_NAME + " WHERE " + DbHelper.DATE + "='" + date +"'",null);
        Cursor countCur = database.rawQuery("SELECT * FROM " + DbHelper.TABLE_NAME + " WHERE " + DbHelper.DATE + "='" + date +"' AND " + header + " IS NOT NULL",null);
        Log.d(LOG_TAG, "count: " + "SELECT * FROM " + DbHelper.TABLE_NAME + " WHERE " + DbHelper.DATE + "='" + date + "'");

        countCur.moveToFirst();
        int[] count = new int[3];
        for (int i = 0; i < countCur.getCount(); i++) {
            if (countCur.getString(2).length()!=0) count[0]++;
            if (countCur.getString(3).length()!=0) count[1]++;
            if (countCur.getString(4).length()!=0) count[2]++;
            countCur.moveToNext();
        }
        Log.d(LOG_TAG,"count" + Integer.toString(count[0]));
        countCur.close();
        return count;
    }

    public static void deleteById(String id){
        database.execSQL("DELETE FROM " + DbHelper.TABLE_NAME + " WHERE _id='" + id + "'");
    }

    public static void closeDB(){
        if (database != null && database.isOpen()){
            Log.d(LOG_TAG,"closing DB "+database.hashCode());
            database.close();
        }
        database = null;
    }

    /*public static void refreshAchievements(Context mContext, AchievFragment achievFragment){
        (new AsyncAchievements(mContext,achievFragment)).execute();
    }*/

    public static void refreshAchievements(Context mContext, MaterialCalendarFragment materialCalendarFragment){
        (new AsyncAchievements(mContext,materialCalendarFragment)).execute();
    }

    public static void refreshCalendarMarkers(Context mContext, MaterialCalendarView materialCalendarView){
        (new AsyncCalendarColoring(mContext, materialCalendarView)).execute();
    }

    private static class AsyncAchievements extends AsyncTask<Void,Void,Void> {
        Context mContext;
        TextView gText, dText, bText;
        int gCount, dCount, bCount;

        /*public AsyncAchievements(Context mContext, AchievFragment achievFragment){
            if (achievFragment != null) {
                ArrayList<TextView> textViews = achievFragment.getGoodCoins();
                try {
                    this.gText = textViews.get(0);
                    this.dText = textViews.get(1);
                    this.bText = textViews.get(2);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            this.mContext = mContext;
        }*/

        public AsyncAchievements(Context mContext, MaterialCalendarFragment materialCalendarFragment){
            if (materialCalendarFragment != null) {
                ArrayList<TextView> textViews = materialCalendarFragment.getGoodCoins();
                try {
                    this.gText = textViews.get(0);
                    this.dText = textViews.get(1);
                    this.bText = textViews.get(2);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            this.mContext = mContext;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SQLiteDatabase database = getDbInstance(mContext);
            gCount = 0;
            dCount = 0;
            bCount = 0;
            Cursor cursor = null;
            try {

                cursor = database.rawQuery("SELECT * FROM " + DbHelper.TABLE_NAME, null);
                cursor.moveToFirst();

                for (int i = 0; i < cursor.getCount(); i++) {
                    if (cursor.getString(2).length()!=0) gCount++;
                    if (cursor.getString(3).length()!=0) dCount++;
                    if (cursor.getString(4).length()!=0) bCount++;
                    cursor.moveToNext();
                }

                cursor.close();
            } catch (Exception e) {
                if (cursor != null) cursor.close();
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                gText.setText(Integer.toString(gCount));
                bText.setText(Integer.toString(bCount));
                dText.setText(Integer.toString(dCount));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static class AsyncCalendarColoring extends AsyncTask<Void, Void, HashSet<CalendarDay>> {
        private Context mContext;
        private MaterialCalendarView materialCalendarView;
        private CalendarDay selectedDate;
        private final String LOG_TAG = this.getClass().getSimpleName();

        public AsyncCalendarColoring(Context context, MaterialCalendarView materialCalendarView) {
            mContext = context;
            this.materialCalendarView = materialCalendarView;
            if (materialCalendarView != null) selectedDate = materialCalendarView.getCurrentDate();
        }

        @Override
        protected HashSet<CalendarDay> doInBackground(Void... params) {

            SQLiteDatabase database = getDbInstance(mContext);
            String[] headers = new String[]{DbHelper.DATE};
            Log.v(LOG_TAG, "start");
            Cursor cursor;
            Date date;
            HashSet<CalendarDay> hashSet = new HashSet<>();
            try {
                int selectedMonth = selectedDate.getMonth() + 1;
                String selectedMonthString;

                selectedMonthString = Integer.toString(selectedMonth);

                if (selectedMonth < 10) {
                    StringBuilder stringBuilder = new StringBuilder(selectedMonthString);
                    stringBuilder.insert(0, '0');
                    selectedMonthString = stringBuilder.toString();
                }


                String where = DbHelper.DATE + " LIKE '" + selectedMonthString + "/__/____'";
                cursor = database.query(DbHelper.TABLE_NAME, headers, where, null, null, null, null);
                Log.i(LOG_TAG, "SELECT * FROM " + DbHelper.TABLE_NAME + " WHERE " + where);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    String dateString = "";
                    do {
                        if (!dateString.equals(cursor.getString(0))) {
                            dateString = cursor.getString(0);
                            date = new Date(Date.parse(cursor.getString(0)));
                            CalendarDay calendarDay = new CalendarDay(date);
                            hashSet.add(calendarDay);
                        }
                        cursor.moveToNext();
                    } while (!cursor.isAfterLast());
                    cursor.close();
                } else {
                    cursor.close();
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return hashSet;
        }

        @Override
        protected void onPostExecute(final HashSet<CalendarDay> daysToMark) {
            super.onPostExecute(daysToMark);

            try {
                if (daysToMark != null) {
                    DayViewDecorator dayViewDecorator = new DayViewDecorator() {
                        @Override
                        public boolean shouldDecorate(CalendarDay day) {
                            return daysToMark.contains(day);
                        }

                        @Override
                        public void decorate(DayViewFacade view) {
                            view.addSpan(new DotSpan(5, R.color.colorPrimary));
                        }
                    };

                    MaterialCalendarView calendarView;
                    if (materialCalendarView != null) {
                        materialCalendarView.removeDecorators();
                        materialCalendarView.addDecorator(dayViewDecorator);
                    }
                }
                Log.v(LOG_TAG, "stop");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
