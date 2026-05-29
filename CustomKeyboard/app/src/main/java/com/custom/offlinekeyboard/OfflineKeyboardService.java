package com.custom.offlinekeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.view.inputmethod.EditorInfo;

public class OfflineKeyboardService extends InputMethodService implements View.OnTouchListener {

    // UI States
    private int languageMode = 0; // 0 = EN, 1 = TE Vowels (అ), 2 = TE Consonants (క)
    private boolean isShifted = false;
    private boolean isSymbolsMode = false;

    private Button[] letterButtons = new Button[26];
    private Button shiftButton;
    private Button modeButton;
    private Button languageButton;

    // ID mapping for keys
    private final int[] buttonIds = {
            R.id.key_1_1, R.id.key_1_2, R.id.key_1_3, R.id.key_1_4, R.id.key_1_5,
            R.id.key_1_6, R.id.key_1_7, R.id.key_1_8, R.id.key_1_9, R.id.key_1_10,
            R.id.key_2_1, R.id.key_2_2, R.id.key_2_3, R.id.key_2_4, R.id.key_2_5,
            R.id.key_2_6, R.id.key_2_7, R.id.key_2_8, R.id.key_2_9,
            R.id.key_3_1, R.id.key_3_2, R.id.key_3_3, R.id.key_3_4, R.id.key_3_5,
            R.id.key_3_6, R.id.key_3_7
    };

    // Character Mappings

    // 1. English (26 keys)
    private final String[] englishChars = {
            "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m"
    };

