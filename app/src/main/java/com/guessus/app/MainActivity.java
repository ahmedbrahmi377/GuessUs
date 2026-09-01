package com.guessus.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    // =========================
    // SUPABASE / GAME VARIABLES
    // =========================

    private LinearLayout content;
    private Handler handler = new Handler();

    private String roomCode = "";
    private String roomId = "";
    private String playerName = "";

    private int score = 0;
    private int round = 0;
    private int questionIndex = 0;

    private boolean dark = false;
    private boolean isHost = false;
    private boolean answerSent = false;
    private boolean predictionSent = false;

    private Runnable syncRunnable;

    // =========================
    // QUESTIONS
    // =========================

    private final String[] questions = {

        "من أكثر واحد يضحك في وقت غلط؟",

        "لو ربحت مليون، شنو أول حاجة تعملها؟",

        "من أكثر واحد يتأخر على المواعيد؟",

        "لو تسافر غدوة، وين تمشي؟",

        "شنو أكثر شيء تحب تغيره في شخصيتك؟",

        "من أكثر واحد يفهمك من نظرة؟",

        "شنو أكثر حاجة تفرحك بسرعة؟",

        "من أكثر واحد ممكن ينسى حاجة مهمة؟",

        "من أكثر واحد يحب المغامرات؟",

        "من أكثر واحد مستحيل تعرف شنو يفكر؟",

        "لو عندك يوم كامل بدون مسؤوليات، شنو تعمل؟",

        "من أكثر واحد يقدر يعيش أسبوع بدون هاتف؟",

        "شنو أكثر موقف يضحكك كل مرة تتذكره؟",

        "من أكثر واحد ممكن يعمل قرار فجأة؟",

        "لو تختار قوة خارقة، شنو تختار؟"
    };

    // =========================
    // COLORS
    // =========================

    private int background() {
        return dark
                ? Color.rgb(20, 20, 25)
                : Color.rgb(250, 250, 250);
    }

    private int foreground() {
        return dark
                ? Color.WHITE
                : Color.rgb(25, 25, 25);
    }

    private int secondary() {
        return dark
                ? Color.rgb(190, 190, 200)
                : Color.rgb(90, 90, 90);
    }

    // =========================
    // ACTIVITY
    // =========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        home();
    }

    @Override
    protected void onDestroy() {
        stopSync();
        super.onDestroy();
    }

    // =========================
    // UI HELPERS
    // =========================

    private TextView text(String value, int size) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(foreground());
        t.setPadding(20, 16, 20, 16);

        return t;
    }

    private TextView title(String value) {

        TextView t = text(value, 30);

        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, Typeface.BOLD);

        return t;
    }

    private Button button(String value) {

        Button b = new Button(this);

        b.setText(value);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTextColor(foreground());

        return b;
    }

    private void base() {

        stopSync();

        content = new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                18, 18, 18, 18
        );

        content.setBackgroundColor(
                background()
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(content);

        setContentView(scroll);
    }

    private void space() {

        Space s = new Space(this);

        content.addView(
                s,
                new LinearLayout.LayoutParams(
                        1,
                        12
                )
        );
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================
    // HOME
    // =========================

    private void home() {

        base();

        space();

        content.addView(
                title("GuessUs 🎮")
        );

        TextView sub =
                text(
                        "أسئلة • توقعات • أصدقاء\n" +
                        "اللعبة التي تختبر مدى معرفتك بأصحابك!",
                        18
                );

        sub.setGravity(Gravity.CENTER);

        content.addView(sub);

        space();

        Button create =
                button("🏠  Create Room");

        Button join =
                button("🔑  Join Room");

        Button settings =
                button("⚙️  Settings");

        content.addView(create);
        content.addView(join);
        content.addView(settings);

        space();

        TextView info =
                text(
                        "حتى 6 لاعبين • أونلاين • Chat • نقاط",
                        15
                );

        info.setGravity(Gravity.CENTER);

        content.addView(info);

        create.setOnClickListener(
                v -> createScreen()
        );

        join.setOnClickListener(
                v -> joinScreen()
        );

        settings.setOnClickListener(
                v -> settings()
        );
    }

    // =========================
    // CREATE ROOM SCREEN
    // =========================

    private void createScreen() {

        base();

        content.addView(
                title("🏠 Create Room")
        );

        space();

        EditText name =
                new EditText(this);

        name.setHint("اكتب اسمك");
        name.setTextColor(foreground());
        name.setHintTextColor(secondary());

        content.addView(name);

        space();

        Button create =
                button("✨ إنشاء الغرفة");

        Button back =
                button("← Back");

        content.addView(create);
        content.addView(back);

        create.setOnClickListener(v -> {

            String nameValue =
                    name.getText()
                            .toString()
                            .trim();

            if (nameValue.isEmpty()) {

                toast("اكتب اسمك أولًا");

                return;
            }

            playerName =
                    nameValue;

            createRoom();
        });

        back.setOnClickListener(
                v -> home()
        );
    }

    // =========================
    // CREATE ROOM
    // =========================

    private void createRoom() {

        toast("جاري إنشاء الغرفة...");

        new Thread(() -> {

            try {

                String code;

                // Generate a unique 4-digit room code
                while (true) {

                    code =
                            String.valueOf(
                                    1000 +
                                    new Random()
                                            .nextInt(9000)
                            );

                    String checkUrl =
                            BuildConfig.SUPABASE_URL +
                            "/rest/v1/rooms" +
                            "?code=eq." +
                            code +
                            "&select=id";

                    JSONArray existing =
                            new JSONArray(
                                    request(
                                            "GET",
                                            checkUrl,
                                            null,
                                            false
                                    )
                            );

                    if (existing.length() == 0) {
                        break;
                    }
                }

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

                String response =
                        request(
                                "POST",
                                BuildConfig.SUPABASE_URL +
                                "/rest/v1/rooms",
                                room.toString(),
                                true
                        );

                JSONArray result =
                        new JSONArray(response);

                if (result.length() == 0) {

                    throw new Exception(
                            "Supabase لم يرجع الغرفة"
                    );
                }

                JSONObject created =
                        result.getJSONObject(0);

                roomId =
                        created.getString("id");

                roomCode =
                        created.getString("code");

                isHost = true;

                // Create game state
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
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/game_state",
                        state.toString(),
                        false
                );

                // Add host as player
                addPlayer();

                runOnUiThread(
                        () -> lobby()
                );

            } catch (Exception e) {

                runOnUiThread(
                        () -> toast(
                                "فشل إنشاء الغرفة:\n" +
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    // =========================
    // JOIN SCREEN
    // =========================

    private void joinScreen() {

        base();

        content.addView(
                title("🔑 Join Room")
        );

        space();

        EditText name =
                new EditText(this);

        name.setHint("اكتب اسمك");

        content.addView(name);

        EditText code =
                new EditText(this);

        code.setHint("كود الغرفة - 4 أرقام");
        code.setInputType(2);

        content.addView(code);

        space();

        Button join =
                button("🚪 دخول الغرفة");

        Button back =
                button("← Back");

        content.addView(join);
        content.addView(back);

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
            roomCode = c;

            joinRoom();
        });

        back.setOnClickListener(
                v -> home()
        );
    }

    // =========================
    // JOIN ROOM
    // =========================

    private void joinRoom() {

        toast("جاري البحث عن الغرفة...");

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/rooms" +
                        "?code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=id,code,status";

                JSONArray rooms =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                if (rooms.length() == 0) {

                    throw new Exception(
                            "الغرفة غير موجودة"
                    );
                }

                JSONObject room =
                        rooms.getJSONObject(0);

                roomId =
                        room.getString("id");

                String status =
                        room.optString(
                                "status",
                                "waiting"
                        );

                if (status.equals("playing")) {

                    throw new Exception(
                            "اللعبة بدأت بالفعل"
                    );
                }

                // Count players
                String playersUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=id";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        playersUrl,
                                        null,
                                        false
                                )
                        );

                if (players.length() >= 6) {

                    throw new Exception(
                            "الغرفة ممتلئة"
                    );
                }

                isHost = false;

                addPlayer();

                runOnUiThread(
                        () -> lobby()
                );

            } catch (Exception e) {

                runOnUiThread(
                        () -> toast(
                                "تعذر الدخول:\n" +
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    // =========================
    // ADD PLAYER
    // =========================

    private void addPlayer()
            throws Exception {

        JSONObject player =
                new JSONObject();

        player.put(
                "room_id",
                roomId
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
                playerName +
                "_" +
                System.currentTimeMillis()
        );

        player.put(
                "room_code",
                roomCode
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
                BuildConfig.SUPABASE_URL +
                "/rest/v1/players",
                player.toString(),
                false
        );
    }

    // =========================
    // LOBBY
    // =========================

    private void lobby() {

        base();

        content.addView(
                title(
                        "🎮 غرفة " +
                        roomCode
                )
        );

        space();

        TextView playersView =
                text(
                        "جاري تحميل اللاعبين...",
                        18
                );

        content.addView(
                playersView
        );

        space();

        Button ready =
                button("🟢 Ready");

        Button start =
                button(
                        isHost
                        ? "🚀 Start Game"
                        : "⏳ بانتظار الـHost"
                );

        Button chat =
                button("💬 Chat");

        Button leave =
                button("🚪 Leave Room");

        content.addView(ready);
        content.addView(start);
        content.addView(chat);
        content.addView(leave);

        loadPlayers(playersView);

        // Refresh lobby every 2 seconds
        startSync(
                () -> {

                    if (!isFinishing()) {
                        loadPlayers(playersView);
                    }
                }
        );

        ready.setOnClickListener(
                v -> setReady()
        );

        start.setOnClickListener(v -> {

            if (!isHost) {

                toast(
                        "فقط صاحب الغرفة يستطيع البدء"
                );

                return;
            }

            startGame();
        });

        chat.setOnClickListener(
                v -> chat()
        );

        leave.setOnClickListener(
                v -> leaveRoom()
        );
    }

    // =========================
    // LOAD PLAYERS
    // =========================

    private void loadPlayers(
            TextView view
    ) {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=name,score,ready,is_host" +
                        "&order=joined_at.asc";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                StringBuilder text =
                        new StringBuilder();

                text.append(
                        "👥 Players "
                );

                text.append(
                        players.length()
                );

                text.append(
                        "/6\n\n"
                );

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    JSONObject p =
                            players.getJSONObject(i);

                    text.append(
                            i + 1
                    );

                    text.append(". ");

                    text.append(
                            p.optString(
                                    "name"
                            )
                    );

                    if (
                            p.optBoolean(
                                    "is_host"
                            )
                    ) {
                        text.append(" 👑");
                    }

                    if (
                            p.optBoolean(
                                    "ready"
                            )
                    ) {
                        text.append(" 🟢");
                    } else {
                        text.append(" ⚪");
                    }

                    text.append("\n");
                }

                runOnUiThread(
                        () -> view.setText(
                                text.toString()
                        )
                );

            } catch (Exception e) {

                runOnUiThread(
                        () -> view.setText(
                                "تعذر تحميل اللاعبين"
                        )
                );
            }

        }).start();
    }

    // =========================
    // READY
    // =========================

    private void setReady() {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&name=eq." +
                        URLEncoder.encode(
                                playerName,
                                "UTF-8"
                        );

                JSONObject data =
                        new JSONObject();

                data.put(
                        "ready",
                        true
                );

                request(
                        "PATCH",
                        url,
                        data.toString(),
                        false
                );

                runOnUiThread(
                        () -> toast(
                                "🟢 أنت Ready!"
                        )
                );

            } catch (Exception e) {

                runOnUiThread(
                        () -> toast(
                                "فشل Ready"
                        )
                );
            }

        }).start();
    }

    // =========================
    // START GAME
    // =========================

    private void startGame() {

        new Thread(() -> {

            try {

                String playersUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=name,ready";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        playersUrl,
                                        null,
                                        false
                                )
                        );

                if (players.length() < 1) {

                    throw new Exception(
                            "لا يوجد لاعبون"
                    );
                }

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    if (
                            !players
                                    .getJSONObject(i)
                                    .optBoolean(
                                            "ready"
                                    )
                    ) {

                        throw new Exception(
                                "ليس كل اللاعبين Ready"
                        );
                    }
                }

                // Change room status
                JSONObject roomData =
                        new JSONObject();

                roomData.put(
                        "status",
                        "playing"
                );

                String roomUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/rooms" +
                        "?code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        );

                request(
                        "PATCH",
                        roomUrl,
                        roomData.toString(),
                        false
                );

                // Update game state
                updateGameState(
                        0,
                        "answering"
                );

                round = 0;
                questionIndex = 0;

                runOnUiThread(
                        () -> answerScreen()
                );

            } catch (Exception e) {

                runOnUiThread(
                        () -> toast(
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    // =========================
    // ANSWER SCREEN
    // =========================

    private void answerScreen() {

        stopSync();

        answerSent = false;

        base();

        content.addView(
                title(
                        "❓ الجولة " +
                        (round + 1)
                )
        );

        TextView question =
                text(
                        questions[
                                questionIndex %
                                questions.length
                        ],
                        24
                );

        question.setGravity(
                Gravity.CENTER
        );

        question.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(
                question
        );

        space();

        content.addView(
                text(
                        "اكتب إجابتك، وبعدها سنحاول معرفة من يعرفك أكثر 🎯",
                        17
                )
        );

        EditText answer =
                new EditText(this);

        answer.setHint(
                "اكتب إجابتك هنا..."
        );

        answer.setTextColor(
                foreground()
        );

        answer.setHintTextColor(
                secondary()
        );

        answer.setMinLines(2);

        content.addView(answer);

        space();

        Button send =
                button(
                        "✓ إرسال الإجابة"
                );

        Button chat =
                button(
                        "💬 Chat"
                );

        content.addView(send);
        content.addView(chat);

        send.setOnClickListener(v -> {

            if (answerSent) {

                toast(
                        "أرسلت إجابتك بالفعل"
                );

                return;
            }

            String value =
                    answer.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {

                toast(
                        "اكتب إجابة أولًا"
                );

                return;
            }

            sendAnswer(value);
        });

        chat.setOnClickListener(
                v -> chat()
        );
    }

    // =========================
    // SEND ANSWER
    // =========================

    private void sendAnswer(
            String answer
    ) {

        answerSent = true;

        new Thread(() -> {

            try {

                // Remove previous answer
                String deleteUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/round_answers" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&round=eq." +
                        round +
                        "&player_name=eq." +
                        URLEncoder.encode(
                                playerName,
                                "UTF-8"
                        );

                request(
                        "DELETE",
                        deleteUrl,
                        null,
                        false
                );

                JSONObject data =
                        new JSONObject();

                data.put(
                        "room_code",
                        roomCode
                );

                data.put(
                        "round",
                        round
                );

                data.put(
                        "player_name",
                        playerName
                );

                data.put(
                        "answer",
                        answer
                );

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/round_answers",
                        data.toString(),
                        false
                );

                runOnUiThread(
                        () -> waitingForAnswers()
                );

            } catch (Exception e) {

                answerSent = false;

                runOnUiThread(
                        () -> toast(
                                "فشل إرسال الإجابة:\n" +
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    // =========================
    // WAIT FOR ANSWERS
    // =========================

    private void waitingForAnswers() {

        base();

        content.addView(
                title(
                        "⏳ تم إرسال إجابتك!"
                )
        );

        content.addView(
                text(
                        "ننتظر بقية اللاعبين...",
                        19
                )
        );

        TextView count =
                text(
                        "0 / 0",
                        24
                );

        count.setGravity(
                Gravity.CENTER
        );

        content.addView(count);

        Button chat =
                button("💬 Chat");

        content.addView(chat);

        chat.setOnClickListener(
                v -> chat()
        );

        startSync(
                () -> checkAnswers(count)
        );
    }

    // =========================
    // CHECK ANSWERS
    // =========================

    private void checkAnswers(
            TextView counter
    ) {

        new Thread(() -> {

            try {

                String playersUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=name";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        playersUrl,
                                        null,
                                        false
                                )
                        );

                String answersUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/round_answers" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&round=eq." +
                        round +
                        "&select=player_name";

                JSONArray answers =
                        new JSONArray(
                                request(
                                        "GET",
                                        answersUrl,
                                        null,
                                        false
                                )
                        );

                int total =
                        players.length();

                int submitted =
                        answers.length();

                runOnUiThread(
                        () -> counter.setText(
                                submitted +
                                " / " +
                                total +
                                " لاعبين أجابوا"
                        )
                );

                if (
                        total > 0 &&
                        submitted >= total
                ) {

                    updateGameState(
                            questionIndex,
                            "predicting"
                    );

                    runOnUiThread(
                            () -> predictionScreen()
                    );
                }

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================
    // PREDICTION SCREEN
    // =========================

    private void predictionScreen() {

        stopSync();

        predictionSent = false;

        base();

        content.addView(
                title(
                        "🎯 توقع إجابة صاحبك"
                )
        );

        content.addView(
                text(
                        "اختار لاعبًا واكتب الإجابة التي تعتقد أنه كتبها.",
                        18
                )
        );

        space();

        Spinner spinner =
                new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_spinner_dropdown_item
                );

        spinner.setAdapter(adapter);

        content.addView(spinner);

        EditText prediction =
                new EditText(this);

        prediction.setHint(
                "شنو تتوقع أنه كتب؟"
        );

        prediction.setTextColor(
                foreground()
        );

        prediction.setHintTextColor(
                secondary()
        );

        prediction.setMinLines(2);

        content.addView(prediction);

        space();

        Button predict =
                button(
                        "🎯 Predict"
                );

        Button skip =
                button(
                        "⏭ Skip"
                );

        Button chat =
                button(
                        "💬 Chat"
                );

        content.addView(predict);
        content.addView(skip);
        content.addView(chat);

        loadPredictionPlayers(
                spinner,
                adapter
        );

        predict.setOnClickListener(v -> {

            if (predictionSent) {

                toast(
                        "أرسلت توقعك بالفعل"
                );

                return;
            }

            if (
                    spinner.getSelectedItem()
                    == null
            ) {

                toast(
                        "اختار لاعبًا"
                );

                return;
            }

            String target =
                    spinner
                            .getSelectedItem()
                            .toString();

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

            submitPrediction(
                    target,
                    predicted
            );
        });

        skip.setOnClickListener(
                v -> nextRound()
        );

        chat.setOnClickListener(
                v -> chat()
        );
    }

    // =========================
    // LOAD TARGET PLAYERS
    // =========================

    private void loadPredictionPlayers(
            Spinner spinner,
            ArrayAdapter<String> adapter
    ) {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=name";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    String name =
                            players
                                    .getJSONObject(i)
                                    .optString(
                                            "name"
                                    );

                    if (
                            !name.equals(
                                    playerName
                            )
                    ) {

                        adapter.add(name);
                    }
                }

                runOnUiThread(
                        () -> spinner
                                .setAdapter(
                                        adapter
                                )
                );

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================
    // SUBMIT PREDICTION
    // =========================

    private void submitPrediction(
            String target,
            String predicted
    ) {

        predictionSent = true;

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/round_answers" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&round=eq." +
                        round +
                        "&player_name=eq." +
                        URLEncoder.encode(
                                target,
                                "UTF-8"
                        ) +
                        "&select=answer";

                JSONArray answers =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                if (answers.length() == 0) {

                    throw new Exception(
                            "إجابة اللاعب غير موجودة"
                    );
                }

                String actual =
                        answers
                                .getJSONObject(0)
                                .optString(
                                        "answer"
                                );

                boolean correct =
                        actual
                                .trim()
                                .equalsIgnoreCase(
                                        predicted.trim()
                                );

                int points =
                        correct
                        ? 5
                        : 0;

                JSONObject prediction =
                        new JSONObject();

                prediction.put(
                        "room_code",
                        roomCode
                );

                prediction.put(
                        "round",
                        round
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
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/predictions",
                        prediction.toString(),
                        false
                );

                if (correct) {

                    score += 5;

                    updateScore();
                }

                final int earned =
                        points;

                final boolean right =
                        correct;

                runOnUiThread(
                        () -> predictionResult(
                                target,
                                predicted,
                                actual,
                                right,
                                earned
                        )
                );

            } catch (Exception e) {

                predictionSent = false;

                runOnUiThread(
                        () -> toast(
                                "تعذر التوقع:\n" +
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    // =========================
    // PREDICTION RESULT
    // =========================

    private void predictionResult(
            String target,
            String predicted,
            String actual,
            boolean correct,
            int points
    ) {

        base();

        content.addView(
                title(
                        correct
                        ? "🎉 توقع صحيح!"
                        : "❌ توقع خاطئ"
                )
        );

        content.addView(
                text(
                        "👤 اللاعب: " +
                        target +
                        "\n\n" +
                        "🎯 توقعك: " +
                        predicted +
                        "\n\n" +
                        "💬 إجابته: " +
                        actual +
                        "\n\n" +
                        "⭐ النقاط: +" +
                        points +
                        "\n" +
                        "🏆 مجموعك: " +
                        score,
                        20
                )
        );

        if (correct) {

            content.addView(
                    text(
                            "🔥 +3 نقاط للتوقع الصحيح\n" +
                            "✨ +2 Bonus للمطابقة\n" +
                            "💥 المجموع +5!",
                            18
                    )
            );
        }

        Button next =
                button(
                        "➡ Next Question"
                );

        Button chat =
                button(
                        "💬 Chat"
                );

        content.addView(next);
        content.addView(chat);

        next.setOnClickListener(
                v -> nextRound()
        );

        chat.setOnClickListener(
                v -> chat()
        );
    }

    // =========================
    // UPDATE SCORE
    // =========================

    private void updateScore() {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&name=eq." +
                        URLEncoder.encode(
                                playerName,
                                "UTF-8"
                        );

                JSONObject data =
                        new JSONObject();

                data.put(
                        "score",
                        score
                );

                request(
                        "PATCH",
                        url,
                        data.toString(),
                        false
                );

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================
    // NEXT ROUND
    // =========================

    private void nextRound() {

        round++;

        questionIndex++;

        if (
                questionIndex >=
                questions.length
        ) {

            leaderboard();

            return;
        }

        if (isHost) {

            updateGameState(
                    questionIndex,
                    "answering"
            );
        }

        answerScreen();
    }

    // =========================
    // LEADERBOARD
    // =========================

    private void leaderboard() {

        base();

        content.addView(
                title(
                        "🏆 النتائج النهائية"
                )
        );

        TextView board =
                text(
                        "جاري تحميل الترتيب...",
                        20
                );

        content.addView(board);

        loadLeaderboard(board);

        Button again =
                button(
                        "🔄 Play Again"
                );

        Button room =
                button(
                        "🏠 Back to Room"
                );

        content.addView(again);
        content.addView(room);

        again.setOnClickListener(
                v -> {

                    round = 0;
                    questionIndex = 0;
                    score = 0;

                    resetScores();

                    if (isHost) {
                        updateGameState(
                                0,
                                "answering"
                        );
                    }

                    answerScreen();
                }
        );

        room.setOnClickListener(
                v -> lobby()
        );
    }

    // =========================
    // LOAD LEADERBOARD
    // =========================

    private void loadLeaderboard(
            TextView board
    ) {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=name,score" +
                        "&order=score.desc";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    JSONObject p =
                            players
                                    .getJSONObject(i);

                    result.append(
                            i + 1
                    );

                    result.append(
                            ". "
                    );

                    result.append(
                            p.optString(
                                    "name"
                            )
                    );

                    result.append(
                            " — "
                    );

                    result.append(
                            p.optInt(
                                    "score"
                            )
                    );

                    result.append(
                            " نقطة\n"
                    );
                }

                runOnUiThread(
                        () -> board.setText(
                                result.toString()
                        )
                );

            } catch (Exception e) {

                runOnUiThread(
                        () -> board.setText(
                                "تعذر تحميل النتائج"
                        )
                );
            }

        }).start();
    }

    // =========================
    // RESET SCORES
    // =========================

    private void resetScores() {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        );

                JSONObject data =
                        new JSONObject();

                data.put(
                        "score",
                        0
                );

                data.put(
                        "ready",
                        false
                );

                request(
                        "PATCH",
                        url,
                        data.toString(),
                        false
                );

                score = 0;

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================
    // CHAT
    // =========================

    private void chat() {

        base();

        content.addView(
                title("💬 Chat")
        );

        TextView messages =
                text(
                        "جاري تحميل الرسائل...",
                        17
                );

        content.addView(messages);

        EditText input =
                new EditText(this);

        input.setHint(
                "اكتب رسالة..."
        );

        input.setTextColor(
                foreground()
        );

        input.setHintTextColor(
                secondary()
        );

        content.addView(input);

        Button send =
                button("📨 Send");

        Button back =
                button("← Back");

        content.addView(send);
        content.addView(back);

        loadMessages(messages);

        startSync(
                () -> loadMessages(messages)
        );

        send.setOnClickListener(v -> {

            String message =
                    input.getText()
                            .toString()
                            .trim();

            if (message.isEmpty()) {
                return;
            }

            sendMessage(message);

            input.setText("");
        });

        back.setOnClickListener(
                v -> lobby()
        );
    }

    // =========================
    // SEND MESSAGE
    // =========================

    private void sendMessage(
            String message
    ) {

        new Thread(() -> {

            try {

                JSONObject data =
                        new JSONObject();

                data.put(
                        "room_id",
                        roomId
                );

                data.put(
                        "player_name",
                        playerName
                );

                data.put(
                        "message",
                        message
                );

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/messages",
                        data.toString(),
                        false
                );

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================
    // LOAD MESSAGES
    // =========================

    private void loadMessages(
            TextView view
    ) {

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/messages" +
                        "?room_id=eq." +
                        URLEncoder.encode(
                                roomId,
                                "UTF-8"
                        ) +
                        "&select=player_name,message,created_at" +
                        "&order=created_at.asc";

                JSONArray messages =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                StringBuilder output =
                        new StringBuilder();

                if (messages.length() == 0) {

                    output.append(
                            "لا توجد رسائل بعد 👋"
                    );
                }

                for (
                        int i = 0;
                        i < messages.length();
                        i++
                ) {

                    JSONObject m =
                            messages
                                    .getJSONObject(i);

                    output.append(
                            "👤 "
                    );

                    output.append(
                            m.optString(
                                    "player_name"
                            )
                    );

                    output.append(
                            "\n"
                    );

                    output.append(
                            m.optString(
                                    "message"
                            )
                    );

                    output.append(
                            "\n\n"
                    );
                }

                runOnUiThread(
                        () -> view.setText(
                                output.toString()
                        )
                );

            } catch (Exception ignored) {

                runOnUiThread(
                        () -> view.setText(
                                "تعذر تحميل الرسائل"
                        )
                );
            }

        }).start();
    }

    // =========================
    // SETTINGS
    // =========================

    private void settings() {

        base();

        content.addView(
                title("⚙️ Settings")
        );

        space();

        Switch darkSwitch =
                new Switch(this);

        darkSwitch.setText(
                "🌙 Dark Mode"
        );

        darkSwitch.setTextColor(
                foreground()
        );

        darkSwitch.setChecked(
                dark
        );

        content.addView(
                darkSwitch
        );

        space();

        content.addView(
                text(
                        "🌐 Language\n" +
                        "العربية • Français • English",
                        17
                )
        );

        space();

        content.addView(
                text(
                        "🎮 GuessUs\n" +
                        "Online party game",
                        15
                )
        );

        Button back =
                button("← Back");

        content.addView(back);

        darkSwitch.setOnCheckedChangeListener(
                (buttonView, checked) -> {

                    dark = checked;

                    settings();
                }
        );

        back.setOnClickListener(
                v -> home()
        );
    }

    // =========================
    // LEAVE ROOM
    // =========================

    private void leaveRoom() {

        stopSync();

        new Thread(() -> {

            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&name=eq." +
                        URLEncoder.encode(
                                playerName,
                                "UTF-8"
                        );

                request(
                        "DELETE",
                        url,
                        null,
                        false
                );

            } catch (Exception ignored) {
            }

            runOnUiThread(() -> {

                roomCode = "";
                roomId = "";
                playerName = "";
                score = 0;
                round = 0;
                questionIndex = 0;

                home();
            });

        }).start();
    }

    // =========================
    // GAME STATE
    // =========================

    private void updateGameState(
            int index,
            String status
    ) {

        new Thread(() -> {

            try {

                String find =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/game_state" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=room_code";

                JSONArray rows =
                        new JSONArray(
                                request(
                                        "GET",
                                        find,
                                        null,
                                        false
                                )
                        );

                JSONObject data =
                        new JSONObject();

                data.put(
                        "question_index",
                        index
                );

                data.put(
                        "status",
                        status
                );

                if (rows.length() > 0) {

                    request(
                            "PATCH",
                            BuildConfig.SUPABASE_URL +
                            "/rest/v1/game_state" +
                            "?room_code=eq." +
                            URLEncoder.encode(
                                    roomCode,
                                    "UTF-8"
                            ),
                            data.toString(),
                            false
                    );

                } else {

                    data.put(
                            "room_code",
                            roomCode
                    );

                    request(
                            "POST",
                            BuildConfig.SUPABASE_URL +
                            "/rest/v1/game_state",
                            data.toString(),
                            false
                    );
                }

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================
    // SYNC
    // =========================

    private void startSync(
            Runnable action
    ) {

        stopSync();

        syncRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        action.run();

                        handler.postDelayed(
                                this,
                                2000
                        );
                    }
                };

        handler.post(
                syncRunnable
        );
    }

    private void stopSync() {

        if (syncRunnable != null) {

            handler.removeCallbacks(
                    syncRunnable
            );

            syncRunnable = null;
        }
    }

    // =========================
    // SUPABASE HTTP
    // =========================

    private String request(
            String method,
            String urlString,
            String body,
            boolean returnRepresentation
    ) throws Exception {

        URL url =
                new URL(urlString);

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setRequestMethod(
                method
        );

        connection.setConnectTimeout(
                15000
        );

        connection.setReadTimeout(
                15000
        );

        connection.setRequestProperty(
                "apikey",
                BuildConfig.SUPABASE_KEY
        );

        connection.setRequestProperty(
                "Authorization",
                "Bearer " +
                BuildConfig.SUPABASE_KEY
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
                returnRepresentation
                ? "return=representation"
                : "return=minimal"
        );

        if (body != null) {

            connection.setDoOutput(true);

            OutputStream output =
                    connection.getOutputStream();

            output.write(
                    body.getBytes("UTF-8")
            );

            output.flush();
            output.close();
        }

        int code =
                connection.getResponseCode();

        InputStream stream;

        if (
                code >= 200 &&
                code < 400
        ) {

            stream =
                    connection.getInputStream();

        } else {

            stream =
                    connection.getErrorStream();
        }

        if (stream == null) {

            throw new IOException(
                    "HTTP " + code
            );
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream
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

        connection.disconnect();

        if (
                code < 200 ||
                code >= 300
        ) {

            throw new IOException(
                    "HTTP " +
                    code +
                    ": " +
                    result
            );
        }

        return result.toString();
    }
}
