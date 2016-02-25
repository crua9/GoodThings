package com.techreviewsandhelp.goodthings;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.techreviewsandhelp.goodthings.settings.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class EnterFragment extends Fragment implements View.OnClickListener {
    private final String LOG_TAG = getClass().getSimpleName();
    static EnterFragment enterFragment;
    SQLiteDatabase db;
    View rootView;
    String noteId;
    String noteDate;

    public EnterFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static EnterFragment newInstance() {
        if (enterFragment == null) enterFragment = new EnterFragment();
        return enterFragment;
    }

    public String getNoteId()
    {
        return noteId;
    }

    public void setNoteId(String noteId){
        this.noteId = noteId;
    }

    public String getNoteDate(){
        return noteDate;
    }

    public void setNoteDate(String noteDate){
        this.noteDate = noteDate;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("lyfecycles", "Enter.onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("lyfecycles", "Enter.onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("lyfecycles","Enter.onCreateView");

        rootView = inflater.inflate(R.layout.fragment_enter, container, false);

        Button buttonOk = (Button)rootView.findViewById(R.id.buttonOk);
        buttonOk.setOnClickListener(this);

        ImageView addButton = (ImageView)rootView.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDB();

                EditText thingsEditText = (EditText)rootView.findViewById(R.id.editThings);
                EditText dealsEditText = (EditText)rootView.findViewById(R.id.editDeals);
                EditText betterEditText = (EditText)rootView.findViewById(R.id.editBetter);
                setNoteId(null);
                setNoteDate(null);
                thingsEditText.setText("");
                dealsEditText.setText("");
                betterEditText.setText("");
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("lyfecycles", "Enter.onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("lyfecycles", "Enter.onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("lyfecycles", "Enter.onResume");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            noteId = savedInstanceState.getString("noteId");
            Log.i(LOG_TAG,"Loaded id from bundle: "+ noteId);
        }

        Log.d("lyfecycles", "Enter.onViewCreated");
        if (savedInstanceState != null) Log.d("lyfecycle",view.toString() + " " + savedInstanceState.toString());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.d("lyfecycles", "Enter.onViewStateRestored");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("lyfecycles", "Enter.onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("lyfecycles", "Enter.onStop");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("lyfecycles", "Enter.onDetach");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("lyfecycles", "Enter.onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("lyfecycles", "Enter.onDestroy");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("lyfecycles", "Enter.onSaveInstanceState");
        if (noteId != null) {
            outState.putString("noteId", noteId);
            Log.v("Saving noteId: ", noteId);
        }
    }

    @Override
    public void onClick(View v) {
        saveToDB();

    }

    public void saveToDB(){
        Context context = getContext();
        EditText thingsEditText = (EditText)rootView.findViewById(R.id.editThings);
        EditText dealsEditText = (EditText)rootView.findViewById(R.id.editDeals);
        EditText betterEditText = (EditText)rootView.findViewById(R.id.editBetter);

        String thingsText = thingsEditText.getText().toString();
        String dealsText = dealsEditText.getText().toString();
        String betterText = betterEditText.getText().toString();

        if (thingsText.length() != 0 || dealsText.length() != 0 || betterText.length() != 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            String date;
            if (noteDate == null) {
                date = dateFormat.format(new Date());
            } else {
                date = noteDate;
            }

            ContentValues values = new ContentValues();
            values.put(DbHelper.DATE, date);
            values.put(DbHelper.THINGS, thingsText);
            values.put(DbHelper.Deeds, dealsText);
            values.put(DbHelper.BETTER, betterText);

            db = GoodThingsTableHelper.getDbInstance(context);

            if (noteId != null){
                int res = db.update(DbHelper.TABLE_NAME, values, "_id = " + noteId, null);
                if (res > 0) Toast.makeText(getContext(), "The note is updated", Toast.LENGTH_SHORT).show();
                else noteId = null;
            }

            if (noteId == null) {
                long idLong = db.insert(DbHelper.TABLE_NAME, null, values);
                noteId = Long.toString(idLong);
                Toast.makeText(getContext(), "The note is added", Toast.LENGTH_SHORT).show();
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (sharedPreferences.getBoolean(SettingsActivity.DRIVE_KEY, false)) {
                ((MainActivity)getActivity()).connectGoogleApi();
            }
            else if (sharedPreferences.getBoolean(SettingsActivity.DROP_KEY, false))
                (new AsyncSync(AsyncSync.SAVE_TO_DROPBOX,getContext())).execute();
        }
        else Toast.makeText(context,"You should enter something",Toast.LENGTH_LONG).show();
    }
}
