package com.guessus.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Random;

public class MainActivity extends Activity {

    // =========================================================
    // GAME
    // =========================================================

    private LinearLayout content;
    private final Handler handler = new Handler();

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
    private Runnable timerRunnable;

    private int secondsLeft = 45;

    // =========================================================
    // 50 QUESTIONS
    // =========================================================

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
            "لو تختار قوة خارقة، شنو تختار؟",
            "من أكثر واحد يحب الأكل؟",
            "من أكثر واحد عنده حظ غريب؟",
            "شنو أول شيء تشتريه لو صار عندك فلوس كثيرة؟",
            "من أكثر واحد يقدر يحفظ سر؟",
            "من أكثر واحد يتوتر بسرعة؟",

            "شنو أكثر تطبيق تستعمله؟",
            "من أكثر واحد يحب السهر؟",
            "من أكثر واحد ممكن يضيع في مكان يعرفه؟",
            "شنو أكثر لعبة تحبها؟",
            "من أكثر واحد يرسل رسائل طويلة؟",
            "من أكثر واحد يرد متأخر؟",
            "لو تعيش في مدينة ثانية، شنو تختار؟",
            "من أكثر واحد يحب التصوير؟",
            "شنو أكثر شيء ما تقدر تعيش بدونه؟",
            "من أكثر واحد عنده أفكار مجنونة؟",

            "من أكثر واحد يقدر يقنع الآخرين؟",
            "لو تقدر تتعلم مهارة فورًا، شنو تختار؟",
            "من أكثر واحد يحب الأفلام؟",
            "من أكثر واحد يسمع موسيقى أكثر؟",
            "شنو أكثر شيء يخليك تضحك؟",
            "من أكثر واحد ممكن يصبح مشهورًا؟",
            "من أكثر واحد يحب التحديات؟",
            "شنو أكثر مكان تحب تزوره؟",
            "من أكثر واحد ينسى أين وضع هاتفه؟",
            "من أكثر واحد يحب المفاجآت؟",