    // 2. General Symbols Mode (?123) (26 keys)
    private final String[] symbolChars = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
            "@", "#", "$", "_", "&", "-", "+", "(", ")",
            "*", "\"", "'", ":", ";", "!", "?"
    };

    // 3. Telugu Vowels Layer (Primary) - అ, ఆ, ఇ, ఈ...
    private final String[] telVowelsNormal = {
            "అ", "ఆ", "ఇ", "ఈ", "ఉ", "ఊ", "ఋ", "ఎ", "ఏ", "ఐ",
            "ఒ", "ఓ", "ఔ", "ం", "ః", "్", "ా", "ి", "ీ",
            "ు", "ూ", "ృ", "ె", "ే", "ై", "ొ"
    };

    // 4. Telugu Vowels Layer (Shifted) - Includes remaining maatras, Telugu digits & symbols
    private final String[] telVowelsShift = {
            "ో", "ౌ", "౧", "౨", "౩", "౪", "౫", "౬", "౭", "౮",
            "౯", "౦", "ౘ", "ౙ", "ౠ", "ౡ", "ౢ", "ౣ", "ఁ",
            "ఀ", "ఄ", "«", "»", "‘", "’", "•"
    };

    // 5. Telugu Consonants Layer (Primary) - క, ఖ, గ, ఘ...
    private final String[] telConsonantsNormal = {
            "క", "ఖ", "గ", "ఘ", "చ", "ఛ", "జ", "ఝ", "ట", "ఠ",
            "డ", "ఢ", "త", "థ", "ద", "ధ", "న", "ప", "ఫ",
            "బ", "భ", "మ", "య", "ర", "ల", "వ"
    };

    // 6. Telugu Consonants Layer (Shifted) - Remaining rare consonants & repeats
    private final String[] telConsonantsShift = {
            "ఙ", "ఞ", "శ", "ష", "స", "హ", "ళ", "ఱ", "క్ష", "ౘ",
            "క", "గ", "చ", "జ", "ట", "డ", "త", "ద", "ప",
            "బ", "మ", "య", "ర", "ల", "వ", "స"
    };

    @Override
    public View onCreateInputView() {
        View keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);

        // Initialize and set Instant Touch Listener for 26 Standard Keys
        for (int i = 0; i < buttonIds.length; i++) {
            letterButtons[i] = keyboardView.findViewById(buttonIds[i]);
            letterButtons[i].setOnTouchListener(this);
        }

        // Initialize Functional Keys
        shiftButton = keyboardView.findViewById(R.id.key_shift);
        shiftButton.setOnTouchListener(this);

        modeButton = keyboardView.findViewById(R.id.key_mode);
        modeButton.setOnTouchListener(this);

        languageButton = keyboardView.findViewById(R.id.key_language);
        languageButton.setOnTouchListener(this);

        keyboardView.findViewById(R.id.key_delete).setOnTouchListener(this);
        keyboardView.findViewById(R.id.key_space).setOnTouchListener(this);
        keyboardView.findViewById(R.id.key_period).setOnTouchListener(this);
        keyboardView.findViewById(R.id.key_enter).setOnTouchListener(this);

        updateKeyboardLabels();
        return keyboardView;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Trigger INSTANTLY on finger down! No more waiting for Action_Up.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Premium Instant Haptic Vibration feedback!
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
            
            // Handle the logical key press
            handleKeyPress(view);
            return true; // Consumes event, bypasses Android's 200ms click listener delay entirely!
        }
        return false;
    }

    private void handleKeyPress(View view) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        int viewId = view.getId();

        if (viewId == R.id.key_shift) {
            handleShift();
        } else if (viewId == R.id.key_mode) {
            handleModeSwitch();
        } else if (viewId == R.id.key_language) {
            handleLanguageSwitch();
        } else if (viewId == R.id.key_delete) {
            ic.deleteSurroundingText(1, 0);
        } else if (viewId == R.id.key_space) {
            ic.commitText(" ", 1);
        } else if (viewId == R.id.key_period) {
            // Dynamic period key based on language mode!
            if (languageMode > 0) {
                ic.commitText("।", 1); // Commit Telugu poorna virama punctuation
            } else {
                ic.commitText(".", 1);
            }
        } else if (viewId == R.id.key_enter) {
            handleEnter(ic);
        } else {
            // Must be one of the primary 26 letter keys
            Button clickedButton = (Button) view;
            CharSequence label = clickedButton.getText();
            
            if (label != null && label.length() > 0) {
                ic.commitText(label, 1);
                
                // If shifted (single capital/alternate state), auto-reset after one keystroke
                if (isShifted) {
                    isShifted = false;
                    updateKeyboardLabels();
                }
            }
        }
    }

    private void handleShift() {
        isShifted = !isShifted;
        updateKeyboardLabels();
    }

    private void handleModeSwitch() {
        isSymbolsMode = !isSymbolsMode;
        isShifted = false;
        updateKeyboardLabels();
    }

    private void handleLanguageSwitch() {
        // Cycle language mode: 0 (EN) -> 1 (Telugu Vowels) -> 2 (Telugu Consonants) -> 0
        languageMode = (languageMode + 1) % 3;
        isSymbolsMode = false;
        isShifted = false;
        updateKeyboardLabels();
    }

    private void handleEnter(InputConnection ic) {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        int action = editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
        
        if (action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_NEXT ||
            action == EditorInfo.IME_ACTION_SEARCH ||
            action == EditorInfo.IME_ACTION_SEND ||
            action == EditorInfo.IME_ACTION_DONE) {
            ic.performEditorAction(action);
        } else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void updateKeyboardLabels() {
        String[] activeCharset;

        // Determine active charset
        if (isSymbolsMode) {
            activeCharset = symbolChars;
        } else if (languageMode == 1) {
            // Telugu Vowels layer
            activeCharset = isShifted ? telVowelsShift : telVowelsNormal;
        } else if (languageMode == 2) {
            // Telugu Consonants layer
            activeCharset = isShifted ? telConsonantsShift : telConsonantsNormal;
        } else {
            // Standard English
            activeCharset = englishChars;
        }

        // 1. Set standard key labels
        for (int i = 0; i < letterButtons.length; i++) {
            String label = activeCharset[i];
            
            // Capitalize English letters if shifted
            if (languageMode == 0 && !isSymbolsMode && isShifted) {
                label = label.toUpperCase();
            }
            letterButtons[i].setText(label);
        }

        // 2. Set Mode Button label (?123 vs ABC)
        if (isSymbolsMode) {
            modeButton.setText("ABC");
        } else {
            modeButton.setText("?123");
        }

        // 3. Set Language Button Label based on current state
        if (languageMode == 0) {
            languageButton.setText("EN");
        } else if (languageMode == 1) {
            languageButton.setText("అ"); // Displays 'A' in Telugu
        } else {
            languageButton.setText("క"); // Displays 'Ka' in Telugu
        }

        // 4. Set Shift Button state/appearance
        if (isSymbolsMode) {
            shiftButton.setText(""); 
            shiftButton.setEnabled(false);
            shiftButton.setAlpha(0.3f);
        } else {
            shiftButton.setEnabled(true);
            shiftButton.setAlpha(1.0f);
            
            // Shift representation
            if (languageMode > 0) {
                // For Telugu layers, shift accesses rare glyphs or digits
                shiftButton.setText(isShifted ? "②" : "①");
            } else {
                // For English, shift represents Uppercase
                shiftButton.setText(isShifted ? "⬆" : "⇧");
            }
        }
    }
}
