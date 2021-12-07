package de.tcrass.minos.model;

import android.os.Parcel;
import android.os.Parcelable;

public class SettingsFormData implements Parcelable, Cloneable {

    private float mazeSize = 0;

    /* --- Getters / Setters --------------------------------------- */

    public float getMazeSize() {
        return mazeSize;
    }

    public void setMazeSize(float mazeSize) {
        this.mazeSize = mazeSize;
    }

    /* --- Constructors -------------------------------------------- */

    public SettingsFormData() {
    }

    private SettingsFormData(float mazeSize) {
        this.mazeSize = mazeSize;
    }

    /* --- Implementation of Parcelable ---------------------------- */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mazeSize);
    }

    public static final Parcelable.Creator<SettingsFormData> CREATOR = new Parcelable.Creator<SettingsFormData>() {

        @Override
        public SettingsFormData createFromParcel(Parcel source) {
            SettingsFormData gameSettings = new SettingsFormData(source.readFloat());
            return gameSettings;
        }

        @Override
        public SettingsFormData[] newArray(int size) {
            return new SettingsFormData[size];
        }
    };

}
