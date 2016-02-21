package com.techreviewsandhelp.goodthings;

public class NoteModel {
    private String id;
    private String goodThings;
    private String goodDeals;
    private String better;
    private String date;

    public NoteModel(){}

    public NoteModel(String id, String date, String goodThings, String goodDeals, String better){
        this.id = id;
        this.goodThings = goodThings;
        this.goodDeals = goodDeals;
        this.better = better;
    }

    public String getDate(){
        return this.date;
    }

    public void setDate(String date){
        this.date = date;
    }

    public String getId(){
        return this.id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getGoodThings(){
        return this.goodThings;
    }

    public void setGoodThings(String goodThings){
        this.goodThings = goodThings;
    }

    public String getGoodDeals(){
        return this.goodDeals;
    }

    public void setGoodDeals(String goodDeals){
        this.goodDeals = goodDeals;
    }

    public String getBetter(){
        return this.better;
    }

    public void setBetter(String better){
        this.better = better;
    }
}
