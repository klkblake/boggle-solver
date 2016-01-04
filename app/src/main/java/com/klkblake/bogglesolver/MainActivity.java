package com.klkblake.bogglesolver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private EditText[] letters = new EditText[5];
    private LetterWatcher[] letterWatchers = new LetterWatcher[5];
    private Button solveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        letters[0] = (EditText) findViewById(R.id.letter00);
        letters[1] = (EditText) findViewById(R.id.letter10);
        letters[2] = (EditText) findViewById(R.id.letter20);
        letters[3] = (EditText) findViewById(R.id.letter30);
        letters[4] = (EditText) findViewById(R.id.letter40);
        solveButton = (Button) findViewById(R.id.solveButton);
        for (int i = 0; i < letters.length; i++) {
            letterWatchers[i] = new LetterWatcher(i);
            letters[i].addTextChangedListener(letterWatchers[i]);
            letters[i].setOnFocusChangeListener(new LetterFocusListener(i));
        }
    }

    private class LetterWatcher implements TextWatcher {
        private final int index;
        public boolean editing = false;

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
                if (index == letters.length - 1) {
                    letters[index].clearFocus();
                } else {
                    EditText letter = letters[index + 1];
                    letter.requestFocus();
                }
            }
        }
    }

    private class LetterFocusListener implements View.OnFocusChangeListener {
        private int index;

        public LetterFocusListener(int index) {
            this.index = index;
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            EditText letter = (EditText) v;
            Editable s = letter.getText();
            if (s.length() == 0) {
                return;
            }
            if (hasFocus) {
                if (s.length() == 1) {
                    // TODO move editing to parent class
                    letterWatchers[index].editing = true;
                    s.replace(0, 1, "" + Character.toLowerCase(s.charAt(0)));
                    letterWatchers[index].editing = false;
                }
                letter.selectAll();
            } else {
                letterWatchers[index].editing = true;
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
                letterWatchers[index].editing = false;
            }
        }
    }
}
