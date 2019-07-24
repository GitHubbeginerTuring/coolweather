package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Suggestion {
    @SerializedName("comf")
    public Comfort comfort;

    @SerializedName("cw")
    public CarWash carWash;

 //   @SerializedName("sport")
    public Sport sport;

    class Comfort{
        @SerializedName("txt")
        public String info;

    }
    class CarWash{
        @SerializedName("txt")
        public String info;
    }
    class Sport{
        @SerializedName("txt")
        public String info;
    }
}
