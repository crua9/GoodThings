package com.techreviewsandhelp.goodthings;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class AlarmReceiver extends BroadcastReceiver {
    public AlarmReceiver() {
    }

    Context mContext;
    String mPeriod;
    public static final String LOG_TAG = AlarmReceiver.class.getSimpleName();

    public static void setAlarm(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("notification_key",false)) {
            long timeStamp = sharedPreferences.getLong("end_of_day", 0);
            if (timeStamp != 0) AlarmReceiver.setEveryDayAlarm(context, timeStamp);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Log.i(LOG_TAG, "onReceive");

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder builder = new Notification.Builder(context);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_hand)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setTicker("Write down about this day")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Good Things")
                .setContentText("Write down about this day");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String sound = prefs.getString("notifications_new_message_ringtone", "");
        if (sound != "") {
            builder.setSound(Uri.parse(sound));
        }
        if (prefs.getBoolean("notifications_new_message_vibrate",false))
            builder.setDefaults(Notification.DEFAULT_VIBRATE);

        Notification notification = builder.getNotification();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(101, notification);

        Calendar cal = Calendar.getInstance();
        //if end of year
        if (cal.get(Calendar.DAY_OF_YEAR) == cal.getActualMaximum(Calendar.DAY_OF_YEAR)){
            (new setNotifyAsync(setNotifyAsync.YEAR_NOTIFY)).execute();
            mPeriod = "year";
            return;
        }

        //if end of month
        if (cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH)){
            (new setNotifyAsync(setNotifyAsync.MONTH_NOTIFY)).execute();
            mPeriod = "month";
            return;
        }

        //if end of week
        if (cal.get(Calendar.DAY_OF_WEEK) == cal.getActualMaximum(Calendar.DAY_OF_WEEK)){
            (new setNotifyAsync(setNotifyAsync.WEEK_NOTIFY)).execute();
            mPeriod = "week";
            return;
        }
    }

    public static void setEveryDayAlarm(Context context, long timeStamp){
        final int REQUEST_CODE = 0;

        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        int alarmType = AlarmManager.RTC_WAKEUP;

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Date date = new Date(timeStamp);
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY,date.getHours());
        calendar.set(Calendar.MINUTE,date.getMinutes());
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        timeStamp = calendar.getTimeInMillis();

        Log.i(LOG_TAG, "Timestamp before: " + (new Date(timeStamp)).toString());
        if (timeStamp < System.currentTimeMillis())
            timeStamp += AlarmManager.INTERVAL_DAY;

        alarmManager.setRepeating(alarmType, timeStamp,AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.i(LOG_TAG, "Set alarm to: " + (new Date(timeStamp)).toString());
        Log.i(LOG_TAG, "Now is: " + (new Date(System.currentTimeMillis())).toString());
        Log.i(LOG_TAG, "delta: " + Long.toString(timeStamp - System.currentTimeMillis()));
    }

    public static void clearNotifications(Context context){
        final int REQUEST_CODE = 0;

        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Toast.makeText(context, "Notifications are off", Toast.LENGTH_SHORT).show();
    }

    class setNotifyAsync extends AsyncTask<Void,Void,Void>{
        public static final int YEAR_NOTIFY = 0;
        public static final int MONTH_NOTIFY = 1;
        public static final int WEEK_NOTIFY = 2;
        int period;
        DbHelper dbHelper;
        SQLiteDatabase database;
        Calendar calendar;
        int[] count;

        public setNotifyAsync(int period){
            this.period = period;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dbHelper = new DbHelper(mContext);
            database = dbHelper.getWritableDatabase();
            calendar = new GregorianCalendar();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String where = null;
                switch (period) {
                    case YEAR_NOTIFY: {
                        where = DbHelper.DATE + " LIKE '__/__/" + calendar.get(Calendar.YEAR) + "'";
                        Log.d(LOG_TAG, "where " + where);
                        break;
                    }
                    case MONTH_NOTIFY:
                    case WEEK_NOTIFY:{
                        int currentMonth = calendar.get(Calendar.MONTH) + 1;
                        String currentMonthString = Integer.toString(currentMonth);
                        if (currentMonth < 10) {
                            StringBuilder stringBuilder = new StringBuilder(currentMonthString);
                            stringBuilder.insert(0, '0');
                            currentMonthString = stringBuilder.toString();
                        }
                        where = DbHelper.DATE + " LIKE '" + currentMonthString + "/__/____'";
                        Log.d(LOG_TAG, "where " + where);
                        break;
                    }
                }
                Cursor cursor = database.query(DbHelper.TABLE_NAME, null, where, null, null, null, null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = new int[3];
                    calendar.setTime(new Date());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE,0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND,0);

                    calendar.set(Calendar.DAY_OF_WEEK, 1);
                    long firstDayOfWeek = calendar.getTimeInMillis();
                    calendar.set(Calendar.DAY_OF_WEEK,7);
                    long lastDayOfWeek = calendar.getTimeInMillis();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        long cursorDate = Date.parse(cursor.getString(1));
                        if (period == WEEK_NOTIFY && (cursorDate < firstDayOfWeek || cursorDate > lastDayOfWeek)) {
                            cursor.moveToNext();
                            continue;
                        }
                        if (cursor.getString(2).length() != 0) count[0]++;
                        if (cursor.getString(3).length() != 0) count[1]++;
                        if (cursor.getString(4).length() != 0) count[2]++;

                        cursor.moveToNext();
                    }
                    cursor.close();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            long when = System.currentTimeMillis()+60000;

            Notification.Builder builder = new Notification.Builder(mContext);
            builder
                    .setSmallIcon(R.drawable.ic_stat_hand)
                    .setTicker("Your Results")
                    .setWhen(when);
            Notification notification = builder.getNotification();// new Notification(R.drawable.ic_stat_hand,"Your Results",when);

            NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            RemoteViews contentView = new RemoteViews(mContext.getPackageName(),R.layout.notification_statistics);
            String notificationHeader = String.format(mContext.getString(R.string.notification_with_period),mPeriod);
                    contentView.setTextViewText(R.id.notification_header, notificationHeader);
            contentView.setTextViewText(R.id.good_notification,Integer.toString(count[0]));
            contentView.setTextViewText(R.id.deals_notification,Integer.toString(count[1]));
            contentView.setTextViewText(R.id.better_notification,Integer.toString(count[2]));

            String link = "http://play.google.com/";
            String stringToSend = String.format(mContext.getString(R.string.sharing_in_notification),mPeriod, count[0],count[1],count[2],link);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, stringToSend);
            PendingIntent shareIntent = PendingIntent.getActivity(mContext,0,intent,0);

            contentView.setOnClickPendingIntent(R.id.share_stat,shareIntent);
            notification.contentView = contentView;

            Intent notificationIntent = new Intent(mContext,MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
            notification.contentIntent = contentIntent;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_LIGHTS;
            if (prefs.getBoolean("notifications_new_message_vibrate",false))
                notification.defaults |= Notification.DEFAULT_VIBRATE;
            String sound = prefs.getString("notifications_new_message_ringtone","");
            if (sound != "") {
                notification.sound = Uri.parse(sound);
            }

            mNotificationManager.notify(102,notification);

            database.close();
            super.onPostExecute(aVoid);
        }
    }
}

