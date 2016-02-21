package com.techreviewsandhelp.goodthings;


import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private List<NoteModel> notes;
    private Activity parentActivity;

    /*public RecyclerViewAdapter(List<NoteModel> notes) {
        this.notes = notes;
    }*/

    public RecyclerViewAdapter(List<NoteModel> notes, Activity parentActivity) {
        this.notes = notes;
        this.parentActivity = parentActivity;
    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_three_lines, parent, false);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("ElemOnClick",v.toString());
                TextView thingsText = (TextView) v.findViewById(R.id.thingsText);
                TextView dealsText = (TextView) v.findViewById(R.id.dealsText);
                TextView betterText = (TextView) v.findViewById(R.id.betterText);
                TextView _id = (TextView) v.findViewById(R.id._id);
                TextView date = (TextView)parentActivity.findViewById(R.id.date_on_list_view);

                Intent intent = new Intent();
                intent.putExtra("_id", _id.getText());
                intent.putExtra("date", date.getText());
                intent.putExtra("field1", thingsText.getText());
                intent.putExtra("field2", dealsText.getText());
                intent.putExtra("field3", betterText.getText());
                parentActivity.setResult(Activity.RESULT_OK, intent);
                parentActivity.finish();
            }
        });
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        NoteModel note = notes.get(position);
        Log.i("myrecycler", note.getGoodThings());

        String goodThingsText = note.getGoodThings();
        String goodDealsText = note.getGoodDeals();
        String betterText = note.getBetter();

        holder.idView.setText(note.getId());
        holder.goodThingsView.setText(goodThingsText);
        holder.goodDealsView.setText(goodDealsText);
        holder.betterView.setText(betterText);
        holder.deleteButtonListener.setNote(note);
        if (goodThingsText.length() == 0){
            holder.goodThingsView.setVisibility(View.GONE);
            holder.gCoin.setVisibility(View.GONE);
        }
        if(goodDealsText.length() == 0){
            holder.goodDealsView.setVisibility(View.GONE);
            holder.dCoin.setVisibility(View.GONE);
        }
        if(betterText.length() == 0){
            holder.betterView.setVisibility(View.GONE);
            holder.bCoin.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    private void delete(NoteModel note){
        GoodThingsTableHelper.deleteById(note.getId());
        int position = notes.indexOf(note);
        notes.remove(position);
        notifyItemRemoved(position);
        ((OneDayActivity)parentActivity).refreshCoinsAmount();
    }


    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView idView;
        private TextView goodThingsView;
        private TextView goodDealsView;
        private TextView betterView;
        private ImageButton deleteButton;
        private DeleteButtonListener deleteButtonListener;
        private ImageView gCoin;
        private ImageView dCoin;
        private ImageView bCoin;

        public ViewHolder(View itemView) {
            super(itemView);
            idView = (TextView)itemView.findViewById(R.id._id);
            goodThingsView = (TextView)itemView.findViewById(R.id.thingsText);
            goodDealsView = (TextView)itemView.findViewById(R.id.dealsText);
            betterView = (TextView)itemView.findViewById(R.id.betterText);
            deleteButton = (ImageButton)itemView.findViewById(R.id.btn_action_delete);
            deleteButtonListener = new DeleteButtonListener();
            deleteButton.setOnClickListener(deleteButtonListener);

            gCoin = (ImageView)itemView.findViewById(R.id.g_coin_at_list);
            dCoin = (ImageView)itemView.findViewById(R.id.d_coin_at_list);
            bCoin = (ImageView)itemView.findViewById(R.id.b_coin_at_list);
        }

        @Override
        public void onClick(View v) {

        }

        private class DeleteButtonListener implements View.OnClickListener {
            private NoteModel note;

            @Override
            public void onClick(View v) {
                delete(note);
            }

            public void setNote(NoteModel note) {
                this.note = note;
            }
        }
    }
}