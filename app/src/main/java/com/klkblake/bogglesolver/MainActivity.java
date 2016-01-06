package com.klkblake.bogglesolver;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int MIN_SIZE = 4;
    private static final int MAX_SIZE = 6;
    private static final String PREF_GRID_SIZE = "grid_size";
    private ProgressBar progressBar;
    private int currentSize = 4;
    private EditText[] letters = new EditText[MAX_SIZE * MAX_SIZE];
    private Button solveButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean editing = false;

    private MappedByteBuffer dict;
    private SolveTask solveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        currentSize = getPreferences(MODE_PRIVATE).getInt(PREF_GRID_SIZE, MIN_SIZE);
        Spinner sizeSpinner = (Spinner) findViewById(R.id.sizeSpinner);
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
                letter.setFilters(new InputFilter[] { new LetterFilter() });
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
        if (dict == null) {
            AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.words);
            FileChannel chan = new FileInputStream(fd.getFileDescriptor()).getChannel();
            try {
                dict = chan.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getLength());
            } catch (IOException e) {
                Log.e("BoggleSolver", "Couldn't map dictionary", e);
                throw new RuntimeException(e);
            }
            dict.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int newSize = position + MIN_SIZE;
        if (currentSize != newSize) {
            if (solveTask != null) {
                solveTask.cancel(true);
            }
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

    public void solve(View view) {
        if (solveTask != null) {
            solveTask.cancel(true);
        }
        String[] letterValues = new String[currentSize * currentSize];
        for (int y = 0; y < currentSize; y++) {
            for (int x = 0; x < currentSize; x++) {
                int i = x + y * MAX_SIZE;
                String str = letters[i].getText().toString();
                if (str.length() == 0) {
                    new AlertDialog.Builder(this)
                            .setMessage("Some entries are missing; please fill them in")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                letterValues[x + y * currentSize] = str.toLowerCase();
            }
        }
        solveTask = new SolveTask();
        solveTask.execute(letterValues);
    }

    private class SolveTask extends AsyncTask<String, Integer, Word[]> {
        private final int size = currentSize;
        private final HashSet<String> words = new HashSet<>();
        private String[] dice;
        private int progress = 0;
        private int totalScore = 0;

        @Override
        protected Word[] doInBackground(String... params) {
            publishProgress(0);
            byte[] prefix = new byte[32];
            dice = params;
            int[] offsets = new int[26];
            boolean[] wordEnds = new boolean[26];
            decode(0, offsets, wordEnds);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    String die = dice[x + y * size];
                    byte letter1 = (byte) die.charAt(0);
                    if (die.length() == 1) {
                        search(offsets, wordEnds, prefix, 0, letter1, (byte)-1, x, y, 0);
                    } else {
                        byte letter2 = (byte) die.charAt(1);
                        search(offsets, wordEnds, prefix, 0, letter1, letter2, x, y, 0);
                    }
                }
            }
            String[] sortedWords = words.toArray(new String[words.size()]);
            Arrays.sort(sortedWords, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    if (lhs.length() != rhs.length()) {
                        return rhs.length() - lhs.length();
                    }
                    return lhs.compareTo(rhs);
                }
            });
            Word[] results = new Word[sortedWords.length];
            for (int i = 0; i < results.length; i++) {
                results[i] = new Word(sortedWords[i]);
                totalScore += results[i].score;
            }
            return results;
        }

        private void decode(int offset, int[] offsets, boolean[] wordEnds) {
            dict.position(offset);
            int numChildren = dict.get() & 0xff;
            for (int i = 0; i < numChildren; i++) {
                int c = dict.get() & 0xff;
                int entry = dict.getInt() & 0xffffff;
                offsets[c] = entry & 0x7fffff;
                wordEnds[c] = (entry & 0x800000) != 0;
                dict.position(dict.position() - 1);
            }
        }

        private void search(int[] offsets, boolean[] wordEnds, byte[] prefix, int len,
                            byte letter, byte next, int x, int y, long visited) {
            if (isCancelled()) {
                return;
            }
            visited |= 1 << (x + y * size);
            prefix[len++] = letter;
            if (wordEnds[letter - 'a'] && next == -1 && len >= 3) {
                char[] chars = new char[len];
                for (int j = 0; j < len; j++) {
                    chars[j] = (char) prefix[j];
                }
                words.add(String.valueOf(chars));
            }
            int offset = offsets[letter - 'a'];
            if (offset == 0) {
                updateProgress(visited);
                return;
            }
            offsets = new int[26];
            wordEnds = new boolean[26];
            decode(offset, offsets, wordEnds);
            if (next != -1) {
                search(offsets, wordEnds, prefix, len, next, (byte)-1, x, y, visited);
                updateProgress(visited);
                return;
            }
            int minx = max(x - 1, 0);
            int miny = max(y - 1, 0);
            int maxx = min(x + 1, size - 1);
            int maxy = min(y + 1, size - 1);
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    int i = xx + yy * size;
                    if ((visited & (1 << i)) != 0) {
                        continue;
                    }
                    String die = dice[i];
                    byte letter1 = (byte) die.charAt(0);
                    if (die.length() == 1) {
                        search(offsets, wordEnds, prefix, len, letter1, (byte)-1, xx, yy, visited);
                    } else {
                        byte letter2 = (byte) die.charAt(1);
                        search(offsets, wordEnds, prefix, len, letter1, letter2, xx, yy, visited);
                    }
                }
            }
            updateProgress(visited);
        }

        private void updateProgress(long visited) {
            if (Long.bitCount(visited) == 2 && !isCancelled()) {
                publishProgress(++progress);
            }
        }

        @Override
            protected void onProgressUpdate(Integer... values) {
            if (values[0] == 0) {
                progressBar.setVisibility(View.VISIBLE);
                int size2 = size - 2;
                int max = 4 * 3 +          // Corners
                        size2 * 4 * 5 +    // Edges
                        size2 * size2 * 8; // Middle
                progressBar.setMax(max);
            }
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Word[] words) {
            onCancelled(words);
            Intent intent = new Intent(getApplicationContext(), ResultsActivity.class);
            intent.putExtra(ResultsActivity.TOTAL_SCORE_EXTRA, totalScore);
            intent.putExtra(ResultsActivity.RESULTS_EXTRA, words);
            startActivity(intent);
        }

        @Override
        protected void onCancelled(Word[] words) {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private static class LetterFilter extends LoginFilter.UsernameFilterGeneric {
        @Override
        public boolean isAllowed(char c) {
            return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z';
        }
    }

    private static boolean enforceLengthLimit(Editable s) {
        boolean isUpper = Character.isUpperCase(s.charAt(0));
        int expected = isUpper ? 2 : 1;
        if (s.length() > expected) {
            s.delete(expected, s.length());
        }
        return isUpper;
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
            if (solveTask != null) {
                solveTask.cancel(true);
            }
            editing = true;
            enforceLengthLimit(s);
            editing = false;
            if ((s.length() > 1 || s.length() == 1 && Character.isLowerCase(s.charAt(0)))) {
                int i;
                for (i = index + 1; i < letters.length && letters[i].getVisibility() == View.GONE; i++) {
                }
                if (i == letters.length) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(solveButton.getWindowToken(), 0, new ResultReceiver(handler) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            solveButton.setFocusableInTouchMode(true);
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
            if (hasFocus) {
                solveButton.setFocusableInTouchMode(false);
                solveButton.setFocusable(false);
            }
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
                boolean isUpper = enforceLengthLimit(s);
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
