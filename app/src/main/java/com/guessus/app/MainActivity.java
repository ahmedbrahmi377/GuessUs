package com.guessus.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    // ============================================================
    // SUPABASE / GAME
    // ============================================================

    private final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;

    private static final int MAX_PLAYERS = 2;
    private static final int ANSWER_TIME = 45;
    private static final long POLL_LOBBY = 2200L;
    private static final long POLL_GAME = 1600L;
    private static final long POLL_CHAT = 3000L;

    // ============================================================
    // PLAYER / ROOM STATE
    // ============================================================

    private String roomCode = "";
    private String roomId = "";
    private String playerName = "";
    private String playerId = "";

    private int score = 0;
    private int questionIndex = 0;

    private boolean isHost = false;
    private boolean answerSent = false;
    private boolean predictionSent = false;
    private boolean nextReadySent = false;

    private String currentScreen = "";
    private int screenToken = 0;

    // ============================================================
    // SETTINGS / THEME
    // ============================================================

    private SharedPreferences prefs;
    private boolean darkMode;
    private boolean musicEnabled;
    private boolean soundEnabled;

    private int bgColor;
    private int cardColor;
    private int textColor;
    private int secondaryTextColor;
    private int accentColor;
    private int buttonTextColor;

    // ============================================================
    // AUDIO
    // ============================================================

    private MediaPlayer musicPlayer;
    private SoundPool soundPool;

    private int clickSoundId;
    private int successSoundId;
    private int wrongSoundId;
    private int timeUpSoundId;

    private boolean clickLoaded;
    private boolean successLoaded;
    private boolean wrongLoaded;
    private boolean timeUpLoaded;

    // ============================================================
    // ASYNC / TIMERS
    // ============================================================

    private CountDownTimer answerTimer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(3);
    private final AtomicBoolean pollBusy = new AtomicBoolean(false);

    // ============================================================
    // QUESTIONS
    // ============================================================

    private final String[] questions = {
            "شنو أكثر حاجة تفرحك؟",
            "شنو أكثر حاجة تزعجك؟",
            "شنو أكثر أكلة تحبها؟",
            "شنو أكثر لون تحبه؟",
            "شنو أكثر لعبة تحبها؟",
            "شنو أكثر فيلم عجبك؟",
            "شنو أكثر مسلسل تحبه؟",
            "شنو أكثر مادة تحبها؟",
            "شنو أكثر مادة ما تحبهاش؟",
            "شنو أكثر مكان تحب تمشي له؟",
            "لو تربح مليون، شنو أول حاجة تعملها؟",
            "شنو أكثر حاجة تخاف منها؟",
            "شنو أكثر عادة عندك؟",
            "شنو أكثر تطبيق تستعمله؟",
            "شنو أكثر أغنية تحبها؟",
            "شنو أكثر شخصية تحبها؟",
            "شنو أكثر رياضة تحبها؟",
            "شنو أكثر حيوان تحبه؟",
            "شنو أكثر فصل تحبه؟",
            "شنو أكثر وقت في النهار تحبه؟",
            "شنو الحاجة اللي مستحيل تستغنى عليها؟",
            "شنو أكثر حاجة تضحكك؟",
            "شنو أكثر موقف محرج صارلك؟",
            "شنو أكثر حلم عندك؟",
            "شنو أكثر دولة تحب تزورها؟",
            "شنو أكثر صفة تحبها في الشخص؟",
            "شنو أكثر صفة ما تحبهاش؟",
            "شنو أكثر شيء تتمنى يتغير؟",
            "شنو أكثر شيء تحب تتعلمه؟",
            "شنو أكثر ذكرى تحبها؟",
            "لو تنجم تختار قوة خارقة، شنو تختار؟",
            "لو تعيش في عالم لعبة، شنو تختار؟",
            "لو ترجع للماضي، شنو تبدل؟",
            "لو عندك يوم كامل فاضي، شنو تعمل؟",
            "لو تختار شخص يسافر معاك، شكون تختار؟",
            "شنو أول شيء تعملو كي تفيق؟",
            "شنو آخر شيء تعملو قبل ما ترقد؟",
            "شنو أكثر كلمة تستعملها؟",
            "شنو أكثر شيء يخليك متوتر؟",
            "شنو أكثر شيء يعطيك طاقة؟",
            "شنو أكثر موقف تتذكره من طفولتك؟",
            "شنو أكثر مكان ترتاح فيه؟",
            "شنو أكثر شيء تحب في شخصيتك؟",
            "شنو أكثر شيء تحب تغيره في شخصيتك؟",
            "شنو أكثر قرار فرحت بيه؟",
            "شنو أكثر شيء تحب الناس تعرفه عليك؟",
            "شنو أكثر شيء ما يفهموش الناس عليك؟",
            "شنو تتوقع تعمل بعد خمس سنوات؟",
            "شنو أكثر سؤال تحب تسأله للناس؟",
            "شنو تتوقع يكون جواب صاحبك؟"
    };

    // ============================================================
    // ACTIVITY LIFECYCLE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("guessus_settings", MODE_PRIVATE);
        darkMode = prefs.getBoolean("dark_mode", false);
        musicEnabled = prefs.getBoolean("music_enabled", true);
        soundEnabled = prefs.getBoolean("sound_enabled", true);

        setupTheme();
        loadOrCreatePlayerId();
        initAudio();
        showHome();
    }

    private void loadOrCreatePlayerId() {
        playerId = prefs.getString("player_id", "");
        if (playerId == null || playerId.trim().isEmpty()) {
            playerId = UUID.randomUUID().toString();
            prefs.edit().putString("player_id", playerId).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicEnabled) startMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMusic();
    }

    @Override
    protected void onDestroy() {
        stopAllPolling();
        stopTimer();
        releaseMusic();
        releaseAudioEffects();
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if ("home".equals(currentScreen)) {
            super.onBackPressed();
            return;
        }

        if ("settings".equals(currentScreen)) {
            showHome();
            return;
        }

        if (!roomCode.isEmpty()) {
            if ("lobby".equals(currentScreen)) {
                showHome();
            } else {
                showLobby();
            }
        } else {
            showHome();
        }
    }

    // ============================================================
    // THEME
    // ============================================================

    private void setupTheme() {
        if (darkMode) {
            bgColor = Color.rgb(11, 9, 20);
            cardColor = Color.rgb(28, 23, 43);
            textColor = Color.WHITE;
            secondaryTextColor = Color.rgb(195, 187, 215);
            accentColor = Color.rgb(150, 82, 240);
            buttonTextColor = Color.WHITE;
        } else {
            bgColor = Color.rgb(245, 240, 255);
            cardColor = Color.WHITE;
            textColor = Color.rgb(35, 25, 50);
            secondaryTextColor = Color.rgb(105, 92, 125);
            accentColor = Color.rgb(112, 62, 205);
            buttonTextColor = Color.WHITE;
        }

        Window window = getWindow();
        window.setStatusBarColor(bgColor);
        window.setNavigationBarColor(bgColor);

        int flags = 0;
        if (!darkMode) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        prefs.edit().putBoolean("dark_mode", darkMode).apply();
        setupTheme();
        showSettings();
    }

    private void toggleMusic() {
        musicEnabled = !musicEnabled;
        prefs.edit().putBoolean("music_enabled", musicEnabled).apply();
        if (musicEnabled) startMusic(); else stopMusic();
        showSettings();
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        prefs.edit().putBoolean("sound_enabled", soundEnabled).apply();
        showSettings();
        if (soundEnabled) playClick();
    }

    // ============================================================
    // AUDIO
    // ============================================================

    private void initAudio() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attrs)
                    .setMaxStreams(4)
                    .build();

            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status != 0) return;
                if (sampleId == clickSoundId) clickLoaded = true;
                else if (sampleId == successSoundId) successLoaded = true;
                else if (sampleId == wrongSoundId) wrongLoaded = true;
                else if (sampleId == timeUpSoundId) timeUpLoaded = true;
            });

            clickSoundId = soundPool.load(this, R.raw.click, 1);
            successSoundId = soundPool.load(this, R.raw.success, 1);
            wrongSoundId = soundPool.load(this, R.raw.wrong, 1);
            timeUpSoundId = soundPool.load(this, R.raw.time_up, 1);
        } catch (Exception ignored) {
            soundPool = null;
        }
    }

    private void playSound(int soundId, boolean loaded, float volume) {
        if (!soundEnabled || soundPool == null || soundId == 0 || !loaded) return;
        try {
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private void playClick() {
        playSound(clickSoundId, clickLoaded, 0.62f);
    }

    private void playSuccess() {
        playSound(successSoundId, successLoaded, 0.85f);
    }

    private void playWrong() {
        playSound(wrongSoundId, wrongLoaded, 0.80f);
    }

    private void playTimeUp() {
        playSound(timeUpSoundId, timeUpLoaded, 0.90f);
    }

    private void releaseAudioEffects() {
        try {
            if (soundPool != null) {
                soundPool.release();
                soundPool = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void startMusic() {
        if (!musicEnabled) return;
        try {
            if (musicPlayer == null) {
                musicPlayer = MediaPlayer.create(this, R.raw.bg_music);
                if (musicPlayer == null) return;
                musicPlayer.setLooping(true);
                musicPlayer.setVolume(0.28f, 0.28f);
            }
            if (!musicPlayer.isPlaying()) musicPlayer.start();
        } catch (Exception ignored) {
        }
    }

    private void pauseMusic() {
        try {
            if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
        } catch (Exception ignored) {
        }
    }

    private void stopMusic() {
        try {
            if (musicPlayer != null) {
                if (musicPlayer.isPlaying()) musicPlayer.stop();
                musicPlayer.reset();
                musicPlayer.release();
                musicPlayer = null;
            }
        } catch (Exception ignored) {
            musicPlayer = null;
        }
    }

    private void releaseMusic() {
        try {
            if (musicPlayer != null) {
                musicPlayer.release();
                musicPlayer = null;
            }
        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // POLLING / LIFECYCLE HELPERS
    // ============================================================

    private void stopAllPolling() {
        screenToken++;
        handler.removeCallbacksAndMessages(null);
        pollBusy.set(false);
    }

    private int beginScreen(String name) {
        stopAllPolling();
        currentScreen = name;
        return screenToken;
    }

    private boolean isScreen(int token, String name) {
        return token == screenToken && name.equals(currentScreen) && !isFinishing();
    }

    private void schedulePolling(Runnable runnable, long delay, int token, String name) {
        handler.postDelayed(() -> {
            if (isScreen(token, name)) runnable.run();
        }, delay);
    }

    // ============================================================
    // BASIC UI
    // ============================================================

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout root() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(18), dp(18), dp(24));
        layout.setBackgroundColor(bgColor);
        return layout;
    }

    private ScrollView scroll(View child) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(8));
        scroll.addView(child);
        return scroll;
    }

    private void applyBackground(LinearLayout root, String drawableName) {
        try {
            int id = getResources().getIdentifier(drawableName, "drawable", getPackageName());
            if (id != 0) root.setBackgroundResource(id);
        } catch (Exception ignored) {
        }
    }

    private TextView title(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(29);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(13), 0, dp(14));
        return tv;
    }

    private TextView subtitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(secondaryTextColor);
        tv.setTextSize(15);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(4), dp(8), dp(12));
        return tv;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(16);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(4), dp(8), dp(4), dp(7));
        return tv;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(cardColor);
        drawable.setCornerRadius(dp(22));
        card.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(7), 0, dp(7));
        card.setLayoutParams(params);
        return card;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(buttonTextColor);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setStateListAnimator(null);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(accentColor);
        drawable.setCornerRadius(dp(17));
        b.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        params.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(params);
        b.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                playClick();
            }
            return false;
        });
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(secondaryTextColor);
        e.setTextColor(textColor);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(15), 0, dp(15), 0);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(darkMode ? Color.rgb(41, 34, 59) : Color.rgb(242, 238, 249));
        drawable.setCornerRadius(dp(15));
        e.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
        );
        params.setMargins(0, dp(6), 0, dp(6));
        e.setLayoutParams(params);
        return e;
    }

    private void setScreen(View view, String name) {
        currentScreen = name;
        setContentView(view);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(220).start();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String safeMessage(Exception e, String fallback) {
        String message = e == null ? null : e.getMessage();
        if (message == null || message.trim().isEmpty()) return fallback;
        return message;
    }

    private void setBusy(Button button, boolean busy, String busyText, String normalText) {
        if (button == null) return;
        button.setEnabled(!busy);
        button.setAlpha(busy ? 0.65f : 1f);
        button.setText(busy ? busyText : normalText);
    }

    // ============================================================
    // HOME
    // ============================================================

    private void showHome() {
        stopTimer();
        int token = beginScreen("home");

        LinearLayout root = root();
        applyBackground(root, "bg_home");

        TextView logo = title("GuessUs");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(44);
        root.addView(logo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(165)
        ));

        TextView info = subtitle("توقّع إجابات صاحبك وشوف شكون يعرف شكون أكثر 🎯");
        info.setTextColor(Color.WHITE);
        info.setTextSize(16);
        root.addView(info);

        Button create = button("🎮 إنشاء غرفة");
        create.setOnClickListener(v -> showCreateRoom());
        root.addView(create);

        Button join = button("🚪 الانضمام لغرفة");
        join.setOnClickListener(v -> showJoinRoom());
        root.addView(join);

        Button settings = button("⚙️ الإعدادات");
        settings.setOnClickListener(v -> showSettings());
        root.addView(settings);

        TextView features = subtitle("👥 لاعبان • 🎯 توقع • 💬 Chat • 🏆 نقاط • 🎵 موسيقى");
        features.setTextColor(Color.WHITE);
        root.addView(features);

        TextView version = subtitle("GuessUs • Online Party Game");
        version.setTextColor(Color.WHITE);
        root.addView(version);

        setScreen(scroll(root), "home");
        if (musicEnabled && token == screenToken) startMusic();
    }

    // ============================================================
    // CREATE ROOM
    // ============================================================

    private void showCreateRoom() {
        int token = beginScreen("create");
        LinearLayout root = root();

        root.addView(title("إنشاء غرفة 🎮"));
        root.addView(subtitle("أنت الـHost. ادخل اسمك ثم شارك الكود مع صاحبك."));

        EditText name = input("اسمك");
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(18)});
        root.addView(name);

        Button create = button("✨ إنشاء الغرفة");
        root.addView(create);

        create.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) {
                toast("اكتب اسمك أولاً");
                return;
            }

            playerName = n;
            setBusy(create, true, "⏳ جاري الإنشاء...", "✨ إنشاء الغرفة");

            networkExecutor.execute(() -> createRoom(create, token));
        });

        Button back = button("رجوع");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setScreen(scroll(root), "create");
    }

    private void createRoom(Button source, int token) {
        try {
            validateSupabase();

            String createdCode = "";
            JSONObject createdRoom = null;
            Exception lastError = null;

            for (int attempt = 0; attempt < 3; attempt++) {
                String code = generateRoomCode();

                JSONObject room = new JSONObject();
                room.put("code", code);
                room.put("status", "waiting");

                try {
                    String result = request(
                            "POST",
                            api("/rooms"),
                            room.toString()
                    );

                    JSONArray rooms = safeJsonArray(result);
                    if (rooms.length() > 0) {
                        createdRoom = rooms.getJSONObject(0);
                    } else {
                        String lookup = request(
                                "GET",
                                api("/rooms?code=eq." +
                                        URLEncoder.encode(code, "UTF-8") +
                                        "&select=*&limit=1"),
                                null
                        );
                        JSONArray found = safeJsonArray(lookup);
                        if (found.length() > 0) createdRoom = found.getJSONObject(0);
                    }

                    if (createdRoom != null) {
                        createdCode = createdRoom.optString("code", code);
                        break;
                    }
                } catch (Exception e) {
                    lastError = e;
                    if (e.getMessage() == null || !e.getMessage().contains("HTTP 409")) {
                        throw e;
                    }
                }
            }

            if (createdRoom == null) {
                throw new Exception(lastError == null ?
                        "Supabase لم يرجع الغرفة" : lastError.getMessage());
            }

            roomId = createdRoom.optString("id", "");
            roomCode = createdCode;
            isHost = true;
            score = 0;
            questionIndex = 0;

            if (roomId.isEmpty()) throw new Exception("Supabase لم يرجع room id");

            createGameState();
            addPlayer();

            runOnUiThread(() -> {
                if (!isScreen(token, "create")) return;
                playSuccess();
                showLobby();
            });
        } catch (Exception e) {
            String message = safeMessage(e, "تعذر إنشاء الغرفة");
            runOnUiThread(() -> {
                if (!isScreen(token, "create")) return;
                setBusy(source, false, "⏳ جاري الإنشاء...", "✨ إنشاء الغرفة");
                toast("فشل إنشاء الغرفة: " + message);
            });
        }
    }

    private String generateRoomCode() {
        int value = 1000 + (int) (Math.random() * 9000);
        return String.valueOf(value);
    }

    private void createGameState() throws Exception {
        JSONObject state = new JSONObject();
        state.put("room_code", roomCode);
        state.put("question_index", 0);
        state.put("status", "waiting");
        state.put("updated_at", getCurrentTimestamp());
        request("POST", api("/game_state"), state.toString());
    }

    // ============================================================
    // JOIN ROOM
    // ============================================================

    private void showJoinRoom() {
        int token = beginScreen("join");
        LinearLayout root = root();

        root.addView(title("الانضمام 🚪"));
        root.addView(subtitle("ادخل اسمك وكود الغرفة المكوّن من 4 أرقام."));

        EditText name = input("اسمك");
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(18)});
        root.addView(name);

        EditText code = input("كود الغرفة - 4 أرقام");
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        root.addView(code);

        Button join = button("🚀 انضمام");
        root.addView(join);

        join.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String c = code.getText().toString().trim();

            if (n.isEmpty()) {
                toast("اكتب اسمك");
                return;
            }
            if (c.length() != 4) {
                toast("الكود لازم يكون 4 أرقام");
                return;
            }

            playerName = n;
            setBusy(join, true, "⏳ جاري الانضمام...", "🚀 انضمام");
            networkExecutor.execute(() -> joinRoom(c, join, token));
        });

        Button back = button("رجوع");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setScreen(scroll(root), "join");
    }

    private void joinRoom(String code, Button source, int token) {
        try {
            validateSupabase();
            String encoded = URLEncoder.encode(code, "UTF-8");

            JSONArray rooms = safeJsonArray(request(
                    "GET",
                    api("/rooms?code=eq." + encoded +
                            "&select=*&limit=1"),
                    null
            ));

            if (rooms.length() == 0) throw new Exception("الغرفة غير موجودة");

            JSONObject room = rooms.getJSONObject(0);
            String status = room.optString("status", "waiting");
            if (!"waiting".equalsIgnoreCase(status)) {
                throw new Exception("اللعبة بدأت بالفعل");
            }

            roomId = room.optString("id", "");
            roomCode = room.optString("code", code);
            isHost = false;

            if (roomId.isEmpty()) throw new Exception("معرّف الغرفة غير موجود");

            JSONArray players = getPlayers();
            if (players.length() >= MAX_PLAYERS) throw new Exception("الغرفة ممتلئة");

            for (int i = 0; i < players.length(); i++) {
                JSONObject existing = players.optJSONObject(i);
                if (existing != null && existing.optString("name", "").equalsIgnoreCase(playerName)) {
                    throw new Exception("الاسم مستعمل في الغرفة");
                }
            }

            addPlayer();

            runOnUiThread(() -> {
                if (!isScreen(token, "join")) return;
                playSuccess();
                showLobby();
            });
        } catch (Exception e) {
            String message = safeMessage(e, "تعذر الانضمام");
            runOnUiThread(() -> {
                if (!isScreen(token, "join")) return;
                setBusy(source, false, "⏳ جاري الانضمام...", "🚀 انضمام");
                toast("فشل الانضمام: " + message);
            });
        }
    }

    // ============================================================
    // PLAYER DATA
    // ============================================================

    private void addPlayer() throws Exception {
        JSONObject player = new JSONObject();
        player.put("room_id", roomId);
        player.put("room_code", roomCode);
        player.put("name", playerName);
        player.put("is_host", isHost);
        player.put("player_id", playerId);
        player.put("score", 0);
        player.put("ready", false);

        request("POST", api("/players"), player.toString());
    }

    private JSONArray getPlayers() throws Exception {
        return safeJsonArray(request(
                "GET",
                api("/players?room_id=eq." +
                        URLEncoder.encode(roomId, "UTF-8") +
                        "&select=*&order=joined_at.asc"),
                null
        ));
    }

    private JSONObject getMe() throws Exception {
        JSONArray data = safeJsonArray(request(
                "GET",
                api("/players?player_id=eq." +
                        URLEncoder.encode(playerId, "UTF-8") +
                        "&room_id=eq." + URLEncoder.encode(roomId, "UTF-8") +
                        "&select=*&limit=1"),
                null
        ));
        if (data.length() == 0) throw new Exception("اللاعب غير موجود");
        return data.getJSONObject(0);
    }

    // ============================================================
    // LOBBY
    // ============================================================

    private void showLobby() {
        stopTimer();
        int token = beginScreen("lobby");

        LinearLayout root = root();
        // لا نستخدم bg_lobby هنا لأن الصورة القديمة تحتوي على عناصر UI مرسومة بداخلها
        // مثل Lobby / كود تجريبي / زر بدء اللعبة، وهذا يسبب تكرار وتداخل العناصر.
        applyCleanLobbyBackground(root);

        TextView t = title("🎮 Lobby");
        t.setTextColor(Color.WHITE);
        t.setTextSize(30);
        t.setPadding(0, dp(8), 0, dp(10));
        root.addView(t);

        TextView hint = subtitle("الغرفة جاهزة • شارك الكود مع صاحبك");
        hint.setTextColor(Color.WHITE);
        hint.setAlpha(0.90f);
        root.addView(hint);

        // بطاقة الكود: منفصلة وواضحة حتى لا يتداخل مع بقية الواجهة.
        LinearLayout codeCard = card();
        codeCard.setPadding(dp(18), dp(12), dp(18), dp(12));

        TextView codeLabel = label("🔑 كود الغرفة");
        codeLabel.setTextColor(textColor);
        codeLabel.setGravity(Gravity.CENTER);
        codeCard.addView(codeLabel);

        TextView room = new TextView(this);
        room.setText(roomCode == null ? "----" : roomCode);
        room.setTextColor(accentColor);
        room.setTextSize(32);
        room.setGravity(Gravity.CENTER);
        room.setTypeface(null, Typeface.BOLD);
        room.setLetterSpacing(0.12f);
        room.setPadding(0, dp(2), 0, dp(5));
        codeCard.addView(room);
        root.addView(codeCard);

        Button copy = button("📋 نسخ الكود");
        copy.setOnClickListener(v -> copyRoomCode());
        root.addView(copy);

        Button share = button("📤 مشاركة الكود");
        share.setOnClickListener(v -> shareRoomCode());
        root.addView(share);

        TextView status = subtitle("⏳ جاري تحميل اللاعبين...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        root.addView(status);

        LinearLayout playersCard = card();
        playersCard.setPadding(dp(18), dp(10), dp(18), dp(10));
        root.addView(playersCard);
        loadLobbyPlayers(playersCard, status);

        Button ready = button("✅ Ready");
        root.addView(ready);
        ready.setOnClickListener(v -> {
            setBusy(ready, true, "⏳ ...", "✅ Ready");
            networkExecutor.execute(() -> {
                try {
                    setPlayerReady(true);
                    runOnUiThread(() -> {
                        if (!isScreen(token, "lobby")) return;
                        ready.setText("✓ أنت Ready");
                        ready.setEnabled(false);
                        playSuccess();
                        toast("تم تسجيلك Ready");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (!isScreen(token, "lobby")) return;
                        setBusy(ready, false, "⏳ ...", "✅ Ready");
                        toast(safeMessage(e, "تعذر تسجيل Ready"));
                    });
                }
            });
        });

        if (isHost) {
            Button start = button("🚀 بدء اللعبة");
            root.addView(start);
            start.setOnClickListener(v -> {
                setBusy(start, true, "⏳ جاري البدء...", "🚀 بدء اللعبة");
                networkExecutor.execute(() -> {
                    try {
                        JSONArray players = getPlayers();
                        if (players.length() != 2) throw new Exception("يلزم لاعبان بالضبط");
                        for (int i = 0; i < players.length(); i++) {
                            if (!players.getJSONObject(i).optBoolean("ready", false)) {
                                throw new Exception("لازم الاثنين يعملوا Ready");
                            }
                        }
                        setAllReady(false);
                        setRoomStatus("playing");
                        setGameState(0, "answering");
                        questionIndex = 0;
                        runOnUiThread(() -> {
                            if (isScreen(token, "lobby")) {
                                playSuccess();
                                showAnswerScreen();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            if (!isScreen(token, "lobby")) return;
                            setBusy(start, false, "⏳ جاري البدء...", "🚀 بدء اللعبة");
                            toast(safeMessage(e, "تعذر بدء اللعبة"));
                        });
                    }
                });
            });
        }

        Button chat = button("💬 Chat");
        chat.setOnClickListener(v -> showChat());
        root.addView(chat);

        Button leave = button("🚪 مغادرة الغرفة");
        leave.setOnClickListener(v -> leaveRoom());
        root.addView(leave);

        // مسافة سفلية صغيرة تمنع التصاق آخر زر بحافة الشاشة.
        View bottomSpace = new View(this);
        root.addView(bottomSpace, new LinearLayout.LayoutParams(1, dp(12)));

        setScreen(scroll(root), "lobby");
        startLobbyPolling(token, playersCard, status);
    }

    private void applyCleanLobbyBackground(LinearLayout root) {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(18, 8, 43),
                        Color.rgb(49, 18, 91),
                        Color.rgb(17, 12, 45)
                }
        );
        bg.setCornerRadius(0f);
        root.setBackground(bg);
    }

    private void loadLobbyPlayers(LinearLayout card, TextView status) {
        final int token = screenToken;
        networkExecutor.execute(() -> {
            try {
                JSONArray players = getPlayers();
                runOnUiThread(() -> {
                    if (!isScreen(token, "lobby")) return;
                    renderPlayers(card, players);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isScreen(token, "lobby")) status.setText("تعذر تحديث اللاعبين");
                });
            }
        });
    }

    private void renderPlayers(LinearLayout card, JSONArray players) {
        card.removeAllViews();
        TextView header = label("اللاعبين (" + players.length() + "/" + MAX_PLAYERS + ")");
        card.addView(header);

        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.optJSONObject(i);
            if (p == null) continue;
            String name = p.optString("name", "?");
            boolean host = p.optBoolean("is_host", false);
            boolean ready = p.optBoolean("ready", false);

            TextView row = new TextView(this);
            row.setText((host ? "👑 " : "👤 ") + name +
                    (ready ? "   ✓ Ready" : "   • Waiting"));
            row.setTextColor(textColor);
            row.setTextSize(16);
            row.setPadding(0, dp(10), 0, dp(10));
            card.addView(row);
        }
    }

    private void startLobbyPolling(int token, LinearLayout playersCard, TextView status) {
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!isScreen(token, "lobby")) return;
                if (!pollBusy.compareAndSet(false, true)) {
                    schedulePolling(this, 700L, token, "lobby");
                    return;
                }

                networkExecutor.execute(() -> {
                    try {
                        JSONObject me = getMe();
                        isHost = me.optBoolean("is_host", isHost);
                        score = me.optInt("score", score);

                        JSONArray players = getPlayers();
                        JSONObject state = getGameState();
                        String gameStatus = state.optString("status", "waiting");
                        int index = state.optInt("question_index", 0);

                        runOnUiThread(() -> {
                            if (!isScreen(token, "lobby")) return;
                            status.setText("متصل • " + players.length() + " / " + MAX_PLAYERS + " لاعبين");
                            renderPlayers(playersCard, players);
                            if ("answering".equals(gameStatus)) {
                                questionIndex = Math.max(0, Math.min(index, questions.length - 1));
                                showAnswerScreen();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            if (isScreen(token, "lobby")) status.setText("⚠️ الاتصال متذبذب...");
                        });
                    } finally {
                        pollBusy.set(false);
                        schedulePolling(this, POLL_LOBBY, token, "lobby");
                    }
                });
            }
        };
        schedulePolling(poll, 1000L, token, "lobby");
    }

    private void copyRoomCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("GuessUs Room", roomCode));
            playSuccess();
            toast("تم نسخ كود الغرفة 📋");
        }
    }

    private void shareRoomCode() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,
                "🎮 تعال نلعب GuessUs!\nكود الغرفة: " + roomCode);
        startActivity(Intent.createChooser(intent, "مشاركة كود GuessUs"));
    }

    // ============================================================
    // ANSWER SCREEN
    // ============================================================

    private void showAnswerScreen() {
        stopTimer();
        int token = beginScreen("answer");

        questionIndex = Math.max(0, Math.min(questionIndex, questions.length - 1));
        answerSent = false;
        predictionSent = false;
        nextReadySent = false;

        LinearLayout root = root();
        applyBackground(root, "bg_game");

        TextView header = title("السؤال " + (questionIndex + 1) + "/" + questions.length);
        root.addView(header);

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(questions.length);
        progress.setProgress(questionIndex + 1);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        progressParams.setMargins(dp(8), 0, dp(8), dp(14));
        root.addView(progress, progressParams);

        LinearLayout qCard = card();
        TextView q = new TextView(this);
        q.setText(questions[questionIndex]);
        q.setTextColor(textColor);
        q.setTextSize(23);
        q.setGravity(Gravity.CENTER);
        q.setTypeface(null, Typeface.BOLD);
        q.setPadding(dp(5), dp(20), dp(5), dp(20));
        qCard.addView(q);
        root.addView(qCard);

        TextView timer = subtitle("⏱️ " + ANSWER_TIME + " ثانية");
        timer.setTextSize(20);
        timer.setTextColor(accentColor);
        timer.setTypeface(null, Typeface.BOLD);
        root.addView(timer);

        EditText answer = input("اكتب إجابتك...");
        answer.setSingleLine(false);
        answer.setMinHeight(dp(110));
        answer.setGravity(Gravity.TOP | Gravity.RIGHT);
        answer.setPadding(dp(15), dp(12), dp(15), dp(12));
        root.addView(answer);

        Button send = button("📤 إرسال الإجابة");
        root.addView(send);

        Button skip = button("⏭️ تخطي السؤال");
        root.addView(skip);

        send.setOnClickListener(v -> {
            if (answerSent) return;
            String value = answer.getText().toString().trim();
            if (value.isEmpty()) {
                toast("اكتب إجابتك");
                return;
            }
            answerSent = true;
            send.setEnabled(false);
            skip.setEnabled(false);

            networkExecutor.execute(() -> {
                try {
                    submitAnswer(value);
                    runOnUiThread(() -> {
                        if (!isScreen(token, "answer")) return;
                        playSuccess();
                        showWaitingForAnswers();
                    });
                } catch (Exception e) {
                    answerSent = false;
                    runOnUiThread(() -> {
                        if (!isScreen(token, "answer")) return;
                        send.setEnabled(true);
                        skip.setEnabled(true);
                        toast("فشل إرسال الإجابة: " + safeMessage(e, "خطأ في الاتصال"));
                    });
                }
            });
        });

        skip.setOnClickListener(v -> {
            if (answerSent) return;
            answerSent = true;
            send.setEnabled(false);
            skip.setEnabled(false);

            networkExecutor.execute(() -> {
                try {
                    submitAnswer("SKIP");
                    runOnUiThread(() -> {
                        if (!isScreen(token, "answer")) return;
                        playClick();
                        showWaitingForAnswers();
                    });
                } catch (Exception e) {
                    answerSent = false;
                    runOnUiThread(() -> {
                        if (!isScreen(token, "answer")) return;
                        send.setEnabled(true);
                        skip.setEnabled(true);
                        toast("تعذر التخطي: " + safeMessage(e, "خطأ في الاتصال"));
                    });
                }
            });
        });

        Button chat = button("💬 Chat");
        chat.setOnClickListener(v -> showChat());
        root.addView(chat);

        setScreen(scroll(root), "answer");
        startAnswerTimer(timer, send, skip);
    }

    private void startAnswerTimer(TextView timer, Button send, Button skip) {
        answerTimer = new CountDownTimer(ANSWER_TIME * 1000L, 1000L) {
            @Override
            public void onTick(long left) {
                timer.setText("⏱️ " + (left / 1000L) + " ثانية");
            }

            @Override
            public void onFinish() {
                timer.setText("⏰ انتهى الوقت");
                playTimeUp();
                if (answerSent) return;

                answerSent = true;
                send.setEnabled(false);
                skip.setEnabled(false);

                networkExecutor.execute(() -> {
                    try {
                        submitAnswer("SKIP");
                    } catch (Exception ignored) {
                    }
                    runOnUiThread(() -> {
                        if ("answer".equals(currentScreen)) showWaitingForAnswers();
                    });
                });
            }
        }.start();
    }

    private void stopTimer() {
        if (answerTimer != null) {
            answerTimer.cancel();
            answerTimer = null;
        }
    }

    private void submitAnswer(String answer) throws Exception {
        JSONObject object = new JSONObject();
        object.put("room_code", roomCode);
        object.put("round", questionIndex);
        object.put("player_name", playerName);
        object.put("answer", answer);
        request("POST", api("/round_answers"), object.toString());
    }

    private JSONArray getRoundAnswers() throws Exception {
        return safeJsonArray(request(
                "GET",
                api("/round_answers?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&round=eq." + questionIndex + "&select=*"),
                null
        ));
    }

    // ============================================================
    // WAITING FOR ANSWERS
    // ============================================================

    private void showWaitingForAnswers() {
        stopTimer();
        int token = beginScreen("waiting_answers");
        LinearLayout root = root();
        root.addView(title("⏳ نستنّاو صاحبك"));
        root.addView(subtitle("تم إرسال إجابتك بنجاح.\nكي يكمل اللاعب الآخر، تبدأ مرحلة التوقع."));

        TextView counter = subtitle("الإجابات: 0 / 2");
        counter.setTextSize(19);
        counter.setTextColor(accentColor);
        root.addView(counter);

        Button chat = button("💬 Chat");
        chat.setOnClickListener(v -> showChat());
        root.addView(chat);

        setScreen(scroll(root), "waiting_answers");
        startAnswerPolling(counter, token);
    }

    private void startAnswerPolling(TextView counter, int token) {
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!isScreen(token, "waiting_answers")) return;
                if (!pollBusy.compareAndSet(false, true)) {
                    schedulePolling(this, 700L, token, "waiting_answers");
                    return;
                }

                networkExecutor.execute(() -> {
                    try {
                        JSONArray answers = getRoundAnswers();
                        JSONArray players = getPlayers();
                        JSONObject state = getGameState();
                        String status = state.optString("status", "answering");

                        runOnUiThread(() -> {
                            if (!isScreen(token, "waiting_answers")) return;
                            counter.setText("الإجابات: " + answers.length() + " / " + players.length());
                            if ("predicting".equals(status)) showPredictionScreen();
                        });

                        if (answers.length() >= 2 && players.length() == 2 && isHost &&
                                "answering".equals(status)) {
                            setGameState(questionIndex, "predicting");
                            runOnUiThread(() -> {
                                if (isScreen(token, "waiting_answers")) showPredictionScreen();
                            });
                        }
                    } catch (Exception ignored) {
                    } finally {
                        pollBusy.set(false);
                        schedulePolling(this, POLL_GAME, token, "waiting_answers");
                    }
                });
            }
        };
        schedulePolling(poll, 900L, token, "waiting_answers");
    }

    // ============================================================
    // PREDICTION
    // ============================================================

    private void showPredictionScreen() {
        stopTimer();
        int token = beginScreen("prediction");
        predictionSent = false;

        LinearLayout root = root();
        root.addView(title("🎯 وقت التوقع!"));
        root.addView(subtitle("اختار صاحبك وتوقّع بالضبط شنوّة كتب."));

        LinearLayout questionCard = card();
        TextView question = new TextView(this);
        question.setText(questions[questionIndex]);
        question.setTextColor(textColor);
        question.setTextSize(20);
        question.setGravity(Gravity.CENTER);
        question.setTypeface(null, Typeface.BOLD);
        question.setPadding(dp(5), dp(15), dp(5), dp(15));
        questionCard.addView(question);
        root.addView(questionCard);

        root.addView(label("اختار اللاعب:"));
        Spinner spinner = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(55));
        spinnerParams.setMargins(0, dp(5), 0, dp(10));
        root.addView(spinner, spinnerParams);

        root.addView(label("توقّع إجابته:"));
        EditText prediction = input("شنوّة تتوقع جاوب؟");
        prediction.setSingleLine(false);
        prediction.setMinHeight(dp(90));
        prediction.setGravity(Gravity.TOP | Gravity.RIGHT);
        prediction.setPadding(dp(15), dp(12), dp(15), dp(12));
        root.addView(prediction);

        Button submit = button("🎯 تأكيد التوقع");
        root.addView(submit);
        Button skip = button("⏭️ ما نحبش نتوقع");
        root.addView(skip);

        submit.setEnabled(false);
        skip.setEnabled(false);
        loadPredictionPlayers(spinner, submit, skip, token);

        submit.setOnClickListener(v -> submitPredictionFromUi(spinner, prediction, submit, skip, false, token));
        skip.setOnClickListener(v -> submitPredictionFromUi(spinner, prediction, submit, skip, true, token));

        Button chat = button("💬 Chat");
        chat.setOnClickListener(v -> showChat());
        root.addView(chat);

        setScreen(scroll(root), "prediction");
    }

    private void submitPredictionFromUi(Spinner spinner, EditText prediction,
                                         Button submit, Button skip, boolean skipPrediction, int token) {
        if (predictionSent) return;
        Object selected = spinner.getSelectedItem();
        if (selected == null) {
            toast("اختار لاعب");
            return;
        }

        String target = selected.toString();
        String predicted = skipPrediction ? "SKIP" : prediction.getText().toString().trim();
        if (!skipPrediction && predicted.isEmpty()) {
            toast("اكتب توقعك");
            return;
        }

        predictionSent = true;
        submit.setEnabled(false);
        skip.setEnabled(false);

        networkExecutor.execute(() -> {
            try {
                submitPrediction(target, predicted);
                runOnUiThread(() -> {
                    if (!isScreen(token, "prediction")) return;
                    showPredictionWaiting();
                });
            } catch (Exception e) {
                predictionSent = false;
                runOnUiThread(() -> {
                    if (!isScreen(token, "prediction")) return;
                    submit.setEnabled(true);
                    skip.setEnabled(true);
                    toast("فشل إرسال التوقع: " + safeMessage(e, "خطأ في الاتصال"));
                });
            }
        });
    }

    private void loadPredictionPlayers(Spinner spinner, Button submit, Button skip, int token) {
        networkExecutor.execute(() -> {
            try {
                JSONArray players = getPlayers();
                ArrayList<String> names = new ArrayList<>();

                for (int i = 0; i < players.length(); i++) {
                    JSONObject p = players.optJSONObject(i);
                    if (p == null) continue;
                    String name = p.optString("name", "");
                    if (!name.isEmpty() && !name.equals(playerName)) names.add(name);
                }

                runOnUiThread(() -> {
                    if (!isScreen(token, "prediction")) return;
                    if (names.isEmpty()) {
                        toast("ما فماش لاعب متاح");
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            this, android.R.layout.simple_spinner_item, names) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            TextView view = (TextView) super.getView(position, convertView, parent);
                            view.setTextColor(textColor);
                            view.setTextSize(16);
                            view.setPadding(dp(12), 0, dp(12), 0);
                            return view;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                            view.setTextColor(textColor);
                            view.setTextSize(16);
                            view.setPadding(dp(12), dp(12), dp(12), dp(12));
                            return view;
                        }
                    };
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                    submit.setEnabled(true);
                    skip.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isScreen(token, "prediction")) toast("تعذر تحميل اللاعبين");
                });
            }
        });
    }

    private void submitPrediction(String target, String predicted) throws Exception {
        String targetAnswer = getTargetAnswer(target);
        boolean correct = !"SKIP".equals(predicted) && similarAnswer(predicted, targetAnswer);
        int points = 0;

        if (correct) {
            points = 3;
            if (normalize(predicted).equals(normalize(targetAnswer))) points += 2;
        }

        JSONObject prediction = new JSONObject();
        prediction.put("room_code", roomCode);
        prediction.put("round", questionIndex);
        prediction.put("predictor", playerName);
        prediction.put("target", target);
        prediction.put("predicted_answer", predicted);
        prediction.put("correct", correct);
        prediction.put("points", points);
        request("POST", api("/predictions"), prediction.toString());

        if (points > 0) {
            score += points;
            updateMyScore();
            playSuccess();
        } else {
            playWrong();
        }
    }

    private String getTargetAnswer(String target) throws Exception {
        JSONArray result = safeJsonArray(request(
                "GET",
                api("/round_answers?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&round=eq." + questionIndex +
                        "&player_name=eq." + URLEncoder.encode(target, "UTF-8") +
                        "&select=answer&limit=1"),
                null
        ));
        if (result.length() == 0) throw new Exception("إجابة اللاعب غير موجودة");
        return result.getJSONObject(0).optString("answer", "");
    }

    // ============================================================
    // PREDICTION WAITING
    // ============================================================

    private void showPredictionWaiting() {
        int token = beginScreen("prediction_waiting");
        LinearLayout root = root();
        root.addView(title("⏳ تم إرسال التوقع"));
        root.addView(subtitle("نستنّاو اللاعب الآخر يكمل."));

        TextView counter = subtitle("التوقعات: 0 / 2");
        counter.setTextSize(19);
        counter.setTextColor(accentColor);
        root.addView(counter);

        Button chat = button("💬 Chat");
        chat.setOnClickListener(v -> showChat());
        root.addView(chat);

        setScreen(scroll(root), "prediction_waiting");
        startPredictionPolling(counter, token);
    }

    private void startPredictionPolling(TextView counter, int token) {
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!isScreen(token, "prediction_waiting")) return;
                if (!pollBusy.compareAndSet(false, true)) {
                    schedulePolling(this, 700L, token, "prediction_waiting");
                    return;
                }

                networkExecutor.execute(() -> {
                    try {
                        JSONArray predictions = getPredictions();
                        JSONArray players = getPlayers();
                        JSONObject state = getGameState();
                        String status = state.optString("status", "predicting");

                        runOnUiThread(() -> {
                            if (!isScreen(token, "prediction_waiting")) return;
                            counter.setText("التوقعات: " + predictions.length() + " / " + players.length());
                            if ("results".equals(status)) showResults();
                        });

                        if (predictions.length() >= 2 && players.length() == 2 && isHost &&
                                "predicting".equals(status)) {
                            setGameState(questionIndex, "results");
                            runOnUiThread(() -> {
                                if (isScreen(token, "prediction_waiting")) showResults();
                            });
                        }
                    } catch (Exception ignored) {
                    } finally {
                        pollBusy.set(false);
                        schedulePolling(this, POLL_GAME, token, "prediction_waiting");
                    }
                });
            }
        };
        schedulePolling(poll, 900L, token, "prediction_waiting");
    }

    private JSONArray getPredictions() throws Exception {
        return safeJsonArray(request(
                "GET",
                api("/predictions?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&round=eq." + questionIndex + "&select=*"),
                null
        ));
    }

    // ============================================================
    // RESULTS / LEADERBOARD
    // ============================================================

    private void showResults() {
        int token = beginScreen("results");
        LinearLayout root = root();
        applyBackground(root, "bg_results");

        root.addView(title("🏆 نتيجة الجولة"));

        LinearLayout scoreCard = card();
        TextView scoreText = new TextView(this);
        scoreText.setText("⭐ نقاطك\n" + score);
        scoreText.setTextColor(accentColor);
        scoreText.setTextSize(28);
        scoreText.setGravity(Gravity.CENTER);
        scoreText.setTypeface(null, Typeface.BOLD);
        scoreCard.addView(scoreText);
        root.addView(scoreCard);

        loadRoundResult(scoreCard, token);

        Button next = button("➡️ جاهز للجولة التالية");
        root.addView(next);
        next.setOnClickListener(v -> {
            if (nextReadySent) return;
            nextReadySent = true;
            next.setEnabled(false);
            networkExecutor.execute(() -> {
                try {
                    setPlayerReady(true);
                    runOnUiThread(() -> {
                        if (isScreen(token, "results")) waitForNextRound();
                    });
                } catch (Exception e) {
                    nextReadySent = false;
                    runOnUiThread(() -> {
                        if (!isScreen(token, "results")) return;
                        next.setEnabled(true);
                        toast("تعذر التسجيل: " + safeMessage(e, "خطأ في الاتصال"));
                    });
                }
            });
        });

        Button chat = button("💬 Chat");
        chat.setOnClickListener(v -> showChat());
        root.addView(chat);

        setScreen(scroll(root), "results");
        startResultsPolling(token, "results");
    }

    private void loadRoundResult(LinearLayout card, int token) {
        networkExecutor.execute(() -> {
            try {
                JSONArray data = safeJsonArray(request(
                        "GET",
                        api("/predictions?room_code=eq." +
                                URLEncoder.encode(roomCode, "UTF-8") +
                                "&round=eq." + questionIndex +
                                "&predictor=eq." + URLEncoder.encode(playerName, "UTF-8") +
                                "&select=*&limit=1"),
                        null
                ));
                if (data.length() == 0) return;

                JSONObject p = data.getJSONObject(0);
                boolean correct = p.optBoolean("correct", false);
                int points = p.optInt("points", 0);

                runOnUiThread(() -> {
                    if (!isScreen(token, "results")) return;
                    TextView result = new TextView(this);
                    result.setText(correct ? "🎯 توقّع صحيح!\n+" + points + " نقاط"
                            : "❌ التوقع ما صابش\n0 نقاط");
                    result.setTextColor(textColor);
                    result.setTextSize(18);
                    result.setGravity(Gravity.CENTER);
                    result.setPadding(0, dp(15), 0, dp(10));
                    card.addView(result);
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void waitForNextRound() {
        int token = beginScreen("waiting_next");
        LinearLayout root = root();
        root.addView(title("⏳ جاهز"));
        root.addView(subtitle("نستنّاو اللاعب الآخر...") );
        setScreen(scroll(root), "waiting_next");
        startResultsPolling(token, "waiting_next");
    }

    private void startResultsPolling(int token, String screenName) {
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!("results".equals(currentScreen) || "waiting_next".equals(currentScreen)) || token != screenToken) return;
                if (!pollBusy.compareAndSet(false, true)) {
                    schedulePolling(this, 700L, token, screenName);
                    return;
                }

                networkExecutor.execute(() -> {
                    try {
                        JSONObject state = getGameState();
                        String status = state.optString("status", "results");
                        int index = state.optInt("question_index", questionIndex);
                        JSONArray players = getPlayers();

                        boolean allReady = players.length() == 2;
                        for (int i = 0; i < players.length(); i++) {
                            if (!players.getJSONObject(i).optBoolean("ready", false)) {
                                allReady = false;
                                break;
                            }
                        }

                        if ("answering".equals(status)) {
                            questionIndex = Math.max(0, Math.min(index, questions.length - 1));
                            runOnUiThread(() -> {
                                if (token == screenToken && ("results".equals(currentScreen) || "waiting_next".equals(currentScreen))) {
                                    showAnswerScreen();
                                }
                            });
                            return;
                        }

                        if ("finished".equals(status)) {
                            runOnUiThread(() -> {
                                if (token == screenToken && ("results".equals(currentScreen) || "waiting_next".equals(currentScreen))) {
                                    showLeaderboard();
                                }
                            });
                            return;
                        }

                        if (allReady && isHost && "results".equals(status)) {
                            advanceRound();
                            return;
                        }
                    } catch (Exception ignored) {
                    } finally {
                        pollBusy.set(false);
                        if (token == screenToken && ("results".equals(currentScreen) || "waiting_next".equals(currentScreen))) {
                            schedulePolling(this, POLL_GAME, token, screenName);
                        }
                    }
                });
            }
        };
        schedulePolling(poll, 1000L, token, screenName);
    }

    private void advanceRound() {
        networkExecutor.execute(() -> {
            try {
                int next = questionIndex + 1;
                setAllReady(false);

                if (next >= questions.length) {
                    setGameState(questionIndex, "finished");
                    setRoomStatus("waiting");
                    runOnUiThread(this::showLeaderboard);
                    return;
                }

                questionIndex = next;
                setGameState(questionIndex, "answering");
                runOnUiThread(this::showAnswerScreen);
            } catch (Exception e) {
                runOnUiThread(() -> toast("تعذر الانتقال للجولة التالية: " + safeMessage(e, "خطأ")));
            }
        });
    }

    private void showLeaderboard() {
        stopTimer();
        int token = beginScreen("leaderboard");
        LinearLayout root = root();
        root.addView(title("🏆 Leaderboard"));
        root.addView(subtitle("النتائج النهائية"));

        LinearLayout scoreCard = card();
        root.addView(scoreCard);

        networkExecutor.execute(() -> {
            try {
                JSONArray players = getPlayers();
                List<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < players.length(); i++) list.add(players.getJSONObject(i));
                list.sort((a, b) -> Integer.compare(b.optInt("score", 0), a.optInt("score", 0)));

                runOnUiThread(() -> {
                    if (!isScreen(token, "leaderboard")) return;
                    scoreCard.removeAllViews();
                    for (int i = 0; i < list.size(); i++) {
                        JSONObject p = list.get(i);
                        TextView row = new TextView(this);
                        String medal = i == 0 ? "🥇" : (i == 1 ? "🥈" : "🏅");
                        row.setText(medal + "  " + (i + 1) + ". " + p.optString("name", "?") +
                                "\n      ⭐ " + p.optInt("score", 0) + " نقطة");
                        row.setTextColor(textColor);
                        row.setTextSize(19);
                        row.setPadding(0, dp(14), 0, dp(14));
                        scoreCard.addView(row);
                    }

                    if (list.size() == 2) {
                        int a = list.get(0).optInt("score", 0);
                        int b = list.get(1).optInt("score", 0);
                        TextView winner = new TextView(this);
                        winner.setText(a == b ? "🤝 تعادل!" : "👑 الفائز: " + list.get(0).optString("name", "?"));
                        winner.setTextColor(accentColor);
                        winner.setTextSize(22);
                        winner.setGravity(Gravity.CENTER);
                        winner.setTypeface(null, Typeface.BOLD);
                        winner.setPadding(0, dp(15), 0, dp(5));
                        scoreCard.addView(winner);
                    }
                });
            } catch (Exception ignored) {
            }
        });

        Button playAgain = button("🔄 Play Again");
        root.addView(playAgain);
        playAgain.setOnClickListener(v -> {
            if (!isHost) {
                toast("الـHost هو اللي يبدأ من جديد");
                return;
            }
            playAgain.setEnabled(false);
            networkExecutor.execute(() -> {
                try {
                    resetScores();
                    setAllReady(false);
                    questionIndex = 0;
                    setRoomStatus("playing");
                    setGameState(0, "answering");
                    runOnUiThread(this::showAnswerScreen);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        playAgain.setEnabled(true);
                        toast("تعذر إعادة اللعبة: " + safeMessage(e, "خطأ"));
                    });
                }
            });
        });

        Button lobby = button("🏠 العودة للـLobby");
        lobby.setOnClickListener(v -> showLobby());
        root.addView(lobby);

        Button home = button("🏠 الصفحة الرئيسية");
        home.setOnClickListener(v -> showHome());
        root.addView(home);

        setScreen(scroll(root), "leaderboard");
    }

    // ============================================================
    // SCORES / READY / LEAVE
    // ============================================================

    private void resetScores() throws Exception {
        JSONArray players = getPlayers();
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.getJSONObject(i);
            String id = p.optString("player_id", "");
            if (id.isEmpty()) continue;

            JSONObject update = new JSONObject();
            update.put("score", 0);
            update.put("ready", false);
            request("PATCH", api("/players?player_id=eq." +
                    URLEncoder.encode(id, "UTF-8")), update.toString());
        }
        score = 0;
    }

    private void updateMyScore() throws Exception {
        JSONObject update = new JSONObject();
        update.put("score", score);
        request("PATCH", api("/players?player_id=eq." +
                URLEncoder.encode(playerId, "UTF-8")), update.toString());
    }

    private void setPlayerReady(boolean ready) throws Exception {
        JSONObject update = new JSONObject();
        update.put("ready", ready);
        request("PATCH", api("/players?player_id=eq." +
                URLEncoder.encode(playerId, "UTF-8")), update.toString());
    }

    private void setAllReady(boolean ready) throws Exception {
        JSONObject update = new JSONObject();
        update.put("ready", ready);
        request("PATCH", api("/players?room_id=eq." +
                URLEncoder.encode(roomId, "UTF-8")), update.toString());
    }

    private void leaveRoom() {
        stopAllPolling();
        networkExecutor.execute(() -> {
            try {
                if (isHost) {
                    JSONArray players = getPlayers();
                    for (int i = 0; i < players.length(); i++) {
                        JSONObject p = players.getJSONObject(i);
                        String id = p.optString("player_id", "");
                        if (!id.equals(playerId) && !id.isEmpty()) {
                            JSONObject update = new JSONObject();
                            update.put("is_host", true);
                            request("PATCH", api("/players?player_id=eq." +
                                    URLEncoder.encode(id, "UTF-8")), update.toString());
                            break;
                        }
                    }
                }

                if (!playerId.isEmpty()) {
                    request("DELETE", api("/players?player_id=eq." +
                            URLEncoder.encode(playerId, "UTF-8")), null);
                }

                runOnUiThread(() -> {
                    clearRoomState();
                    showHome();
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("تعذر مغادرة الغرفة: " + safeMessage(e, "خطأ")));
            }
        });
    }

    private void clearRoomState() {
        roomId = "";
        roomCode = "";
        playerName = "";
        isHost = false;
        score = 0;
        questionIndex = 0;
        answerSent = false;
        predictionSent = false;
        nextReadySent = false;
    }

    // ============================================================
    // GAME STATE / ROOM STATUS
    // ============================================================

    private JSONObject getGameState() throws Exception {
        JSONArray data = safeJsonArray(request(
                "GET",
                api("/game_state?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&select=*&limit=1"),
                null
        ));
        if (data.length() == 0) throw new Exception("game state not found");
        return data.getJSONObject(0);
    }

    private void setGameState(int index, String status) throws Exception {
        JSONObject update = new JSONObject();
        update.put("question_index", index);
        update.put("status", status);
        update.put("updated_at", getCurrentTimestamp());
        request("PATCH", api("/game_state?room_code=eq." +
                URLEncoder.encode(roomCode, "UTF-8")), update.toString());
    }

    private void setRoomStatus(String status) throws Exception {
        JSONObject update = new JSONObject();
        update.put("status", status);
        request("PATCH", api("/rooms?id=eq." +
                URLEncoder.encode(roomId, "UTF-8")), update.toString());
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                .format(new Date());
    }

    // ============================================================
    // CHAT
    // ============================================================

    private void showChat() {
        int token = beginScreen("chat");
        LinearLayout root = root();
        root.addView(title("💬 Chat"));
        root.addView(subtitle("احكوا مع بعض أثناء اللعب."));

        LinearLayout messages = card();
        root.addView(messages);

        EditText message = input("اكتب رسالة...");
        message.setSingleLine(false);
        message.setMinHeight(dp(65));
        message.setGravity(Gravity.TOP | Gravity.RIGHT);
        message.setPadding(dp(15), dp(10), dp(15), dp(10));
        root.addView(message);

        Button send = button("📤 إرسال");
        root.addView(send);
        send.setOnClickListener(v -> {
            String text = message.getText().toString().trim();
            if (text.isEmpty()) return;
            setBusy(send, true, "⏳ ...", "📤 إرسال");

            networkExecutor.execute(() -> {
                try {
                    JSONObject object = new JSONObject();
                    object.put("room_id", roomId);
                    object.put("player_name", playerName);
                    object.put("message", text);
                    request("POST", api("/messages"), object.toString());
                    runOnUiThread(() -> {
                        if (!isScreen(token, "chat")) return;
                        message.setText("");
                        setBusy(send, false, "⏳ ...", "📤 إرسال");
                        playSuccess();
                        loadMessages(messages, token);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (!isScreen(token, "chat")) return;
                        setBusy(send, false, "⏳ ...", "📤 إرسال");
                        toast("فشل إرسال الرسالة: " + safeMessage(e, "خطأ في الاتصال"));
                    });
                }
            });
        });

        Button back = button("رجوع");
        back.setOnClickListener(v -> showLobby());
        root.addView(back);

        setScreen(scroll(root), "chat");
        loadMessages(messages, token);
        startChatPolling(messages, token);
    }

    private void loadMessages(LinearLayout container, int token) {
        networkExecutor.execute(() -> {
            try {
                JSONArray data = safeJsonArray(request(
                        "GET",
                        api("/messages?room_id=eq." +
                                URLEncoder.encode(roomId, "UTF-8") +
                                "&select=*&order=created_at.desc&limit=50"),
                        null
                ));

                runOnUiThread(() -> {
                    if (!isScreen(token, "chat")) return;
                    renderMessages(container, data);
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void renderMessages(LinearLayout container, JSONArray data) {
        container.removeAllViews();
        if (data.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("ما فما حتى رسالة 👀");
            empty.setTextColor(secondaryTextColor);
            empty.setGravity(Gravity.CENTER);
            container.addView(empty);
            return;
        }

        for (int i = data.length() - 1; i >= 0; i--) {
            JSONObject m = data.optJSONObject(i);
            if (m == null) continue;
            String name = m.optString("player_name", "?");
            String text = m.optString("message", "");

            TextView row = new TextView(this);
            row.setText("👤 " + name + "\n   " + text);
            row.setTextColor(textColor);
            row.setTextSize(15);
            row.setPadding(0, dp(8), 0, dp(8));
            container.addView(row);
        }
    }

    private void startChatPolling(LinearLayout messages, int token) {
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (!isScreen(token, "chat")) return;
                loadMessages(messages, token);
                schedulePolling(this, POLL_CHAT, token, "chat");
            }
        };
        schedulePolling(poll, POLL_CHAT, token, "chat");
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    private void showSettings() {
        stopTimer();
        int token = beginScreen("settings");

        LinearLayout root = root();
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        TextView pageTitle = title("⚙️ الإعدادات");
        pageTitle.setTextSize(30);
        pageTitle.setTextColor(textColor);
        root.addView(pageTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)
        ));

        TextView pageSub = subtitle("تحكم في المظهر والموسيقى والمؤثرات");
        pageSub.setTextColor(secondaryTextColor);
        pageSub.setTextSize(15);
        root.addView(pageSub);

        // Appearance
        LinearLayout appearance = card();
        appearance.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        appearance.setGravity(Gravity.RIGHT);

        TextView appearanceTitle = label("🎨 المظهر");
        appearanceTitle.setTextSize(19);
        appearanceTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        appearance.addView(appearanceTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)
        ));

        TextView appearanceStatus = new TextView(this);
        appearanceStatus.setText(darkMode ? "🌙 الوضع الداكن مفعل" : "☀️ الوضع الفاتح مفعل");
        appearanceStatus.setTextColor(textColor);
        appearanceStatus.setTextSize(17);
        appearanceStatus.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        appearanceStatus.setPadding(0, dp(3), 0, dp(5));
        appearance.addView(appearanceStatus);
        root.addView(appearance);

        Button dark = button(darkMode ? "☀️ الوضع الفاتح" : "🌙 الوضع الداكن");
        dark.setOnClickListener(v -> toggleDarkMode());
        root.addView(dark);

        // Audio
        LinearLayout audio = card();
        audio.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        audio.setGravity(Gravity.RIGHT);

        TextView audioTitle = label("🎵 الصوت");
        audioTitle.setTextSize(19);
        audioTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        audio.addView(audioTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)
        ));

        TextView musicStatus = new TextView(this);
        musicStatus.setText(musicEnabled ? "🎵 الموسيقى مفعلة" : "🔇 الموسيقى متوقفة");
        musicStatus.setTextColor(textColor);
        musicStatus.setTextSize(16);
        musicStatus.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        musicStatus.setPadding(0, dp(3), 0, dp(3));
        audio.addView(musicStatus);

        TextView soundStatus = new TextView(this);
        soundStatus.setText(soundEnabled ? "🔊 المؤثرات مفعلة" : "🔇 المؤثرات متوقفة");
        soundStatus.setTextColor(textColor);
        soundStatus.setTextSize(16);
        soundStatus.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        soundStatus.setPadding(0, dp(3), 0, 0);
        audio.addView(soundStatus);
        root.addView(audio);

        Button music = button(musicEnabled ? "🔇 إيقاف الموسيقى" : "🎵 تشغيل الموسيقى");
        music.setOnClickListener(v -> toggleMusic());
        root.addView(music);

        Button sound = button(soundEnabled ? "🔇 إيقاف المؤثرات" : "🔊 تشغيل المؤثرات");
        sound.setOnClickListener(v -> toggleSound());
        root.addView(sound);

        // About
        LinearLayout about = card();
        about.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        about.setGravity(Gravity.RIGHT);

        TextView aboutTitle = label("🎮 GuessUs");
        aboutTitle.setTextSize(20);
        aboutTitle.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        about.addView(aboutTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)
        ));

        TextView aboutText = new TextView(this);
        aboutText.setText("لعبة توقّع إجابات صاحبك.\n\n" +
                "👥 لاعبان\n" +
                "🎯 توقّعات\n" +
                "🏆 نقاط\n" +
                "💬 Chat\n" +
                "🎵 موسيقى\n\n" +
                "الإصدار 1.0");
        aboutText.setTextColor(textColor);
        aboutText.setTextSize(16);
        aboutText.setGravity(Gravity.RIGHT);
        aboutText.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        aboutText.setLineSpacing(dp(2), 1.05f);
        about.addView(aboutText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(about);

        Button back = button("رجوع");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        setScreen(scroll(root), "settings");
    }

    // ============================================================
    // ANSWER COMPARISON
    // ============================================================

    private String normalize(String value) {
        if (value == null) return "";
        String s = value.trim().toLowerCase(Locale.ROOT);
        s = s.replace("أ", "ا");
        s = s.replace("إ", "ا");
        s = s.replace("آ", "ا");
        s = s.replace("ة", "ه");
        s = s.replace("ى", "ي");
        s = s.replace("ؤ", "و");
        s = s.replace("ئ", "ي");
        s = s.replaceAll("[ًٌٍَُِّْـ]", "");
        s = s.replaceAll("[^\\p{L}\\p{N}\\s]", "");
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }

    private boolean similarAnswer(String a, String b) {
        if (a == null || b == null) return false;
        String x = normalize(a);
        String y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;

        if (x.length() >= 3 && y.length() >= 3 && (x.contains(y) || y.contains(x))) return true;

        String[] wordsX = x.split(" ");
        String[] wordsY = y.split(" ");
        int matches = 0;
        for (String wx : wordsX) {
            for (String wy : wordsY) {
                if (wx.equals(wy) && wx.length() >= 3) matches++;
            }
        }
        return matches > 0;
    }

    // ============================================================
    // SUPABASE HTTP
    // ============================================================

    private void validateSupabase() throws Exception {
        if (SUPABASE_URL == null || SUPABASE_URL.trim().isEmpty() ||
                SUPABASE_URL.contains("${") || SUPABASE_URL.contains("null")) {
            throw new Exception("SUPABASE_URL غير مضبوط في BuildConfig");
        }
        if (SUPABASE_KEY == null || SUPABASE_KEY.trim().isEmpty() ||
                SUPABASE_KEY.contains("${") || SUPABASE_KEY.contains("null")) {
            throw new Exception("SUPABASE_KEY غير مضبوط في BuildConfig");
        }
    }

    /** Builds a Supabase REST URL and prevents /rest/v1/rest/v1/... */
    private String api(String path) throws Exception {
        validateSupabase();
        String base = SUPABASE_URL.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String suffix = "/rest/v1";
        if (base.length() >= suffix.length() &&
                base.regionMatches(true, base.length() - suffix.length(), suffix, 0, suffix.length())) {
            base = base.substring(0, base.length() - suffix.length());
            while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        }
        if (path == null || path.trim().isEmpty()) throw new Exception("مسار Supabase فارغ");
        String clean = path.trim();
        if (!clean.startsWith("/")) clean = "/" + clean;
        return base + "/rest/v1" + clean;
    }

    private String request(String method, String urlString, String body) throws Exception {
        validateSupabase();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(9000);
            connection.setReadTimeout(12000);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            connection.setRequestProperty("apikey", SUPABASE_KEY);
            connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Prefer",
                    "POST".equalsIgnoreCase(method) ? "return=representation" : "return=minimal");

            if (body != null && !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                connection.setDoOutput(true);
                byte[] data = body.getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                }
            }

            int responseCode = connection.getResponseCode();
            InputStream input = responseCode >= 200 && responseCode < 400
                    ? connection.getInputStream() : connection.getErrorStream();

            String responseBody = "";
            if (input != null) {
                StringBuilder result = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                }
                responseBody = result.toString();
            }

            if (responseCode < 200 || responseCode >= 300) {
                String clean = responseBody;
                if (clean.length() > 450) clean = clean.substring(0, 450);
                throw new Exception("HTTP " + responseCode +
                        (clean.isEmpty() ? "" : ": " + clean));
            }

            return responseBody;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private JSONArray safeJsonArray(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) return new JSONArray();
        String trimmed = value.trim();
        if (trimmed.startsWith("[")) return new JSONArray(trimmed);
        JSONArray array = new JSONArray();
        if (trimmed.startsWith("{")) array.put(new JSONObject(trimmed));
        return array;
    }
}
