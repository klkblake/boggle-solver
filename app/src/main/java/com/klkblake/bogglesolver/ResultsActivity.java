package com.klkblake.bogglesolver;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ResultsActivity extends AppCompatActivity {
    public static final String TOTAL_SCORE_EXTRA = "total_score";
    public static final String RESULTS_EXTRA = "results";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        TextView totalScoreText = (TextView) findViewById(R.id.totalScore);
        ListView resultsList = (ListView) findViewById(R.id.resultsList);
        Intent intent = getIntent();
        int totalScore = intent.getIntExtra(TOTAL_SCORE_EXTRA, -1);
        Parcelable[] results = intent.getParcelableArrayExtra(RESULTS_EXTRA);
        totalScoreText.setText(String.format("%d", totalScore));
        resultsList.setAdapter(new ResultsAdapter(results));
        setTitle(getResources().getQuantityString(R.plurals.results_title, results.length, results.length));
    }

    private class ResultsAdapter extends ArrayAdapter<Parcelable> {
        public ResultsAdapter(Parcelable[] results) {
            super(ResultsActivity.this, 0, results);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout view;
            if (convertView == null) {
                view = (LinearLayout) getLayoutInflater().inflate(R.layout.result_item, parent, false);
            } else {
                view = (LinearLayout) convertView;
            }
            TextView wordView = (TextView) view.findViewById(R.id.word);
            TextView scoreView = (TextView) view.findViewById(R.id.score);
            Word word = (Word) getItem(position);
            wordView.setText(word.word);
            scoreView.setText(String.format("%d", word.score));
            return view;
        }
    }
}
