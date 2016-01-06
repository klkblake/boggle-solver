package com.klkblake.bogglesolver;

import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.view.View;

/**
 * Created by kyle on 6/01/16.
 */
public class Word implements Parcelable {
    public final String word;
    public final int score;

    public Word(String word) {
        this.word = word;
        switch (word.length()) {
            case 3:
            case 4:
                score = 1;
                break;
            case 5:
                score = 2;
                break;
            case 6:
                score = 3;
                break;
            case 7:
                score = 5;
                break;
            default:
                score = 11;
        }
    }

    protected Word(Parcel in) {
        word = in.readString();
        score = in.readInt();
    }

    public static final Creator<Word> CREATOR = new Creator<Word>() {
        @Override
        public Word createFromParcel(Parcel in) {
            return new Word(in);
        }

        @Override
        public Word[] newArray(int size) {
            return new Word[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(word);
        dest.writeInt(score);
    }
}
