package com.klkblake.bogglesolver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int MIN_SIZE = 4;
    private static final int MAX_SIZE = 6;
    private static final String PREF_GRID_SIZE = "grid_size";
    private Spinner sizeSpinner;
    private int currentSize = 4;
    private EditText[] letters = new EditText[MAX_SIZE * MAX_SIZE];
    private Button solveButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean editing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentSize = getPreferences(MODE_PRIVATE).getInt(PREF_GRID_SIZE, MIN_SIZE);
        sizeSpinner = (Spinner) findViewById(R.id.sizeSpinner);
        sizeSpinner.setSelection(currentSize - MIN_SIZE);
        sizeSpinner.setOnItemSelectedListener(this);
        SquareGridLayout layout = (SquareGridLayout) findViewById(R.id.letterGrid);
        for (int y = 0; y < MAX_SIZE; y++) {
            for (int x = 0; x < MAX_SIZE; x++) {
                EditText letter = (EditText) getLayoutInflater().inflate(R.layout.letter_entry, layout, false);
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) letter.getLayoutParams();
                params.columnSpec = GridLayout.spec(x, GridLayout.FILL, 1);
                params.rowSpec = GridLayout.spec(y, GridLayout.FILL, 1);
                letter.setLayoutParams(params);
                int i = x + y * MAX_SIZE;
                letter.setId(0xffff00 | x << 4 | y);
                letter.addTextChangedListener(new LetterWatcher(i));
                letter.setOnFocusChangeListener(new LetterFocusListener());
                if (x >= currentSize || y >= currentSize) {
                    letter.setVisibility(View.GONE);
                }
                letters[i] = letter;
                layout.addView(letter);
            }
        }
        solveButton = (Button) findViewById(R.id.solveButton);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int newSize = position + MIN_SIZE;
        if (currentSize != newSize) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_GRID_SIZE, newSize).apply();
            currentSize = newSize;
            for (int y = 0; y < MAX_SIZE; y++) {
                for (int x = 0; x < MAX_SIZE; x++) {
                    int i = x + y * MAX_SIZE;
                    if (x >= currentSize || y >= currentSize) {
                        letters[i].setVisibility(View.GONE);
                    } else {
                        letters[i].setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        throw new RuntimeException("Impossible");
    }

    private class LetterWatcher implements TextWatcher {
        private final int index;

        public LetterWatcher(int index) {
            this.index = index;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (editing) {
                return;
            }
            if ((s.length() > 1 || s.length() == 1 && Character.isLowerCase(s.charAt(0)))) {
                int i;
                for (i = index + 1; i < letters.length && letters[i].getVisibility() == View.GONE; i++) {
                }
                if (i == letters.length) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(solveButton.getWindowToken(), 0, new ResultReceiver(handler) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            solveButton.requestFocus();
                        }
                    });
                } else {
                    letters[i].requestFocus();
                }
            }
        }
    }

    private class LetterFocusListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            EditText letter = (EditText) v;
            Editable s = letter.getText();
            if (s.length() == 0) {
                return;
            }
            if (hasFocus) {
                if (s.length() == 1) {
                    editing = true;
                    s.replace(0, 1, "" + Character.toLowerCase(s.charAt(0)));
                    editing = false;
                }
            } else {
                editing = true;
                boolean isUpper = Character.isUpperCase(s.charAt(0));
                int expected = isUpper ? 2 : 1;
                if (s.length() > expected) {
                    s.delete(expected, s.length());
                }
                if (isUpper) {
                    if (s.length() == 2) {
                        s.replace(1, 2, "" + Character.toLowerCase(s.charAt(1)));
                    }
                } else {
                    s.replace(0, 1, "" + Character.toUpperCase(s.charAt(0)));
                }
                editing = false;
            }
        }
    }
}
