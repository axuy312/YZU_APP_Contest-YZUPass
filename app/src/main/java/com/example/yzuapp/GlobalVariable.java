package com.example.yzuapp;

import android.app.Application;

public class GlobalVariable extends Application {
    private String card;
    private String Sid;

    public void setCardID(String c) {
        this.card = c;
    }

    public String getCardID() {
        return card;
    }

    public void setSid(String in)
    {this.Sid = in;}

    public String getSid(){
        return Sid;
    }
}