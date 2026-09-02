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
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    // ============================================================
    // SUPABASE
    // ============================================================

    private final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;

    // ============================================================
    // GAME SETTINGS
    // ============================================================

    private static final int MAX_PLAYERS = 2;
    private static final int ANSWER_TIME = 45;

    // ============================================================
    // PLAYER / ROOM
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

    // ============================================================
    // SETTINGS
    // ============================================================

    private SharedPreferences prefs;

    private boolean darkMode;
    private boolean musicEnabled;
    private boolean soundEnabled;

    // ============================================================
    // COLORS
    // ============================================================

    private int bgColor;
    private int cardColor;
    private int textColor;
    private int secondaryTextColor;
    private int accentColor;
    private int buttonTextColor;

    // ============================================================
    // MEDIA
    // ============================================================

    private MediaPlayer musicPlayer;
    private ToneGenerator toneGenerator;

    // ============================================================
    // TIMERS / HANDLERS
    // ============================================================

    private CountDownTimer answerTimer;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final AtomicBoolean pollingBusy =
            new AtomicBoolean(false);

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
    // CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "guessus_settings",
                MODE_PRIVATE
        );

        darkMode = prefs.getBoolean(
                "dark_mode",
                false
        );

        musicEnabled = prefs.getBoolean(
                "music_enabled",
                true
        );

        soundEnabled = prefs.getBoolean(
                "sound_enabled",
                true
        );

        setupTheme();

        playerId = prefs.getString(
                "player_id",
                ""
        );

        if (playerId.isEmpty()) {

            playerId =
                    UUID.randomUUID().toString();

            prefs.edit()
                    .putString(
                            "player_id",
                            playerId
                    )
                    .apply();
        }

        initSound();

        showHome();

        if (musicEnabled) {
            startMusic();
        }
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    @Override
    protected void onResume() {
        super.onResume();

        if (musicEnabled &&
                currentScreen != null &&
                !currentScreen.equals("chat")) {

            startMusic();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        pauseMusic();
    }

    @Override
    protected void onDestroy() {

        stopTimer();

        handler.removeCallbacksAndMessages(null);

        releaseMusic();

        if (toneGenerator != null) {

            toneGenerator.release();

            toneGenerator = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if ("home".equals(currentScreen)) {

            super.onBackPressed();

            return;
        }

        if ("lobby".equals(currentScreen) ||
                "answer".equals(currentScreen) ||
                "waiting_answers".equals(currentScreen) ||
                "prediction".equals(currentScreen) ||
                "prediction_waiting".equals(currentScreen) ||
                "results".equals(currentScreen) ||
                "waiting_next".equals(currentScreen) ||
                "leaderboard".equals(currentScreen) ||
                "chat".equals(currentScreen)) {

            if (!roomCode.isEmpty()) {

                showLobby();

            } else {

                showHome();
            }

            return;
        }

        showHome();
    }

    // ============================================================
    // THEME
    // ============================================================

    private void setupTheme() {

        if (darkMode) {

            bgColor = Color.rgb(
                    13,
                    10,
                    25
            );

            cardColor = Color.rgb(
                    30,
                    24,
                    48
            );

            textColor = Color.WHITE;

            secondaryTextColor =
                    Color.rgb(
                            195,
                            187,
                            215
                    );

            accentColor =
                    Color.rgb(
                            185,
                            90,
                            255
                    );

            buttonTextColor =
                    Color.WHITE;

        } else {

            bgColor =
                    Color.rgb(
                            245,
                            240,
                            255
                    );

            cardColor =
                    Color.WHITE;

            textColor =
                    Color.rgb(
                            35,
                            25,
                            50
                    );

            secondaryTextColor =
                    Color.rgb(
                            105,
                            92,
                            125
                    );

            accentColor =
                    Color.rgb(
                            130,
                            55,
                            210
                    );

            buttonTextColor =
                    Color.WHITE;
        }
    }

    private void toggleDarkMode() {

        darkMode = !darkMode;

        prefs.edit()
                .putBoolean(
                        "dark_mode",
                        darkMode
                )
                .apply();

        setupTheme();

        playClick();

        showSettings();
    }

    private void toggleMusic() {

        musicEnabled = !musicEnabled;

        prefs.edit()
                .putBoolean(
                        "music_enabled",
                        musicEnabled
                )
                .apply();

        if (musicEnabled) {

            startMusic();

        } else {

            stopMusic();
        }

        playClick();

        showSettings();
    }

    private void toggleSound() {

        soundEnabled = !soundEnabled;

        prefs.edit()
                .putBoolean(
                        "sound_enabled",
                        soundEnabled
                )
                .apply();

        if (soundEnabled) {
            playClick();
        }

        showSettings();
    }

    // ============================================================
    // MUSIC
    // ============================================================

    private void initSound() {

        try {

            toneGenerator =
                    new ToneGenerator(
                            AudioManager.STREAM_MUSIC,
                            70
                    );

        } catch (Exception ignored) {
        }
    }

    private void startMusic() {

        if (!musicEnabled) {
            return;
        }

        try {

            if (musicPlayer == null) {

                int resourceId =
                        getResources()
                                .getIdentifier(
                                        "bg_music",
                                        "raw",
                                        getPackageName()
                                );

                if (resourceId == 0) {
                    return;
                }

                musicPlayer =
                        MediaPlayer.create(
                                this,
                                resourceId
                        );

                if (musicPlayer == null) {
                    return;
                }

                musicPlayer.setLooping(true);

                musicPlayer.setVolume(
                        0.35f,
                        0.35f
                );
            }

            if (!musicPlayer.isPlaying()) {

                musicPlayer.start();
            }

        } catch (Exception ignored) {
        }
    }

    private void pauseMusic() {

        try {

            if (musicPlayer != null &&
                    musicPlayer.isPlaying()) {

                musicPlayer.pause();
            }

        } catch (Exception ignored) {
        }
    }

    private void stopMusic() {

        try {

            if (musicPlayer != null) {

                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                }

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

    private void playClick() {

        if (!soundEnabled ||
                toneGenerator == null) {
            return;
        }

        try {

            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    70
            );

        } catch (Exception ignored) {
        }
    }

    private void playSuccess() {

        if (!soundEnabled ||
                toneGenerator == null) {
            return;
        }

        try {

            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_ACK,
                    120
            );

        } catch (Exception ignored) {
        }
    }

    private void playWrong() {

        if (!soundEnabled ||
                toneGenerator == null) {
            return;
        }

        try {

            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_NACK,
                    150
            );

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // BASIC UI
    // ============================================================

    private int dp(int value) {

        return (int)
                (
                        value *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                                + 0.5f
                );
    }

    private LinearLayout root() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        layout.setBackgroundColor(
                bgColor
        );

        return layout;
    }

    private ScrollView scroll(View child) {

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        scroll.setClipToPadding(false);

        scroll.addView(child);

        return scroll;
    }

    private TextView title(String text) {

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextColor(
                textColor
        );

        tv.setTextSize(28);

        tv.setGravity(
                Gravity.CENTER
        );

        tv.setTypeface(
                null,
                Typeface.BOLD
        );

        tv.setPadding(
                0,
                dp(15),
                0,
                dp(15)
        );

        return tv;
    }

    private TextView subtitle(String text) {

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextColor(
                secondaryTextColor
        );

        tv.setTextSize(15);

        tv.setGravity(
                Gravity.CENTER
        );

        tv.setPadding(
                dp(8),
                dp(5),
                dp(8),
                dp(12)
        );

        return tv;
    }

    private TextView label(String text) {

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextColor(
                textColor
        );

        tv.setTextSize(16);

        tv.setTypeface(
                null,
                Typeface.BOLD
        );

        tv.setPadding(
                dp(4),
                dp(8),
                dp(4),
                dp(8)
        );

        return tv;
    }

    private LinearLayout card() {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(16)
        );

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                cardColor
        );

        drawable.setCornerRadius(
                dp(22)
        );

        card.setBackground(
                drawable
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                dp(8),
                0,
                dp(8)
        );

        card.setLayoutParams(params);

        return card;
    }

    private Button button(String text) {

        Button b =
                new Button(this);

        b.setText(text);

        b.setTextSize(16);

        b.setTextColor(
                buttonTextColor
        );

        b.setAllCaps(false);

        b.setGravity(
                Gravity.CENTER
        );

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                accentColor
        );

        drawable.setCornerRadius(
                dp(16)
        );

        b.setBackground(
                drawable
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(58)
                );

        params.setMargins(
                0,
                dp(7),
                0,
                dp(7)
        );

        b.setLayoutParams(params);

        b.setOnClickListener(
                v -> playClick()
        );

        return b;
    }

    private EditText input(String hint) {

        EditText e =
                new EditText(this);

        e.setHint(hint);

        e.setHintTextColor(
                secondaryTextColor
        );

        e.setTextColor(
                textColor
        );

        e.setTextSize(16);

        e.setSingleLine(true);

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                darkMode
                        ? Color.rgb(
                                42,
                                35,
                                62
                        )
                        : Color.rgb(
                                242,
                                238,
                                249
                        )
        );

        drawable.setCornerRadius(
                dp(15)
        );

        e.setBackground(
                drawable
        );

        e.setPadding(
                dp(15),
                0,
                dp(15),
                0
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        params.setMargins(
                0,
                dp(7),
                0,
                dp(7)
        );

        e.setLayoutParams(params);

        return e;
    }

    private void animateIn(View view) {

        AlphaAnimation animation =
                new AlphaAnimation(
                        0.0f,
                        1.0f
                );

        animation.setDuration(350);

        view.startAnimation(animation);
    }

    private void toast(String text) {

        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void setScreen(
            View view,
            String name
    ) {

        currentScreen = name;

        setContentView(view);

        animateIn(view);
    }

    // ============================================================
    // HOME
    // ============================================================

    private void showHome() {

        stopTimer();

        LinearLayout root =
                root();

        try {

            root.setBackgroundResource(
                    R.drawable.bg_home
            );

        } catch (Exception ignored) {
        }

        TextView logo =
                title("GuessUs");

        logo.setTextColor(
                Color.WHITE
        );

        logo.setTextSize(44);

        logo.setTypeface(
                null,
                Typeface.BOLD
        );

        root.addView(
                logo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(170)
                )
        );

        TextView info =
                subtitle(
                        "توقّع إجابات صاحبك وشوف شكون يعرف شكون أكثر 🎯"
                );

        info.setTextColor(
                Color.WHITE
        );

        info.setTextSize(16);

        root.addView(info);

        Button create =
                button("🎮 إنشاء غرفة");

        create.setOnClickListener(
                v -> showCreateRoom()
        );

        root.addView(create);

        Button join =
                button("🚪 الانضمام لغرفة");

        join.setOnClickListener(
                v -> showJoinRoom()
        );

        root.addView(join);

        Button settings =
                button("⚙️ الإعدادات");

        settings.setOnClickListener(
                v -> showSettings()
        );

        root.addView(settings);

        TextView features =
                subtitle(
                        "👥 لاعبان • 🎯 توقع • 💬 Chat • 🏆 نقاط • 🎵 موسيقى"
                );

        features.setTextColor(
                Color.WHITE
        );

        features.setTextSize(14);

        root.addView(features);

        TextView version =
                subtitle(
                        "GuessUs • Online Party Game"
                );

        version.setTextColor(
                Color.WHITE
        );

        root.addView(version);

        setScreen(
                scroll(root),
                "home"
        );

        if (musicEnabled) {
            startMusic();
        }
    }

    // ============================================================
    // CREATE ROOM
    // ============================================================

    private void showCreateRoom() {

        LinearLayout root =
                root();

        root.addView(
                title("إنشاء غرفة 🎮")
        );

        root.addView(
                subtitle(
                        "أنت الـHost. ادخل اسمك ثم شارك الكود مع صاحبك."
                )
        );

        EditText name =
                input("اسمك");

        name.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(18)
                }
        );

        root.addView(name);

        Button create =
                button("✨ إنشاء الغرفة");

        create.setOnClickListener(v -> {

            String n =
                    name.getText()
                            .toString()
                            .trim();

            if (n.isEmpty()) {

                toast("اكتب اسمك أولاً");

                return;
            }

            playerName = n;

            create.setEnabled(false);

            new Thread(
                    () -> createRoom(create)
            ).start();
        });

        root.addView(create);

        Button back =
                button("رجوع");

        back.setOnClickListener(
                v -> showHome()
        );

        root.addView(back);

        setScreen(
                scroll(root),
                "create"
        );
    }

    private void createRoom(
            Button source
    ) {

        try {

            String code =
                    generateRoomCode();

            JSONObject room =
                    new JSONObject();

            room.put(
                    "code",
                    code
            );

            room.put(
                    "status",
                    "waiting"
            );

            String result =
                    request(
                            "POST",
                            SUPABASE_URL +
                                    "/rest/v1/rooms",
                            room.toString()
                    );

            JSONArray rooms =
                    new JSONArray(result);

            if (rooms.length() == 0) {

                throw new Exception(
                        "room creation failed"
                );
            }

            JSONObject created =
                    rooms.getJSONObject(0);

            roomId =
                    created.getString("id");

            roomCode =
                    created.getString("code");

            isHost = true;

            score = 0;

            questionIndex = 0;

            createGameState();

            addPlayer();

            runOnUiThread(
                    () -> showLobby()
            );

        } catch (Exception e) {

            runOnUiThread(() -> {

                source.setEnabled(true);

                toast(
                        "فشل إنشاء الغرفة"
                );
            });
        }
    }

    private String generateRoomCode() {

        return String.valueOf(
                1000 +
                        (int)
                                (
                                        Math.random()
                                                * 9000
                                )
        );
    }

    private void createGameState()
            throws Exception {

        JSONObject state =
                new JSONObject();

        state.put(
                "room_code",
                roomCode
        );

        state.put(
                "question_index",
                0
        );

        state.put(
                "status",
                "waiting"
        );

        request(
                "POST",
                SUPABASE_URL +
                        "/rest/v1/game_state",
                state.toString()
        );
    }

    // ============================================================
    // JOIN
    // ============================================================

    private void showJoinRoom() {

        LinearLayout root =
                root();

        root.addView(
                title("الانضمام 🚪")
        );

        root.addView(
                subtitle(
                        "ادخل اسمك وكود الغرفة المكوّن من 4 أرقام."
                )
        );

        EditText name =
                input("اسمك");

        name.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(18)
                }
        );

        root.addView(name);

        EditText code =
                input("كود الغرفة - 4 أرقام");

        code.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        code.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(4)
                }
        );

        root.addView(code);

        Button join =
                button("🚀 انضمام");

        join.setOnClickListener(v -> {

            String n =
                    name.getText()
                            .toString()
                            .trim();

            String c =
                    code.getText()
                            .toString()
                            .trim();

            if (n.isEmpty()) {

                toast("اكتب اسمك");

                return;
            }

            if (c.length() != 4) {

                toast(
                        "الكود لازم يكون 4 أرقام"
                );

                return;
            }

            playerName = n;

            join.setEnabled(false);

            new Thread(
                    () -> joinRoom(c, join)
            ).start();
        });

        root.addView(join);

        Button back =
                button("رجوع");

        back.setOnClickListener(
                v -> showHome()
        );

        root.addView(back);

        setScreen(
                scroll(root),
                "join"
        );
    }

    private void joinRoom(
            String code,
            Button source
    ) {

        try {

            String encoded =
                    URLEncoder.encode(
                            code,
                            "UTF-8"
                    );

            String result =
                    request(
                            "GET",
                            SUPABASE_URL +
                                    "/rest/v1/rooms?code=eq." +
                                    encoded +
                                    "&select=*",
                            null
                    );

            JSONArray rooms =
                    new JSONArray(result);

            if (rooms.length() == 0) {

                throw new Exception(
                        "الغرفة غير موجودة"
                );
            }

            JSONObject room =
                    rooms.getJSONObject(0);

            String status =
                    room.optString(
                            "status",
                            "waiting"
                    );

            if (!status.equals(
                    "waiting"
            )) {

                throw new Exception(
                        "اللعبة بدأت بالفعل"
                );
            }

            roomId =
                    room.getString("id");

            roomCode =
                    room.getString("code");

            isHost = false;

            JSONArray players =
                    getPlayers();

            if (players.length() >= MAX_PLAYERS) {

                throw new Exception(
                        "الغرفة ممتلئة"
                );
            }

            for (
                    int i = 0;
                    i < players.length();
                    i++
            ) {

                String existing =
                        players
                                .getJSONObject(i)
                                .optString(
                                        "name",
                                        ""
                                );

                if (existing.equalsIgnoreCase(
                        playerName
                )) {

                    throw new Exception(
                            "الاسم مستعمل في الغرفة"
                    );
                }
            }

            addPlayer();

            runOnUiThread(
                    () -> showLobby()
            );

        } catch (Exception e) {

            runOnUiThread(() -> {

                source.setEnabled(true);

                String msg =
                        e.getMessage();

                if (msg == null ||
                        msg.isEmpty()) {

                    msg =
                            "تعذر الانضمام";
                }

                toast(msg);
            });
        }
    }

    // ============================================================
    // PLAYER
    // ============================================================

    private void addPlayer()
            throws Exception {

        JSONObject player =
                new JSONObject();

        player.put(
                "room_id",
                roomId
        );

        player.put(
                "room_code",
                roomCode
        );

        player.put(
                "name",
                playerName
        );

        player.put(
                "is_host",
                isHost
        );

        player.put(
                "player_id",
                playerId
        );

        player.put(
                "score",
                0
        );

        player.put(
                "ready",
                false
        );

        request(
                "POST",
                SUPABASE_URL +
                        "/rest/v1/players",
                player.toString()
        );
    }

    private JSONArray getPlayers()
            throws Exception {

        return new JSONArray(
                request(
                        "GET",
                        SUPABASE_URL +
                                "/rest/v1/players?room_id=eq." +
                                URLEncoder.encode(
                                        roomId,
                                        "UTF-8"
                                ) +
                                "&select=*&order=joined_at.asc",
                        null
                )
        );
    }

    // ============================================================
    // LOBBY
    // ============================================================

    private void showLobby() {

        stopTimer();

        LinearLayout root =
                root();

        root.addView(
                title("🎮 Lobby")
        );

        TextView room =
                subtitle(
                        "كود الغرفة\n" +
                                roomCode
                );

        room.setTextSize(23);

        room.setTextColor(
                textColor
        );

        room.setTypeface(
                null,
                Typeface.BOLD
        );

        root.addView(room);

        Button copy =
                button("📋 نسخ الكود");

        copy.setOnClickListener(
                v -> copyRoomCode()
        );

        root.addView(copy);

        Button share =
                button("📤 مشاركة الكود");

        share.setOnClickListener(
                v -> shareRoomCode()
        );

        root.addView(share);

        LinearLayout playersCard =
                card();

        root.addView(playersCard);

        TextView status =
                new TextView(this);

        status.setTextColor(
                secondaryTextColor
        );

        status.setTextSize(15);

        playersCard.addView(status);

        loadLobbyPlayers(
                playersCard,
                status
        );

        Button ready =
                button("✅ Ready");

        ready.setOnClickListener(v -> {

            ready.setEnabled(false);

            new Thread(() -> {

                try {

                    setPlayerReady(true);

                    playSuccess();

                    runOnUiThread(() -> {

                        ready.setText(
                                "✓ أنت Ready"
                        );

                        toast(
                                "تم تسجيلك Ready"
                        );
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        ready.setEnabled(true);

                        toast(
                                "تعذر تسجيل Ready"
                        );
                    });
                }

            }).start();
        });

        root.addView(ready);

        if (isHost) {

            Button start =
                    button("🚀 بدء اللعبة");

            start.setOnClickListener(v -> {

                start.setEnabled(false);

                new Thread(() -> {

                    try {

                        JSONArray players =
                                getPlayers();

                        if (players.length() != 2) {

                            throw new Exception(
                                    "يلزم لاعبان بالضبط"
                            );
                        }

                        for (
                                int i = 0;
                                i < players.length();
                                i++
                        ) {

                            if (!players
                                    .getJSONObject(i)
                                    .optBoolean(
                                            "ready",
                                            false
                                    )) {

                                throw new Exception(
                                        "لازم الاثنين يعملوا Ready"
                                );
                            }
                        }

                        setAllReady(false);

                        setRoomStatus(
                                "playing"
                        );

                        setGameState(
                                0,
                                "answering"
                        );

                        questionIndex = 0;

                        runOnUiThread(
                                () ->
                                        showAnswerScreen()
                        );

                    } catch (Exception e) {

                        runOnUiThread(() -> {

                            start.setEnabled(true);

                            String msg =
                                    e.getMessage();

                            if (msg == null) {
                                msg =
                                        "تعذر بدء اللعبة";
                            }

                            toast(msg);
                        });
                    }

                }).start();
            });

            root.addView(start);
        }

        Button chat =
                button("💬 Chat");

        chat.setOnClickListener(
                v -> showChat()
        );

        root.addView(chat);

        Button leave =
                button("🚪 مغادرة الغرفة");

        leave.setOnClickListener(
                v -> leaveRoom()
        );

        root.addView(leave);

        startLobbyPolling(status);

        setScreen(
                scroll(root),
                "lobby"
        );
    }

    private void copyRoomCode() {

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );

        if (clipboard != null) {

            clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                            "GuessUs Room",
                            roomCode
                    )
            );

            playSuccess();

            toast(
                    "تم نسخ كود الغرفة 📋"
            );
        }
    }

    private void shareRoomCode() {

        Intent intent =
                new Intent(
                        Intent.ACTION_SEND
                );

        intent.setType(
                "text/plain"
        );

        intent.putExtra(
                Intent.EXTRA_TEXT,
                "🎮 تعال نلعب GuessUs!\n" +
                        "كود الغرفة: " +
                        roomCode
        );

        startActivity(
                Intent.createChooser(
                        intent,
                        "مشاركة كود GuessUs"
                )
        );
    }

    private void loadLobbyPlayers(
            LinearLayout card,
            TextView status
    ) {

        new Thread(() -> {

            try {

                JSONArray players =
                        getPlayers();

                runOnUiThread(() -> {

                    card.removeAllViews();

                    TextView header =
                            new TextView(this);

                    header.setText(
                            "اللاعبين (" +
                                    players.length() +
                                    "/" +
                                    MAX_PLAYERS +
                                    ")"
                    );

                    header.setTextColor(
                            textColor
                    );

                    header.setTextSize(19);

                    header.setTypeface(
                            null,
                            Typeface.BOLD
                    );

                    card.addView(header);

                    for (
                            int i = 0;
                            i < players.length();
                            i++
                    ) {

                        JSONObject p =
                                players.optJSONObject(i);

                        if (p == null) continue;

                        String name =
                                p.optString(
                                        "name",
                                        "?"
                                );

                        boolean host =
                                p.optBoolean(
                                        "is_host",
                                        false
                                );

                        boolean ready =
                                p.optBoolean(
                                        "ready",
                                        false
                                );

                        TextView row =
                                new TextView(this);

                        row.setText(
                                (host
                                        ? "👑 "
                                        : "👤 ")
                                        +
                                        name +
                                        (
                                                ready
                                                        ? "   ✓ Ready"
                                                        : "   • Waiting"
                                        )
                        );

                        row.setTextColor(
                                textColor
                        );

                        row.setTextSize(16);

                        row.setPadding(
                                0,
                                dp(10),
                                0,
                                dp(10)
                        );

                        card.addView(row);
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        status.setText(
                                "تعذر تحديث اللاعبين"
                        )
                );
            }

        }).start();
    }

    private void startLobbyPolling(
            TextView status
    ) {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!"lobby".equals(
                                currentScreen
                        )) {
                            return;
                        }

                        syncHostRole();

                        new Thread(() -> {

                            try {

                                JSONArray players =
                                        getPlayers();

                                runOnUiThread(() ->
                                        status.setText(
                                                "متصل • " +
                                                        players.length() +
                                                        " / " +
                                                        MAX_PLAYERS +
                                                        " لاعبين"
                                        )
                                );

                                checkLobbyGameState();

                            } catch (Exception ignored) {
                            }

                        }).start();

                        handler.postDelayed(
                                this,
                                2500
                        );
                    }
                },
                2500
        );
    }

    private void checkLobbyGameState() {

        try {

            JSONObject state =
                    getGameState();

            String status =
                    state.optString(
                            "status",
                            "waiting"
                    );

            int index =
                    state.optInt(
                            "question_index",
                            0
                    );

            if (status.equals(
                    "answering"
            )) {

                questionIndex = index;

                runOnUiThread(() ->
                        showAnswerScreen()
                );
            }

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // HOST SYNC
    // ============================================================

    private void syncHostRole() {

        try {

            JSONArray me =
                    new JSONArray(
                            request(
                                    "GET",
                                    SUPABASE_URL +
                                            "/rest/v1/players?player_id=eq." +
                                            URLEncoder.encode(
                                                    playerId,
                                                    "UTF-8"
                                            ) +
                                            "&room_id=eq." +
                                            URLEncoder.encode(
                                                    roomId,
                                                    "UTF-8"
                                            ) +
                                            "&select=is_host,score",
                                    null
                            )
                    );

            if (me.length() > 0) {

                JSONObject p =
                        me.getJSONObject(0);

                isHost =
                        p.optBoolean(
                                "is_host",
                                false
                        );

                score =
                        p.optInt(
                                "score",
                                score
                        );
            }

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // ANSWER SCREEN
    // ============================================================

    private void showAnswerScreen() {

        stopTimer();

        answerSent = false;

        predictionSent = false;

        nextReadySent = false;

        LinearLayout root =
                root();

        root.addView(
                title(
                        "السؤال " +
                                (questionIndex + 1) +
                                "/" +
                                questions.length
                )
        );

        ProgressBar progress =
                new ProgressBar(
                        this,
                        null,
                        android.R.attr.progressBarStyleHorizontal
                );

        progress.setMax(
                questions.length
        );

        progress.setProgress(
                questionIndex + 1
        );

        LinearLayout.LayoutParams pp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(8)
                );

        pp.setMargins(
                dp(8),
                dp(4),
                dp(8),
                dp(14)
        );

        progress.setLayoutParams(pp);

        root.addView(progress);

        LinearLayout qCard =
                card();

        TextView q =
                new TextView(this);

        q.setText(
                questions[questionIndex]
        );

        q.setTextColor(
                textColor
        );

        q.setTextSize(23);

        q.setGravity(
                Gravity.CENTER
        );

        q.setTypeface(
                null,
                Typeface.BOLD
        );

        q.setPadding(
                dp(5),
                dp(20),
                dp(5),
                dp(20)
        );

        qCard.addView(q);

        root.addView(qCard);

        TextView timer =
                subtitle(
                        "⏱️ " +
                                ANSWER_TIME +
                                " ثانية"
                );

        timer.setTextSize(20);

        timer.setTextColor(
                accentColor
        );

        timer.setTypeface(
                null,
                Typeface.BOLD
        );

        root.addView(timer);

        EditText answer =
                input(
                        "اكتب إجابتك..."
                );

        answer.setSingleLine(false);

        answer.setMinHeight(
                dp(110)
        );

        answer.setGravity(
                Gravity.TOP |
                        Gravity.RIGHT
        );

        root.addView(answer);

        Button send =
                button("📤 إرسال الإجابة");

        send.setOnClickListener(v -> {

            if (answerSent) {
                return;
            }

            String value =
                    answer.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {

                toast(
                        "اكتب إجابتك"
                );

                return;
            }

            answerSent = true;

            send.setEnabled(false);

            new Thread(() -> {

                try {

                    submitAnswer(value);

                    runOnUiThread(
                            () ->
                                    showWaitingForAnswers()
                    );

                } catch (Exception e) {

                    answerSent = false;

                    runOnUiThread(() -> {

                        send.setEnabled(true);

                        toast(
                                "فشل إرسال الإجابة"
                        );
                    });
                }

            }).start();
        });

        root.addView(send);

        Button skip =
                button("⏭️ تخطي السؤال");

        skip.setOnClickListener(v -> {

            if (answerSent) {
                return;
            }

            answerSent = true;

            send.setEnabled(false);

            skip.setEnabled(false);

            new Thread(() -> {

                try {

                    submitAnswer(
                            "SKIP"
                    );

                    runOnUiThread(
                            () ->
                                    showWaitingForAnswers()
                    );

                } catch (Exception e) {

                    answerSent = false;

                    runOnUiThread(() -> {

                        send.setEnabled(true);

                        skip.setEnabled(true);

                        toast(
                                "تعذر التخطي"
                        );
                    });
                }

            }).start();
        });

        root.addView(skip);

        Button chat =
                button("💬 Chat");

        chat.setOnClickListener(
                v -> showChat()
        );

        root.addView(chat);

        setScreen(
                scroll(root),
                "answer"
        );

        startAnswerTimer(
                timer,
                send,
                skip
        );
    }

    private void startAnswerTimer(
            TextView timer,
            Button send,
            Button skip
    ) {

        answerTimer =
                new CountDownTimer(
                        ANSWER_TIME * 1000L,
                        1000
                ) {

                    @Override
                    public void onTick(
                            long left
                    ) {

                        long seconds =
                                left / 1000;

                        timer.setText(
                                "⏱️ " +
                                        seconds +
                                        " ثانية"
                        );
                    }

                    @Override
                    public void onFinish() {

                        timer.setText(
                                "⏰ انتهى الوقت"
                        );

                        if (!answerSent) {

                            answerSent = true;

                            send.setEnabled(false);

                            skip.setEnabled(false);

                            new Thread(() -> {

                                try {

                                    submitAnswer(
                                            "SKIP"
                                    );

                                    runOnUiThread(
                                            () ->
                                                    showWaitingForAnswers()
                                    );

                                } catch (Exception ignored) {
                                }

                            }).start();
                        }
                    }
                };

        answerTimer.start();
    }

    private void stopTimer() {

        if (answerTimer != null) {

            answerTimer.cancel();

            answerTimer = null;
        }
    }

    // ============================================================
    // ANSWERS
    // ============================================================

    private void submitAnswer(
            String answer
    ) throws Exception {

        JSONObject object =
                new JSONObject();

        object.put(
                "room_code",
                roomCode
        );

        object.put(
                "round",
                questionIndex
        );

        object.put(
                "player_name",
                playerName
        );

        object.put(
                "answer",
                answer
        );

        request(
                "POST",
                SUPABASE_URL +
                        "/rest/v1/round_answers",
                object.toString()
        );
    }

    private JSONArray getRoundAnswers()
            throws Exception {

        return new JSONArray(
                request(
                        "GET",
                        SUPABASE_URL +
                                "/rest/v1/round_answers?room_code=eq." +
                                URLEncoder.encode(
                                        roomCode,
                                        "UTF-8"
                                ) +
                                "&round=eq." +
                                questionIndex +
                                "&select=*",
                        null
                )
        );
    }

    // ============================================================
    // WAITING ANSWERS
    // ============================================================

    private void showWaitingForAnswers() {

        stopTimer();

        LinearLayout root =
                root();

        root.addView(
                title(
                        "⏳ نستنّاو صاحبك"
                )
        );

        TextView info =
                subtitle(
                        "تم إرسال إجابتك بنجاح.\n" +
                                "كي يكمل اللاعب الآخر، تبدأ مرحلة التوقع."
                );

        root.addView(info);

        TextView counter =
                subtitle(
                        "الإجابات: 0 / 2"
                );

        counter.setTextSize(19);

        counter.setTextColor(
                accentColor
        );

        root.addView(counter);

        Button chat =
                button("💬 Chat");

        chat.setOnClickListener(
                v -> showChat()
        );

        root.addView(chat);

        setScreen(
                scroll(root),
                "waiting_answers"
        );

        startAnswerPolling(counter);
    }

    private void startAnswerPolling(
            TextView counter
    ) {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!"waiting_answers"
                                .equals(currentScreen)) {

                            return;
                        }

                        if (!pollingBusy.compareAndSet(
                                false,
                                true
                        )) {

                            handler.postDelayed(
                                    this,
                                    1000
                            );

                            return;
                        }

                        new Thread(() -> {

                            try {

                                JSONArray answers =
                                        getRoundAnswers();

                                JSONArray players =
                                        getPlayers();

                                int count =
                                        answers.length();

                                int total =
                                        players.length();

                                runOnUiThread(() ->
                                        counter.setText(
                                                "الإجابات: " +
                                                        count +
                                                        " / " +
                                                        total
                                        )
                                );

                                JSONObject state =
                                        getGameState();

                                String stateStatus =
                                        state.optString(
                                                "status",
                                                "answering"
                                        );

                                if (stateStatus.equals(
                                        "predicting"
                                )) {

                                    runOnUiThread(() ->
                                            showPredictionScreen()
                                    );

                                    return;
                                }

                                if (count >= total &&
                                        total == 2 &&
                                        isHost) {

                                    setGameState(
                                            questionIndex,
                                            "predicting"
                                    );

                                    runOnUiThread(() ->
                                            showPredictionScreen()
                                    );
                                }

                            } catch (Exception ignored) {

                            } finally {

                                pollingBusy.set(false);
                            }

                        }).start();

                        handler.postDelayed(
                                this,
                                2000
                        );
                    }
                },
                1000
        );
    }

    // ============================================================
    // PREDICTION
    // ============================================================

    private void showPredictionScreen() {

        stopTimer();

        predictionSent = false;

        LinearLayout root =
                root();

        root.addView(
                title("🎯 وقت التوقع!")
        );

        root.addView(
                subtitle(
                        "اختار صاحبك وتوقّع بالضبط شنوّة كتب."
                )
        );

        LinearLayout questionCard =
                card();

        TextView question =
                new TextView(this);

        question.setText(
                questions[questionIndex]
        );

        question.setTextColor(
                textColor
        );

        question.setTextSize(20);

        question.setGravity(
                Gravity.CENTER
        );

        question.setTypeface(
                null,
                Typeface.BOLD
        );

        question.setPadding(
                dp(5),
                dp(15),
                dp(5),
                dp(15)
        );

        questionCard.addView(question);

        root.addView(questionCard);

        root.addView(
                label("اختار اللاعب:")
        );

        Spinner spinner =
                new Spinner(this);

        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                );

        spinnerParams.setMargins(
                0,
                dp(5),
                0,
                dp(10)
        );

        spinner.setLayoutParams(
                spinnerParams
        );

        root.addView(spinner);

        root.addView(
                label("توقّع إجابته:")
        );

        EditText prediction =
                input(
                        "شنوّة تتوقع جاوب؟"
                );

        prediction.setSingleLine(false);

        prediction.setMinHeight(
                dp(90)
        );

        prediction.setGravity(
                Gravity.TOP |
                        Gravity.RIGHT
        );

        root.addView(prediction);

        Button submit =
                button("🎯 تأكيد التوقع");

        root.addView(submit);

        Button skip =
                button("⏭️ ما نحبش نتوقع");

        root.addView(skip);

        submit.setEnabled(false);

        skip.setEnabled(false);

        loadPredictionPlayers(
                spinner,
                submit,
                skip
        );

        submit.setOnClickListener(v -> {

            if (predictionSent) {
                return;
            }

            Object selected =
                    spinner.getSelectedItem();

            if (selected == null) {

                toast(
                        "اختار لاعب"
                );

                return;
            }

            String target =
                    selected.toString();

            String predicted =
                    prediction
                            .getText()
                            .toString()
                            .trim();

            if (predicted.isEmpty()) {

                toast(
                        "اكتب توقعك"
                );

                return;
            }

            predictionSent = true;

            submit.setEnabled(false);

            skip.setEnabled(false);

            new Thread(() -> {

                try {

                    submitPrediction(
                            target,
                            predicted
                    );

                    runOnUiThread(
                            () ->
                                    showPredictionWaiting()
                    );

                } catch (Exception e) {

                    predictionSent = false;

                    runOnUiThread(() -> {

                        submit.setEnabled(true);

                        skip.setEnabled(true);

                        toast(
                                "فشل إرسال التوقع"
                        );
                    });
                }

            }).start();
        });

        skip.setOnClickListener(v -> {

            if (predictionSent) {
                return;
            }

            Object selected =
                    spinner.getSelectedItem();

            if (selected == null) {

                toast(
                        "اختار لاعب"
                );

                return;
            }

            String target =
                    selected.toString();

            predictionSent = true;

            submit.setEnabled(false);

            skip.setEnabled(false);

            new Thread(() -> {

                try {

                    submitPrediction(
                            target,
                            "SKIP"
                    );

                    runOnUiThread(
                            () ->
                                    showPredictionWaiting()
                    );

                } catch (Exception e) {

                    predictionSent = false;

                    runOnUiThread(() -> {

                        submit.setEnabled(true);

                        skip.setEnabled(true);

                        toast(
                                "فشل التخطي"
                        );
                    });
                }

            }).start();
        });

        setScreen(
                scroll(root),
                "prediction"
        );
    }

    private void loadPredictionPlayers(
            Spinner spinner,
            Button submit,
            Button skip
    ) {

        new Thread(() -> {

            try {

                JSONArray players =
                        getPlayers();

                ArrayList<String> names =
                        new ArrayList<>();

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    JSONObject p =
                            players.getJSONObject(i);

                    String name =
                            p.optString(
                                    "name",
                                    ""
                            );

                    if (!name.isEmpty() &&
                            !name.equals(
                                    playerName
                            )) {

                        names.add(name);
                    }
                }

                runOnUiThread(() -> {

                    if (names.isEmpty()) {

                        toast(
                                "ما فماش لاعب متاح"
                        );

                        return;
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<String>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    names
                            ) {

                                @Override
                                public View getView(
                                        int position,
                                        View convertView,
                                        ViewGroup parent
                                ) {

                                    TextView view =
                                            (TextView)
                                                    super.getView(
                                                            position,
                                                            convertView,
                                                            parent
                                                    );

                                    view.setTextColor(
                                            textColor
                                    );

                                    view.setTextSize(16);

                                    view.setPadding(
                                            dp(12),
                                            0,
                                            dp(12),
                                            0
                                    );

                                    return view;
                                }

                                @Override
                                public View getDropDownView(
                                        int position,
                                        View convertView,
                                        ViewGroup parent
                                ) {

                                    TextView view =
                                            (TextView)
                                                    super.getDropDownView(
                                                            position,
                                                            convertView,
                                                            parent
                                                    );

                                    view.setTextColor(
                                            textColor
                                    );

                                    view.setTextSize(16);

                                    view.setPadding(
                                            dp(12),
                                            dp(12),
                                            dp(12),
                                            dp(12)
                                    );

                                    return view;
                                }
                            };

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinner.setAdapter(adapter);

                    submit.setEnabled(true);

                    skip.setEnabled(true);
                });

            } catch (Exception ignored) {

                runOnUiThread(() ->
                        toast(
                                "تعذر تحميل اللاعبين"
                        )
                );
            }

        }).start();
    }

    // ============================================================
    // PREDICTION LOGIC
    // ============================================================

    private void submitPrediction(
            String target,
            String predicted
    ) throws Exception {

        String targetAnswer =
                getTargetAnswer(target);

        boolean correct =
                !predicted.equals("SKIP") &&
                        similarAnswer(
                                predicted,
                                targetAnswer
                        );

        int points = 0;

        /*
         * النظام:
         *
         * توقع صحيح = +3
         *
         * إذا كان توقعك مطابقًا تمامًا
         * لإجابة اللاعب = +2 إضافية
         *
         * المجموع = +5
         */

        if (correct) {

            points = 3;

            if (normalize(predicted)
                    .equals(
                            normalize(
                                    targetAnswer
                            )
                    )) {

                points += 2;
            }
        }

        JSONObject prediction =
                new JSONObject();

        prediction.put(
                "room_code",
                roomCode
        );

        prediction.put(
                "round",
                questionIndex
        );

        prediction.put(
                "predictor",
                playerName
        );

        prediction.put(
                "target",
                target
        );

        prediction.put(
                "predicted_answer",
                predicted
        );

        prediction.put(
                "correct",
                correct
        );

        prediction.put(
                "points",
                points
        );

        request(
                "POST",
                SUPABASE_URL +
                        "/rest/v1/predictions",
                prediction.toString()
        );

        if (points > 0) {

            score += points;

            updateMyScore();

            playSuccess();

        } else {

            playWrong();
        }
    }

    private String getTargetAnswer(
            String target
    ) throws Exception {

        JSONArray result =
                new JSONArray(
                        request(
                                "GET",
                                SUPABASE_URL +
                                        "/rest/v1/round_answers" +
                                        "?room_code=eq." +
                                        URLEncoder.encode(
                                                roomCode,
                                                "UTF-8"
                                        ) +
                                        "&round=eq." +
                                        questionIndex +
                                        "&player_name=eq." +
                                        URLEncoder.encode(
                                                target,
                                                "UTF-8"
                                        ) +
                                        "&select=answer",
                                null
                        )
                );

        if (result.length() == 0) {

            throw new Exception(
                    "إجابة اللاعب غير موجودة"
            );
        }

        return result
                .getJSONObject(0)
                .optString(
                        "answer",
                        ""
                );
    }

    // ============================================================
    // PREDICTION WAITING
    // ============================================================

    private void showPredictionWaiting() {

        LinearLayout root =
                root();

        root.addView(
                title(
                        "⏳ تم إرسال التوقع"
                )
        );

        TextView info =
                subtitle(
                        "نستنّاو اللاعب الآخر يكمل."
                );

        root.addView(info);

        TextView counter =
                subtitle(
                        "التوقعات: 0 / 2"
                );

        counter.setTextSize(19);

        counter.setTextColor(
                accentColor
        );

        root.addView(counter);

        Button chat =
                button("💬 Chat");

        chat.setOnClickListener(
                v -> showChat()
        );

        root.addView(chat);

        setScreen(
                scroll(root),
                "prediction_waiting"
        );

        startPredictionPolling(
                counter
        );
    }

    private void startPredictionPolling(
            TextView counter
    ) {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!"prediction_waiting"
                                .equals(currentScreen)) {

                            return;
                        }

                        new Thread(() -> {

                            try {

                                JSONArray predictions =
                                        getPredictions();

                                JSONArray players =
                                        getPlayers();

                                int count =
                                        predictions.length();

                                int total =
                                        players.length();

                                runOnUiThread(() ->
                                        counter.setText(
                                                "التوقعات: " +
                                                        count +
                                                        " / " +
                                                        total
                                        )
                                );

                                JSONObject state =
                                        getGameState();

                                String status =
                                        state.optString(
                                                "status",
                                                "predicting"
                                        );

                                if (status.equals(
                                        "results"
                                )) {

                                    runOnUiThread(() ->
                                            showResults()
                                    );

                                    return;
                                }

                                if (count >= total &&
                                        total == 2 &&
                                        isHost) {

                                    setGameState(
                                            questionIndex,
                                            "results"
                                    );

                                    runOnUiThread(() ->
                                            showResults()
                                    );
                                }

                            } catch (Exception ignored) {
                            }

                        }).start();

                        handler.postDelayed(
                                this,
                                2000
                        );
                    }
                },
                1000
        );
    }

    private JSONArray getPredictions()
            throws Exception {

        return new JSONArray(
                request(
                        "GET",
                        SUPABASE_URL +
                                "/rest/v1/predictions" +
                                "?room_code=eq." +
                                URLEncoder.encode(
                                        roomCode,
                                        "UTF-8"
                                ) +
                                "&round=eq." +
                                questionIndex +
                                "&select=*",
                        null
                )
        );
    }

    // ============================================================
    // RESULTS
    // ============================================================

    private void showResults() {

        LinearLayout root =
                root();

        root.addView(
                title("🏆 نتيجة الجولة")
        );

        LinearLayout scoreCard =
                card();

        TextView scoreText =
                new TextView(this);

        scoreText.setText(
                "⭐ نقاطك\n" +
                        score
        );

        scoreText.setTextColor(
                accentColor
        );

        scoreText.setTextSize(28);

        scoreText.setGravity(
                Gravity.CENTER
        );

        scoreText.setTypeface(
                null,
                Typeface.BOLD
        );

        scoreCard.addView(scoreText);

        root.addView(scoreCard);

        loadRoundResult(scoreCard);

        Button next =
                button(
                        "➡️ جاهز للجولة التالية"
                );

        next.setOnClickListener(v -> {

            if (nextReadySent) {
                return;
            }

            nextReadySent = true;

            next.setEnabled(false);

            new Thread(() -> {

                try {

                    setPlayerReady(true);

                    runOnUiThread(
                            () ->
                                    waitForNextRound()
                    );

                } catch (Exception e) {

                    nextReadySent = false;

                    runOnUiThread(() -> {

                        next.setEnabled(true);

                        toast(
                                "تعذر التسجيل"
                        );
                    });
                }

            }).start();
        });

        root.addView(next);

        Button chat =
                button("💬 Chat");

        chat.setOnClickListener(
                v -> showChat()
        );

        root.addView(chat);

        setScreen(
                scroll(root),
                "results"
        );

        startResultsPolling();
    }

    private void loadRoundResult(
            LinearLayout card
    ) {

        new Thread(() -> {

            try {

                JSONArray data =
                        new JSONArray(
                                request(
                                        "GET",
                                        SUPABASE_URL +
                                                "/rest/v1/predictions" +
                                                "?room_code=eq." +
                                                URLEncoder.encode(
                                                        roomCode,
                                                        "UTF-8"
                                                ) +
                                                "&round=eq." +
                                                questionIndex +
                                                "&predictor=eq." +
                                                URLEncoder.encode(
                                                        playerName,
                                                        "UTF-8"
                                                ) +
                                                "&select=*",
                                        null
                                )
                        );

                if (data.length() == 0) {
                    return;
                }

                JSONObject p =
                        data.getJSONObject(0);

                boolean correct =
                        p.optBoolean(
                                "correct",
                                false
                        );

                int points =
                        p.optInt(
                                "points",
                                0
                        );

                runOnUiThread(() -> {

                    TextView result =
                            new TextView(this);

                    result.setText(
                            correct
                                    ? "🎯 توقّع صحيح!\n+" +
                                            points +
                                            " نقاط"
                                    : "❌ التوقع ما صابش\n0 نقاط"
                    );

                    result.setTextColor(
                            textColor
                    );

                    result.setTextSize(18);

                    result.setGravity(
                            Gravity.CENTER
                    );

                    result.setPadding(
                            0,
                            dp(15),
                            0,
                            dp(10)
                    );

                    card.addView(result);
                });

            } catch (Exception ignored) {
            }

        }).start();
    }

    // ============================================================
    // NEXT ROUND
    // ============================================================

    private void waitForNextRound() {

        LinearLayout root =
                root();

        root.addView(
                title("⏳ جاهز")
        );

        TextView text =
                subtitle(
                        "نستنّاو اللاعب الآخر..."
                );

        root.addView(text);

        setScreen(
                scroll(root),
                "waiting_next"
        );
    }

    private void startResultsPolling() {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!"results".equals(
                                currentScreen
                        ) &&
                                !"waiting_next".equals(
                                        currentScreen
                                )) {

                            return;
                        }

                        new Thread(() -> {

                            try {

                                JSONObject state =
                                        getGameState();

                                String status =
                                        state.optString(
                                                "status",
                                                "results"
                                        );

                                if (status.equals(
                                        "answering"
                                )) {

                                    questionIndex =
                                            state.optInt(
                                                    "question_index",
                                                    questionIndex
                                            );

                                    runOnUiThread(() ->
                                            showAnswerScreen()
                                    );

                                    return;
                                }

                                if (status.equals(
                                        "finished"
                                )) {

                                    runOnUiThread(() ->
                                            showLeaderboard()
                                    );

                                    return;
                                }

                                JSONArray players =
                                        getPlayers();

                                if (players.length() != 2) {
                                    return;
                                }

                                boolean allReady = true;

                                for (
                                        int i = 0;
                                        i < players.length();
                                        i++
                                ) {

                                    if (!players
                                            .getJSONObject(i)
                                            .optBoolean(
                                                    "ready",
                                                    false
                                            )) {

                                        allReady = false;

                                        break;
                                    }
                                }

                                if (allReady &&
                                        isHost) {

                                    advanceRound();
                                }

                            } catch (Exception ignored) {
                            }

                        }).start();

                        handler.postDelayed(
                                this,
                                2000
                        );
                    }
                },
                1500
        );
    }

    // ============================================================
    // ADVANCE
    // ============================================================

    private void advanceRound() {

        new Thread(() -> {

            try {

                int next =
                        questionIndex + 1;

                setAllReady(false);

                if (next >= questions.length) {

                    setGameState(
                            questionIndex,
                            "finished"
                    );

                    setRoomStatus(
                            "waiting"
                    );

                    runOnUiThread(() ->
                            showLeaderboard()
                    );

                    return;
                }

                questionIndex = next;

                setGameState(
                        questionIndex,
                        "answering"
                );

                runOnUiThread(() ->
                        showAnswerScreen()
                );

            } catch (Exception ignored) {
            }

        }).start();
    }

    // ============================================================
    // LEADERBOARD
    // ============================================================

    private void showLeaderboard() {

        stopTimer();

        LinearLayout root =
                root();

        root.addView(
                title("🏆 Leaderboard")
        );

        root.addView(
                subtitle(
                        "النتائج النهائية"
                )
        );

        LinearLayout card =
                card();

        root.addView(card);

        new Thread(() -> {

            try {

                JSONArray players =
                        getPlayers();

                List<JSONObject> list =
                        new ArrayList<>();

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    list.add(
                            players.getJSONObject(i)
                    );
                }

                list.sort(
                        (a, b) ->
                                Integer.compare(
                                        b.optInt(
                                                "score",
                                                0
                                        ),
                                        a.optInt(
                                                "score",
                                                0
                                        )
                                )
                );

                runOnUiThread(() -> {

                    card.removeAllViews();

                    for (
                            int i = 0;
                            i < list.size();
                            i++
                    ) {

                        JSONObject p =
                                list.get(i);

                        String name =
                                p.optString(
                                        "name",
                                        "?"
                                );

                        int points =
                                p.optInt(
                                        "score",
                                        0
                                );

                        TextView row =
                                new TextView(this);

                        String medal;

                        if (i == 0) {

                            medal = "🥇";

                        } else if (i == 1) {

                            medal = "🥈";

                        } else {

                            medal = "🏅";
                        }

                        row.setText(
                                medal +
                                        "  " +
                                        (i + 1) +
                                        ". " +
                                        name +
                                        "\n      ⭐ " +
                                        points +
                                        " نقطة"
                        );

                        row.setTextColor(
                                textColor
                        );

                        row.setTextSize(19);

                        row.setPadding(
                                0,
                                dp(14),
                                0,
                                dp(14)
                        );

                        card.addView(row);
                    }

                    if (list.size() == 2) {

                        int first =
                                list.get(0)
                                        .optInt(
                                                "score",
                                                0
                                        );

                        int second =
                                list.get(1)
                                        .optInt(
                                                "score",
                                                0
                                        );

                        TextView winner =
                                new TextView(this);

                        if (first == second) {

                            winner.setText(
                                    "🤝 تعادل!"
                            );

                        } else {

                            winner.setText(
                                    "👑 الفائز: " +
                                            list.get(0)
                                                    .optString(
                                                            "name",
                                                            "?"
                                                    )
                            );
                        }

                        winner.setTextColor(
                                accentColor
                        );

                        winner.setTextSize(22);

                        winner.setGravity(
                                Gravity.CENTER
                        );

                        winner.setTypeface(
                                null,
                                Typeface.BOLD
                        );

                        winner.setPadding(
                                0,
                                dp(15),
                                0,
                                dp(5)
                        );

                        card.addView(winner);
                    }
                });

            } catch (Exception ignored) {
            }

        }).start();

        Button playAgain =
                button(
                        "🔄 Play Again"
                );

        playAgain.setOnClickListener(v -> {

            if (!isHost) {

                toast(
                        "الـHost هو اللي يبدأ من جديد"
                );

                return;
            }

            playAgain.setEnabled(false);

            new Thread(() -> {

                try {

                    resetScores();

                    setAllReady(false);

                    questionIndex = 0;

                    setRoomStatus(
                            "playing"
                    );

                    setGameState(
                            0,
                            "answering"
                    );

                    runOnUiThread(() ->
                            showAnswerScreen()
                    );

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        playAgain.setEnabled(true);

                        toast(
                                "تعذر إعادة اللعبة"
                        );
                    });
                }

            }).start();
        });

        root.addView(playAgain);

        Button lobby =
                button(
                        "🏠 العودة للـLobby"
                );

        lobby.setOnClickListener(
                v -> showLobby()
        );

        root.addView(lobby);

        Button home =
                button(
                        "🏠 الصفحة الرئيسية"
                );

        home.setOnClickListener(
                v -> showHome()
        );

        root.addView(home);

        setScreen(
                scroll(root),
                "leaderboard"
        );
    }

    // ============================================================
    // RESET
    // ============================================================

    private void resetScores()
            throws Exception {

        JSONArray players =
                getPlayers();

        for (
                int i = 0;
                i < players.length();
                i++
        ) {

            JSONObject p =
                    players.getJSONObject(i);

            String id =
                    p.optString(
                            "player_id",
                            ""
                    );

            if (id.isEmpty()) {
                continue;
            }

            JSONObject update =
                    new JSONObject();

            update.put(
                    "score",
                    0
            );

            update.put(
                    "ready",
                    false
            );

            request(
                    "PATCH",
                    SUPABASE_URL +
                            "/rest/v1/players?player_id=eq." +
                            URLEncoder.encode(
                                    id,
                                    "UTF-8"
                            ),
                    update.toString()
            );
        }

        score = 0;
    }

    private void updateMyScore()
            throws Exception {

        JSONObject update =
                new JSONObject();

        update.put(
                "score",
                score
        );

        request(
                "PATCH",
                SUPABASE_URL +
                        "/rest/v1/players?player_id=eq." +
                        URLEncoder.encode(
                                playerId,
                                "UTF-8"
                        ),
                update.toString()
        );
    }

    // ============================================================
    // READY
    // ============================================================

    private void setPlayerReady(
            boolean ready
    ) throws Exception {

        JSONObject update =
                new JSONObject();

        update.put(
                "ready",
                ready
        );

        request(
                "PATCH",
                SUPABASE_URL +
                        "/rest/v1/players?player_id=eq." +
                        URLEncoder.encode(
                                playerId,
                                "UTF-8"
                        ),
                update.toString()
        );
    }

    private void setAllReady(
            boolean ready
    ) throws Exception {

        JSONObject update =
                new JSONObject();

        update.put(
                "ready",
                ready
        );

        request(
                "PATCH",
                SUPABASE_URL +
                        "/rest/v1/players?room_id=eq." +
                        URLEncoder.encode(
                                roomId,
                                "UTF-8"
                        ),
                update.toString()
        );
    }

    // ============================================================
    // LEAVE / HOST TRANSFER
    // ============================================================

    private void leaveRoom() {

        new Thread(() -> {

            try {

                if (isHost) {

                    JSONArray players =
                            getPlayers();

                    for (
                            int i = 0;
                            i < players.length();
                            i++
                    ) {

                        JSONObject p =
                                players
                                        .getJSONObject(i);

                        String id =
                                p.optString(
                                        "player_id",
                                        ""
                                );

                        if (!id.equals(
                                playerId
                        )) {

                            JSONObject update =
                                    new JSONObject();

                            update.put(
                                    "is_host",
                                    true
                            );

                            request(
                                    "PATCH",
                                    SUPABASE_URL +
                                            "/rest/v1/players?player_id=eq." +
                                            URLEncoder.encode(
                                                    id,
                                                    "UTF-8"
                                            ),
                                    update.toString()
                            );

                            break;
                        }
                    }
                }

                request(
                        "DELETE",
                        SUPABASE_URL +
                                "/rest/v1/players?player_id=eq." +
                                URLEncoder.encode(
                                        playerId,
                                        "UTF-8"
                                ),
                        null
                );

                runOnUiThread(() -> {

                    roomId = "";

                    roomCode = "";

                    playerName = "";

                    isHost = false;

                    score = 0;

                    questionIndex = 0;

                    showHome();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        toast(
                                "تعذر مغادرة الغرفة"
                        )
                );
            }

        }).start();
    }

    // ============================================================
    // GAME STATE
    // ============================================================

    private JSONObject getGameState()
            throws Exception {

        JSONArray data =
                new JSONArray(
                        request(
                                "GET",
                                SUPABASE_URL +
                                        "/rest/v1/game_state" +
                                        "?room_code=eq." +
                                        URLEncoder.encode(
                                                roomCode,
                                                "UTF-8"
                                        ) +
                                        "&select=*",
                                null
                        )
                );

        if (data.length() == 0) {

            throw new Exception(
                    "game state not found"
            );
        }

        return data.getJSONObject(0);
    }

    private void setGameState(
            int index,
            String status
    ) throws Exception {

        JSONObject update =
                new JSONObject();

        update.put(
                "question_index",
                index
        );

        update.put(
                "status",
                status
        );

        update.put(
                "updated_at",
                getCurrentTimestamp()
        );

        request(
                "PATCH",
                SUPABASE_URL +
                        "/rest/v1/game_state?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ),
                update.toString()
        );
    }

    private String getCurrentTimestamp() {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        Locale.US
                );

        return format.format(
                new Date()
        );
    }

    private void setRoomStatus(
            String status
    ) throws Exception {

        JSONObject update =
                new JSONObject();

        update.put(
                "status",
                status
        );

        request(
                "PATCH",
                SUPABASE_URL +
                        "/rest/v1/rooms?id=eq." +
                        URLEncoder.encode(
                                roomId,
                                "UTF-8"
                        ),
                update.toString()
        );
    }

    // ============================================================
    // CHAT
    // ============================================================

    private void showChat() {

        LinearLayout root =
                root();

        root.addView(
                title("💬 Chat")
        );

        root.addView(
                subtitle(
                        "احكوا مع بعض أثناء اللعب."
                )
        );

        LinearLayout messages =
                card();

        root.addView(messages);

        EditText message =
                input(
                        "اكتب رسالة..."
                );

        message.setSingleLine(false);

        message.setMinHeight(
                dp(65)
        );

        root.addView(message);

        Button send =
                button("📤 إرسال");

        send.setOnClickListener(v -> {

            String text =
                    message.getText()
                            .toString()
                            .trim();

            if (text.isEmpty()) {
                return;
            }

            send.setEnabled(false);

            new Thread(() -> {

                try {

                    JSONObject object =
                            new JSONObject();

                    object.put(
                            "room_id",
                            roomId
                    );

                    object.put(
                            "player_name",
                            playerName
                    );

                    object.put(
                            "message",
                            text
                    );

                    request(
                            "POST",
                            SUPABASE_URL +
                                    "/rest/v1/messages",
                            object.toString()
                    );

                    runOnUiThread(() -> {

                        message.setText("");

                        send.setEnabled(true);

                        loadMessages(
                                messages
                        );
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        send.setEnabled(true);

                        toast(
                                "فشل إرسال الرسالة"
                        );
                    });
                }

            }).start();
        });

        root.addView(send);

        Button back =
                button("رجوع");

        back.setOnClickListener(v -> {

            if (roomCode.isEmpty()) {

                showHome();

            } else {

                showLobby();
            }
        });

        root.addView(back);

        loadMessages(messages);

        setScreen(
                scroll(root),
                "chat"
        );

        startChatPolling(
                messages
        );
    }

    private void loadMessages(
            LinearLayout container
    ) {

        new Thread(() -> {

            try {

                JSONArray data =
                        new JSONArray(
                                request(
                                        "GET",
                                        SUPABASE_URL +
                                                "/rest/v1/messages" +
                                                "?room_id=eq." +
                                                URLEncoder.encode(
                                                        roomId,
                                                        "UTF-8"
                                                ) +
                                                "&select=*&order=created_at.asc&limit=100",
                                        null
                                )
                        );

                runOnUiThread(() -> {

                    container.removeAllViews();

                    if (data.length() == 0) {

                        TextView empty =
                                new TextView(this);

                        empty.setText(
                                "ما فما حتى رسالة 👀"
                        );

                        empty.setTextColor(
                                secondaryTextColor
                        );

                        empty.setGravity(
                                Gravity.CENTER
                        );

                        container.addView(
                                empty
                        );

                        return;
                    }

                    for (
                            int i = 0;
                            i < data.length();
                            i++
                    ) {

                        JSONObject m =
                                data.optJSONObject(i);

                        if (m == null) {
                            continue;
                        }

                        String name =
                                m.optString(
                                        "player_name",
                                        "?"
                                );

                        String text =
                                m.optString(
                                        "message",
                                        ""
                                );

                        TextView row =
                                new TextView(this);

                        row.setText(
                                "👤 " +
                                        name +
                                        "\n" +
                                        "   " +
                                        text
                        );

                        row.setTextColor(
                                textColor
                        );

                        row.setTextSize(15);

                        row.setPadding(
                                0,
                                dp(8),
                                0,
                                dp(8)
                        );

                        container.addView(row);
                    }
                });

            } catch (Exception ignored) {
            }

        }).start();
    }

    private void startChatPolling(
            LinearLayout messages
    ) {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!"chat".equals(
                                currentScreen
                        )) {

                            return;
                        }

                        loadMessages(
                                messages
                        );

                        handler.postDelayed(
                                this,
                                2500
                        );
                    }
                },
                2500
        );
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    private void showSettings() {

        LinearLayout root =
                root();

        root.addView(
                title("⚙️ الإعدادات")
        );

        LinearLayout appearance =
                card();

        TextView appearanceText =
                new TextView(this);

        appearanceText.setText(
                "🎨 المظهر\n\n" +
                        (
                                darkMode
                                        ? "🌙 الوضع الداكن مفعل"
                                        : "☀️ الوضع الفاتح مفعل"
                        )
        );

        appearanceText.setTextColor(
                textColor
        );

        appearanceText.setTextSize(18);

        appearance.addView(
                appearanceText
        );

        root.addView(
                appearance
        );

        Button dark =
                button(
                        darkMode
                                ? "☀️ الوضع الفاتح"
                                : "🌙 الوضع الداكن"
                );

        dark.setOnClickListener(
                v -> toggleDarkMode()
        );

        root.addView(dark);

        LinearLayout audio =
                card();

        TextView audioText =
                new TextView(this);

        audioText.setText(
                "🎵 الصوت\n\n" +
                        (
                                musicEnabled
                                        ? "🎵 الموسيقى مفعلة"
                                        : "🔇 الموسيقى متوقفة"
                        ) +
                        "\n" +
                        (
                                soundEnabled
                                        ? "🔊 المؤثرات مفعلة"
                                        : "🔇 المؤثرات متوقفة"
                        )
        );

        audioText.setTextColor(
                textColor
        );

        audioText.setTextSize(18);

        audio.addView(
                audioText
        );

        root.addView(audio);

        Button music =
                button(
                        musicEnabled
                                ? "🔇 إيقاف الموسيقى"
                                : "🎵 تشغيل الموسيقى"
                );

        music.setOnClickListener(
                v -> toggleMusic()
        );

        root.addView(music);

        Button sound =
                button(
                        soundEnabled
                                ? "🔇 إيقاف المؤثرات"
                                : "🔊 تشغيل المؤثرات"
                );

        sound.setOnClickListener(
                v -> toggleSound()
        );

        root.addView(sound);

        LinearLayout about =
                card();

        TextView aboutText =
                new TextView(this);

        aboutText.setText(
                "🎮 GuessUs\n\n" +
                        "لعبة توقع إجابات صاحبك.\n" +
                        "👥 لاعبان\n" +
                        "🎯 توقعات\n" +
                        "🏆 نقاط\n" +
                        "💬 Chat\n" +
                        "🎵 موسيقى\n\n" +
                        "الإصدار 1.0"
        );

        aboutText.setTextColor(
                textColor
        );

        aboutText.setTextSize(16);

        about.addView(
                aboutText
        );

        root.addView(about);

        Button back =
                button("رجوع");

        back.setOnClickListener(
                v -> showHome()
        );

        root.addView(back);

        setScreen(
                scroll(root),
                "settings"
        );
    }

    // ============================================================
    // ANSWER COMPARISON
    // ============================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String s =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        s = s.replace(
                "أ",
                "ا"
        );

        s = s.replace(
                "إ",
                "ا"
        );

        s = s.replace(
                "آ",
                "ا"
        );

        s = s.replace(
                "ة",
                "ه"
        );

        s = s.replace(
                "ى",
                "ي"
        );

        s = s.replace(
                "ؤ",
                "و"
        );

        s = s.replace(
                "ئ",
                "ي"
        );

        s = s.replaceAll(
                "[ًٌٍَُِّْـ]",
                ""
        );

        s = s.replaceAll(
                "[^\\p{L}\\p{N}\\s]",
                ""
        );

        s = s.replaceAll(
                "\\s+",
                " "
        );

        return s.trim();
    }

    private boolean similarAnswer(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {

            return false;
        }

        String x =
                normalize(a);

        String y =
                normalize(b);

        if (x.isEmpty() ||
                y.isEmpty()) {

            return false;
        }

        if (x.equals(y)) {
            return true;
        }

        if (x.length() >= 3 &&
                y.length() >= 3) {

            if (x.contains(y) ||
                    y.contains(x)) {

                return true;
            }
        }

        String[] wordsX =
                x.split(" ");

        String[] wordsY =
                y.split(" ");

        int matches = 0;

        for (String wx : wordsX) {

            for (String wy : wordsY) {

                if (wx.equals(wy) &&
                        wx.length() >= 3) {

                    matches++;
                }
            }
        }

        return matches > 0;
    }

    // ============================================================
    // NETWORK
    // ============================================================

    private String request(
            String method,
            String urlString,
            String body
    ) throws Exception {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    method
            );

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    15000
            );

            connection.setRequestProperty(
                    "apikey",
                    SUPABASE_KEY
            );

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " +
                            SUPABASE_KEY
            );

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Prefer",
                    "return=representation"
            );

            if (body != null &&
                    !method.equals("GET")) {

                connection.setDoOutput(
                        true
                );

                byte[] data =
                        body.getBytes(
                                StandardCharsets.UTF_8
                        );

                OutputStream output =
                        connection
                                .getOutputStream();

                output.write(data);

                output.flush();

                output.close();
            }

            int responseCode =
                    connection.getResponseCode();

            InputStream input;

            if (responseCode >= 200 &&
                    responseCode < 400) {

                input =
                        connection
                                .getInputStream();

            } else {

                input =
                        connection
                                .getErrorStream();
            }

            if (input == null) {

                throw new Exception(
                        "HTTP " +
                                responseCode
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line =
                            reader.readLine())
                            != null
            ) {

                result.append(line);
            }

            reader.close();

            if (responseCode < 200 ||
                    responseCode >= 300) {

                throw new Exception(
                        "HTTP " +
                                responseCode +
                                ": " +
                                result
                );
            }

            return result.toString();

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }
    }