            "لو تقدر ترجع ليوم واحد من الماضي، أي يوم تختار؟",
            "من أكثر واحد يعطي نصائح جيدة؟",
            "شنو أكثر شيء يزعجك؟",
            "من أكثر واحد يغير رأيه بسرعة؟",
            "من أكثر واحد يحب المنافسة؟",
            "شنو الشيء الذي تتمنى تتقنه؟",
            "من أكثر واحد يستطيع البقاء هادئًا؟",
            "من أكثر واحد ممكن يضحك الجميع؟",
            "شنو أفضل شيء في أصدقائك؟",
            "من أكثر واحد يعرفك فعلًا؟"
    };

    // =========================================================
    // COLORS
    // =========================================================

    private int background() {
        return dark
                ? Color.rgb(15, 17, 24)
                : Color.rgb(247, 248, 252);
    }

    private int cardColor() {
        return dark
                ? Color.rgb(27, 30, 40)
                : Color.WHITE;
    }

    private int foreground() {
        return dark
                ? Color.WHITE
                : Color.rgb(25, 27, 35);
    }

    private int secondary() {
        return dark
                ? Color.rgb(185, 190, 205)
                : Color.rgb(95, 100, 115);
    }

    private int accent() {
        return Color.rgb(105, 85, 220);
    }

    // =========================================================
    // ACTIVITY
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        home();
    }

    @Override
    protected void onDestroy() {

        stopSync();
        stopTimer();

        super.onDestroy();
    }

    // =========================================================
    // UI
    // =========================================================

    private TextView text(String value, int size) {

        TextView t = new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(foreground());
        t.setPadding(18, 14, 18, 14);

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
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);

        GradientDrawable bg = new GradientDrawable();

        bg.setColor(accent());
        bg.setCornerRadius(28);

        b.setBackground(bg);

        b.setPadding(10, 5, 10, 5);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        58
                );

        params.setMargins(0, 7, 0, 7);

        b.setLayoutParams(params);

        return b;
    }

    private EditText input(String hint) {

        EditText e = new EditText(this);

        e.setHint(hint);
        e.setTextColor(foreground());
        e.setHintTextColor(secondary());
        e.setTextSize(17);
        e.setPadding(20, 10, 20, 10);

        GradientDrawable bg = new GradientDrawable();

        bg.setColor(cardColor());
        bg.setCornerRadius(22);
        bg.setStroke(2, dark
                ? Color.rgb(55, 60, 75)
                : Color.rgb(225, 227, 235));

        e.setBackground(bg);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        62
                );

        params.setMargins(0, 8, 0, 8);

        e.setLayoutParams(params);

        return e;
    }

    private void base() {

        stopSync();
        stopTimer();

        content = new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                18,
                20,
                18,
                25
        );

        content.setBackgroundColor(
                background()
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);
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

    // =========================================================
    // HOME
    // =========================================================

    private void home() {

        base();

        space();

        content.addView(
                title("GuessUs 🎮")
        );

        TextView subtitle =
                text(
                        "هل تعرف أصحابك فعلًا؟\n" +
                        "جاوب، توقّع، واربح النقاط!",
                        19
                );

        subtitle.setGravity(Gravity.CENTER);

        content.addView(subtitle);

        space();

        TextView gameInfo =
                text(
                        "👥 حتى 6 لاعبين\n" +
                        "🎯 Predict\n" +
                        "💬 Chat\n" +
                        "🏆 Leaderboard\n" +
                        "❓ 50 سؤال",
                        17
                );

        gameInfo.setGravity(Gravity.CENTER);

        content.addView(gameInfo);

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

        TextView version =
                text(
                        "GuessUs v2.0",
                        14
                );

        version.setGravity(Gravity.CENTER);

        version.setTextColor(secondary());

        content.addView(version);

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

    // =========================================================
    // CREATE
    // =========================================================

    private void createScreen() {

        base();

        content.addView(
                title("🏠 Create Room")
        );

        space();

        EditText name =
                input("اكتب اسمك");

        content.addView(name);

        space();

        Button create =
                button("✨ إنشاء الغرفة");

        Button back =
                button("← رجوع");

        content.addView(create);
        content.addView(back);

        create.setOnClickListener(v -> {

            String n =
                    name.getText()
                            .toString()
                            .trim();

            if (n.isEmpty()) {

                toast("اكتب اسمك أولًا");
                return;
            }

            playerName = n;

            createRoom();
        });

        back.setOnClickListener(
                v -> home()
        );
    }

    // =========================================================
    // CREATE ROOM
    // =========================================================

    private void createRoom() {

        toast("جاري إنشاء الغرفة...");

        new Thread(() -> {

            try {

                String code;

                while (true) {

                    code =
                            String.valueOf(
                                    1000 +
                                            new Random()
                                                    .nextInt(9000)
                            );

                    String check =
                            BuildConfig.SUPABASE_URL +
                                    "/rest/v1/rooms" +
                                    "?code=eq." +
                                    code +
                                    "&select=id";

                    JSONArray existing =
                            new JSONArray(
                                    request(
                                            "GET",
                                            check,
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

                room.put("code", code);
                room.put("status", "waiting");

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
                            "لم يتم إنشاء الغرفة"
                    );
                }

                JSONObject created =
                        result.getJSONObject(0);

                roomId =
                        created.getString("id");

                roomCode =
                        created.getString("code");

                isHost = true;

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

    // =========================================================
    // JOIN
    // =========================================================

    private void joinScreen() {

        base();

        content.addView(
                title("🔑 Join Room")
        );

        space();

        EditText name =
                input("اكتب اسمك");

        EditText code =
                input("كود الغرفة - 4 أرقام");

        code.setInputType(2);

        content.addView(name);
        content.addView(code);

        space();

        Button join =
                button("🚪 دخول الغرفة");

        Button back =
                button("← رجوع");

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

                toast("الكود لازم يكون 4 أرقام");
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

    // =========================================================
    // JOIN ROOM
    // =========================================================

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

    // =========================================================
    // PLAYER
    // =========================================================

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

    // =========================================================
    // LOBBY
    // =========================================================

    private void lobby() {

        base();

        content.addView(
                title("🎮 غرفة " + roomCode)
        );

        TextView hint =
                text(
                        "شارك الكود مع أصحابك 👥",
                        17
                );

        hint.setGravity(Gravity.CENTER);

        content.addView(hint);

        space();

        TextView playersView =
                text(
                        "جاري تحميل اللاعبين...",
                        18
                );

        content.addView(playersView);

        space();

        Button ready =
                button("🟢 أنا Ready");

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

    // =========================================================
    // LOAD PLAYERS
    // =========================================================

    private void loadPlayers(TextView view) {

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

                StringBuilder result =
                        new StringBuilder();

                result.append(
                        "👥 Players "
                );

                result.append(
                        players.length()
                );

                result.append("/6\n\n");

                for (
                        int i = 0;
                        i < players.length();
                        i++
                ) {

                    JSONObject p =
                            players.getJSONObject(i);

                    result.append(
                            i + 1
                    );

                    result.append(". ");

                    result.append(
                            p.optString("name")
                    );

                    if (
                            p.optBoolean("is_host")
                    ) {
                        result.append(" 👑");
                    }

                    if (
                            p.optBoolean("ready")
                    ) {
                        result.append(" 🟢");
                    } else {
                        result.append(" ⚪");
                    }

                    result.append("\n");
                }

                runOnUiThread(
                        () -> view.setText(
                                result.toString()
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

    // =========================================================
    // READY
    // =========================================================

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

    // =========================================================
    // START GAME
    // =========================================================

    private void startGame() {

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
                                "&select=name,ready";

                JSONArray players =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null,
                                        false
                                )
                        );

                if (players.length() < 2) {

                    throw new Exception(
                            "تحتاج لاعبين على الأقل"
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
                                    .optBoolean("ready")
                    ) {

                        throw new Exception(
                                "ليس كل اللاعبين Ready"
                        );
                    }
                }

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

                round = 0;
                questionIndex = 0;

                updateGameState(
                        0,
                        "answering"
                );

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

    // =========================================================
    // ANSWER
    // =========================================================

    private void answerScreen() {

        stopSync();
        stopTimer();

        answerSent = false;

        base();

        content.addView(
                title(
                        "❓ الجولة " +
                                (round + 1)
                )
        );

        TextView progress =
                text(
                        "السؤال " +
                                (questionIndex + 1) +
                                " / " +
                                questions.length,
                        16
                );

        progress.setGravity(Gravity.CENTER);

        progress.setTextColor(secondary());

        content.addView(progress);

        TextView timer =
                text(
                        "⏱️ 45",
                        22
                );

        timer.setGravity(Gravity.CENTER);

        timer.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(timer);

        space();

        TextView question =
                text(
                        questions[
                                questionIndex %
                                        questions.length
                                ],
                        24
                );

        question.setGravity(Gravity.CENTER);

        question.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(question);

        space();

        TextView help =
                text(
                        "اكتب إجابتك بصراحة 😄\n" +
                                "بعدها حاول توقع إجابات أصحابك.",
                        17
                );

        help.setGravity(Gravity.CENTER);

        content.addView(help);

        EditText answer =
                input("اكتب إجابتك هنا...");

        answer.setMinLines(3);

        content.addView(answer);

        Button send =
                button("✓ إرسال الإجابة");

        Button chat =
                button("💬 Chat");

        content.addView(send);
        content.addView(chat);

        send.setOnClickListener(v -> {

            if (answerSent) {

                toast("أرسلت إجابتك بالفعل");
                return;
            }

            String value =
                    answer.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {

                toast("اكتب إجابة أولًا");
                return;
            }

            sendAnswer(value);
        });

        chat.setOnClickListener(
                v -> chat()
        );

        startTimer(
                timer,
                send,
                45
        );
    }

    // =========================================================
    // TIMER
    // =========================================================

    private void startTimer(
            TextView timer,
            Button send,
            int seconds
    ) {

        secondsLeft = seconds;

        timerRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (secondsLeft <= 0) {

                            if (!answerSent) {

                                answerSent = true;

                                sendAnswer(
                                        "لا توجد إجابة"
                                );
                            }

                            return;
                        }

                        timer.setText(
                                "⏱️ " +
                                        secondsLeft
                        );

                        secondsLeft--;

                        handler.postDelayed(
                                this,
                                1000
                        );
                    }
                };

        handler.post(timerRunnable);
    }

    private void stopTimer() {

        if (timerRunnable != null) {

            handler.removeCallbacks(
                    timerRunnable
            );

            timerRunnable = null;
        }
    }

    // =========================================================
    // SEND ANSWER
    // =========================================================

    private void sendAnswer(String answer) {

        stopTimer();

        answerSent = true;

        new Thread(() -> {

            try {

                String delete =
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
                        delete,
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

    // =========================================================
    // WAITING
    // =========================================================

    private void waitingForAnswers() {

        base();

        content.addView(
                title("✅ تم إرسال إجابتك")
        );

        TextView wait =
                text(
                        "ننتظر بقية اللاعبين...",
                        20
                );

        wait.setGravity(Gravity.CENTER);

        content.addView(wait);

        TextView count =
                text(
                        "0 / 0",
                        24
                );

        count.setGravity(Gravity.CENTER);

        count.setTypeface(
                null,
                Typeface.BOLD
        );

        content.addView(count);

        space();

        content.addView(
                text(
                        "🎯 بعد الإجابات ستنتقلون إلى مرحلة Predict.",
                        17
                )
        );

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

    // =========================================================
    // CHECK ANSWERS
    // =========================================================

    private void checkAnswers(TextView counter) {

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

    // =========================================================
    // PREDICT
    // =========================================================

    private void predictionScreen() {

        stopSync();
        stopTimer();

        predictionSent = false;

        base();

        content.addView(
                title("🎯 Predict")
        );

        TextView info =
                text(
                        "حاول تعرف إجابة صاحبك!\n\n" +
                                "✅ توقع صحيح = +3\n" +
                                "✨ إذا طابقت الإجابة بالضبط = +2 Bonus\n" +
                                "🏆 المجموع الممكن = +5",
                        18
                );

        info.setGravity(Gravity.CENTER);

        content.addView(info);

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
                input(
                        "شنو تتوقع أنه كتب؟"
                );

        prediction.setMinLines(2);

        content.addView(prediction);

        space();

        Button predict =
                button("🎯 Predict");

        Button skip =
                button("⏭ Skip");

        Button chat =
                button("💬 Chat");

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
                    spinner.getSelectedItem() == null
            ) {

                toast("اختار لاعبًا");
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

                toast("اكتب توقعك");
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

    // =========================================================
    // LOAD TARGETS
    // =========================================================

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

                        final String finalName =
                                name;

                        runOnUiThread(
                                () -> adapter.add(
                                        finalName
                                )
                        );
                    }
                }

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================================================
    // SUBMIT PREDICTION
    // =========================================================

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

                boolean exact =
                        actual.trim()
                                .equalsIgnoreCase(
                                        predicted.trim()
                                );

                boolean correct =
                        exact ||
                                similarAnswer(
                                        actual,
                                        predicted
                                );

                int points = 0;

                if (correct) {
                    points += 3;
                }

                if (exact) {
                    points += 2;
                }

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

                score += points;

                updateScore();

                final int earned =
                        points;

                final boolean right =
                        correct;

                final boolean bonus =
                        exact;

                runOnUiThread(
                        () -> predictionResult(
                                target,
                                predicted,
                                actual,
                                right,
                                bonus,
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

    // =========================================================
    // SIMPLE ANSWER MATCH
    // =========================================================

    private boolean similarAnswer(
            String a,
            String b
    ) {

        String x =
                normalize(a);

        String y =
                normalize(b);

        if (x.isEmpty() || y.isEmpty()) {
            return false;
        }

        return x.equals(y)
                || x.contains(y)
                || y.contains(x);
    }

    private String normalize(String value) {

        return value
                .toLowerCase()
                .replace(" ", "")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .trim();
    }

    // =========================================================
    // RESULT
    // =========================================================

    private void predictionResult(
            String target,
            String predicted,
            String actual,
            boolean correct,
            boolean exact,
            int points
    ) {

        base();

        content.addView(
                title(
                        correct
                                ? "🎉 ممتاز!"
                                : "❌ ليس هذه المرة"
                )
        );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "👤 اللاعب: "
        );

        result.append(target);

        result.append("\n\n🎯 توقعك: ");

        result.append(predicted);

        result.append("\n\n💬 إجابته: ");

        result.append(actual);

        result.append("\n\n");

        if (correct) {

            result.append(
                    "✅ توقعت إجابته!\n"
            );

            result.append(
                    "⭐ +3 نقاط"
            );

            if (exact) {

                result.append(
                        "\n✨ +2 Bonus للمطابقة الدقيقة"
                );
            }
        } else {

            result.append(
                    "😅 حاول مرة أخرى في الجولة القادمة"
            );
        }

        result.append(
                "\n\n🏆 مجموعك: "
        );

        result.append(score);

        TextView resultView =
                text(
                        result.toString(),
                        19
                );

        resultView.setGravity(
                Gravity.CENTER
        );

        content.addView(resultView);

        space();

        Button next =
                button("➡ السؤال التالي");

        Button leaderboard =
                button("🏆 الترتيب");

        Button chat =
                button("💬 Chat");

        content.addView(next);
        content.addView(leaderboard);
        content.addView(chat);

        next.setOnClickListener(
                v -> nextRound()
        );

        leaderboard.setOnClickListener(
                v -> leaderboard()
        );

        chat.setOnClickListener(
                v -> chat()
        );
    }

    // =========================================================
    // SCORE
    // =========================================================

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

    // =========================================================
    // NEXT ROUND
    // =========================================================

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

    // =========================================================
    // LEADERBOARD
    // =========================================================

    private void leaderboard() {

        base();

        content.addView(
                title("🏆 Leaderboard")
        );

        TextView board =
                text(
                        "جاري تحميل الترتيب...",
                        20
                );

        board.setGravity(
                Gravity.CENTER
        );

        content.addView(board);

        loadLeaderboard(board);

        space();

        Button again =
                button("🔄 Play Again");

        Button room =
                button("🏠 Back to Room");

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

    // =========================================================
    // LOAD LEADERBOARD
    // =========================================================

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
                            players.getJSONObject(i);

                    String medal;

                    if (i == 0) {
                        medal = "🥇";
                    } else if (i == 1) {
                        medal = "🥈";
                    } else if (i == 2) {
                        medal = "🥉";
                    } else {
                        medal = "🏅";
                    }

                    result.append(
                            medal
                    );

                    result.append(" ");

                    result.append(
                            p.optString("name")
                    );

                    result.append(
                            " — "
                    );

                    result.append(
                            p.optInt("score")
                    );

                    result.append(
                            " نقطة\n\n"
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

    // =========================================================
    // RESET
    // =========================================================

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

            } catch (Exception ignored) {
            }

        }).start();
    }

    // =========================================================
    // CHAT
    // =========================================================

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
                input("اكتب رسالة...");

        content.addView(input);

        Button send =
                button("📨 إرسال");

        Button back =
                button("← رجوع");

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

    // =========================================================
    // SEND MESSAGE
    // =========================================================

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

    // =========================================================
    // LOAD CHAT
    // =========================================================

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

                    output.append("👤 ");

                    output.append(
                            m.optString(
                                    "player_name"
                            )
                    );

                    output.append("\n");

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

    // =========================================================
    // SETTINGS
    // =========================================================

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

        darkSwitch.setTextSize(18);

        darkSwitch.setChecked(
                dark
        );

        content.addView(
                darkSwitch
        );

        space();

        content.addView(
                text(
                        "🎮 GuessUs v2.0\n\n" +
                                "👥 Multiplayer\n" +
                                "🎯 Prediction\n" +
                                "💬 Chat\n" +
                                "🏆 Leaderboard\n" +
                                "❓ 50 Questions",
                        17
                )
        );

        space();

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

    // =========================================================
    // LEAVE
    // =========================================================

    private void leaveRoom() {

        stopSync();
        stopTimer();

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

                isHost = false;

                home();
            });

        }).start();
    }

    // =========================================================
    // GAME STATE
    // =========================================================

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

    // =========================================================
    // SYNC
    // =========================================================

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

        handler.post(syncRunnable);
    }

    private void stopSync() {

        if (syncRunnable != null) {

            handler.removeCallbacks(
                    syncRunnable
            );

            syncRunnable = null;
        }
    }

    // =========================================================
    // HTTP / SUPABASE
    // =========================================================

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
                (line = reader.readLine())
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
