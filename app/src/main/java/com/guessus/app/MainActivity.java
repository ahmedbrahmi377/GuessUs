package com.guessus.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

public class MainActivity extends Activity {

    LinearLayout content;
    Handler handler = new Handler();

    String roomCode = "";
    String roomId = "";
    String playerName = "";
    int score = 0;
    int round = 0;

    boolean dark = false;

    final String[] questions = {
        "من أكثر واحد يضحك في وقت غلط؟",
        "لو ربحت مليون، شنو أول حاجة تعملها؟",
        "من أكثر واحد يتأخر على المواعيد؟",
        "لو تسافر غدوة، وين تمشي؟",
        "شنو أكثر عادة تحب تغيرها؟",
        "من أكثر واحد يفهمك من نظرة؟",
        "شنو أكثر شيء يفرحك بسرعة؟",
        "من أكثر واحد ممكن ينسى حاجة مهمة؟",
        "من أكثر واحد يحب المغامرات؟",
        "من أكثر واحد عنده أسرار؟"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        home();
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setPadding(20, 18, 20, 18);
        return t;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    void base() {
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(18, 20, 18, 20);

        content.setBackgroundColor(
            dark ? Color.rgb(25,25,30) : Color.WHITE
        );

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    void home() {
        base();

        TextView title = text("GuessUs 🎮", 32);
        title.setGravity(Gravity.CENTER);
        content.addView(title);

        TextView subtitle =
            text("أسئلة • توقعات • أصدقاء\nلعب أونلاين", 18);
        subtitle.setGravity(Gravity.CENTER);
        content.addView(subtitle);

        Button create = button("🏠 Create Room");
        Button join = button("🔑 Join Room");
        Button settings = button("⚙ Settings");

        content.addView(create);
        content.addView(join);
        content.addView(settings);

        create.setOnClickListener(v -> createScreen());
        join.setOnClickListener(v -> joinScreen());
        settings.setOnClickListener(v -> settings());
    }

    void createScreen() {
        base();

        content.addView(text("🏠 Create Room", 28));

        EditText name = new EditText(this);
        name.setHint("اسمك");
        content.addView(name);

        Button create = button("Create Room");
        Button back = button("← Back");

        content.addView(create);
        content.addView(back);

        create.setOnClickListener(v -> {
            String n = name.getText().toString().trim();

            if (n.isEmpty()) {
                Toast.makeText(
                    this,
                    "اكتب اسمك",
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }

            playerName = n;
            createRoom();
        });

        back.setOnClickListener(v -> home());
    }

    void createRoom() {

        final String code =
            String.valueOf(
                1000 + new Random().nextInt(9000)
            );

        new Thread(() -> {
            try {

                String check =
                    BuildConfig.SUPABASE_URL +
                    "/rest/v1/rooms?code=eq." +
                    code +
                    "&select=id,code";

                JSONArray found =
                    new JSONArray(
                        request("GET", check, null)
                    );

                if (found.length() > 0) {
                    createRoom();
                    return;
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

                JSONArray created =
                    new JSONArray(response);

                if (created.length() == 0) {
                    throw new Exception(
                        "لم يتم إنشاء الغرفة"
                    );
                }

                JSONObject r =
                    created.getJSONObject(0);

                roomId = r.getString("id");
                roomCode = code;

                JSONObject state =
                    new JSONObject();

                state.put("room_code", code);
                state.put("question_index", 0);
                state.put("status", "waiting");

                request(
                    "POST",
                    BuildConfig.SUPABASE_URL +
                    "/rest/v1/game_state",
                    state.toString()
                );

                addPlayer(
                    roomId,
                    roomCode,
                    playerName,
                    true
                );

                runOnUiThread(() -> lobby());

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        "HTTP Error:\n" + e.getMessage(),
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        }).start();
    }

    void joinScreen() {
        base();

        content.addView(text("🔑 Join Room", 28));

        EditText name = new EditText(this);
        name.setHint("اسمك");

        EditText code = new EditText(this);
        code.setHint("كود الغرفة");
        code.setInputType(2);

        content.addView(name);
        content.addView(code);

        Button join = button("Join Room");
        Button back = button("← Back");

        content.addView(join);
        content.addView(back);

        join.setOnClickListener(v -> {

            String n =
                name.getText().toString().trim();

            String c =
                code.getText().toString().trim();

            if (n.isEmpty() || c.length() != 4) {
                Toast.makeText(
                    this,
                    "أدخل الاسم وكود من 4 أرقام",
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }

            playerName = n;
            roomCode = c;

            findRoom();
        });

        back.setOnClickListener(v -> home());
    }

    void findRoom() {

        new Thread(() -> {
            try {

                String url =
                    BuildConfig.SUPABASE_URL +
                    "/rest/v1/rooms?code=eq." +
                    URLEncoder.encode(
                        roomCode,
                        "UTF-8"
                    ) +
                    "&select=id,code,status";

                JSONArray arr =
                    new JSONArray(
                        request("GET", url, null)
                    );

                if (arr.length() == 0) {
                    throw new Exception(
                        "الغرفة غير موجودة"
                    );
                }

                JSONObject room =
                    arr.getJSONObject(0);

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

                addPlayer(
                    roomId,
                    roomCode,
                    playerName,
                    false
                );

                runOnUiThread(() -> lobby());

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        e.getMessage(),
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        }).start();
    }

    void addPlayer(
        String id,
        String code,
        String name,
        boolean host
    ) throws Exception {

        JSONObject p =
            new JSONObject();

        p.put("room_id", id);
        p.put("name", name);
        p.put("is_host", host);
        p.put("player_id", name + "_" + System.currentTimeMillis());
        p.put("room_code", code);
        p.put("score", 0);
        p.put("ready", false);

        request(
            "POST",
            BuildConfig.SUPABASE_URL +
            "/rest/v1/players",
            p.toString()
        );
    }

    void lobby() {

        base();

        TextView title =
            text(
                "🎮 Room: " + roomCode,
                28
            );

        title.setGravity(Gravity.CENTER);
        content.addView(title);

        TextView players =
            text(
                "جاري تحميل اللاعبين...",
                19
            );

        content.addView(players);

        Button ready =
            button("🟢 Ready");

        Button start =
            button("🚀 Start Game");

        Button chat =
            button("💬 Chat");

        Button leave =
            button("← Leave");

        content.addView(ready);
        content.addView(start);
        content.addView(chat);
        content.addView(leave);

        loadPlayers(players);

        ready.setOnClickListener(v ->
            setReady()
        );

        start.setOnClickListener(v ->
            startGame()
        );

        chat.setOnClickListener(v ->
            chat()
        );

        leave.setOnClickListener(v -> {
            roomCode = "";
            roomId = "";
            home();
        });
    }

    void loadPlayers(TextView view) {

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
                    "&select=name,score,ready,is_host";

                JSONArray arr =
                    new JSONArray(
                        request("GET", url, null)
                    );

                StringBuilder out =
                    new StringBuilder();

                out.append(
                    "👥 Players "
                ).append(
                    arr.length()
                ).append(
                    "/6\n\n"
                );

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    JSONObject p =
                        arr.getJSONObject(i);

                    out.append("• ")
                        .append(
                            p.optString("name")
                        );

                    if (p.optBoolean("is_host")) {
                        out.append(" 👑");
                    }

                    if (p.optBoolean("ready")) {
                        out.append(" ✓");
                    }

                    out.append("\n");
                }

                runOnUiThread(() ->
                    view.setText(out.toString())
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                    view.setText(
                        "تعذر تحميل اللاعبين"
                    )
                );
            }
        }).start();
    }

    void setReady() {

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

                data.put("ready", true);

                request(
                    "PATCH",
                    url,
                    data.toString()
                );

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        "Ready ✓",
                        Toast.LENGTH_SHORT
                    ).show()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        "فشل Ready",
                        Toast.LENGTH_SHORT
                    ).show()
                );
            }
        }).start();
    }

    void startGame() {

        new Thread(() -> {
            try {

                JSONObject data =
                    new JSONObject();

                data.put(
                    "status",
                    "playing"
                );

                String url =
                    BuildConfig.SUPABASE_URL +
                    "/rest/v1/rooms" +
                    "?code=eq." +
                    URLEncoder.encode(
                        roomCode,
                        "UTF-8"
                    );

                request(
                    "PATCH",
                    url,
                    data.toString()
                );

                round = 0;
                score = 0;

                runOnUiThread(() ->
                    answerScreen()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        "تعذر بدء اللعبة",
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        }).start();
    }

    void answerScreen() {

        base();

        content.addView(
            text(
                "❓ الجولة " + (round + 1),
                27
            )
        );

        TextView q =
            text(
                questions[
                    round % questions.length
                ],
                23
            );

        q.setGravity(Gravity.CENTER);
        content.addView(q);

        content.addView(
            text(
                "اكتب إجابتك بكلماتك:",
                18
            )
        );

        EditText answer =
            new EditText(this);

        answer.setHint(
            "إجابتك..."
        );

        content.addView(answer);

        Button submit =
            button("✓ إرسال الإجابة");

        Button chat =
            button("💬 Chat");

        content.addView(submit);
        content.addView(chat);

        submit.setOnClickListener(v -> {

            String a =
                answer.getText()
                    .toString()
                    .trim();

            if (a.isEmpty()) {
                Toast.makeText(
                    this,
                    "اكتب إجابتك",
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }

            submitAnswer(a);
        });

        chat.setOnClickListener(v ->
            chat()
        );
    }

    void submitAnswer(String answer) {

        new Thread(() -> {
            try {

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
                    data.toString()
                );

                runOnUiThread(() ->
                    predictionScreen()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        "تعذر إرسال الإجابة:\n" +
                        e.getMessage(),
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        }).start();
    }

    void predictionScreen() {

        base();

        content.addView(
            text(
                "🎯 توقّع إجابة صاحبك",
                27
            )
        );

        content.addView(
            text(
                "اختار لاعبًا واكتب الإجابة التي تتوقع أنه كتبها.",
                18
            )
        );

        Spinner players =
            new Spinner(this);

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item
            );

        content.addView(players);

        loadPredictionPlayers(
            players,
            adapter
        );

        EditText prediction =
            new EditText(this);

        prediction.setHint(
            "شنو تتوقع إجابته؟"
        );

        content.addView(prediction);

        Button predict =
            button("🎯 Predict");

        Button skip =
            button("⏭ Skip");

        content.addView(predict);
        content.addView(skip);

        predict.setOnClickListener(v -> {

            if (players.getSelectedItem() == null) {
                return;
            }

            String target =
                players.getSelectedItem()
                    .toString();

            String answer =
                prediction.getText()
                    .toString()
                    .trim();

            if (answer.isEmpty()) {
                Toast.makeText(
                    this,
                    "اكتب توقعك",
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }

            submitPrediction(
                target,
                answer
            );
        });

        skip.setOnClickListener(v ->
            nextRound()
        );
    }

    void loadPredictionPlayers(
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

                JSONArray arr =
                    new JSONArray(
                        request("GET", url, null)
                    );

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    String name =
                        arr.getJSONObject(i)
                            .optString("name");

                    if (!name.equals(playerName)) {
                        adapter.add(name);
                    }
                }

                runOnUiThread(() ->
                    spinner.setAdapter(adapter)
                );

            } catch (Exception ignored) {
            }
        }).start();
    }

    void submitPrediction(
        String target,
        String predicted
    ) {

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

                JSONArray arr =
                    new JSONArray(
                        request("GET", url, null)
                    );

                if (arr.length() == 0) {
                    throw new Exception(
                        "اللاعب لم يرسل إجابته بعد"
                    );
                }

                String actual =
                    arr.getJSONObject(0)
                        .optString("answer");

                boolean correct =
                    actual.equalsIgnoreCase(
                        predicted.trim()
                    );

                int points =
                    correct ? 5 : 0;

                JSONObject p =
                    new JSONObject();

                p.put(
                    "room_code",
                    roomCode
                );

                p.put(
                    "round",
                    round
                );

                p.put(
                    "predictor",
                    playerName
                );

                p.put(
                    "target",
                    target
                );

                p.put(
                    "predicted_answer",
                    predicted
                );

                p.put(
                    "correct",
                    correct
                );

                p.put(
                    "points",
                    points
                );

                request(
                    "POST",
                    BuildConfig.SUPABASE_URL +
                    "/rest/v1/predictions",
                    p.toString()
                );

                if (correct) {
                    score += 5;
                    updateScore();
                }

                runOnUiThread(() -> {

                    Toast.makeText(
                        this,
                        correct
                            ? "🎉 صحيح! +5"
                            : "❌ غلط",
                        Toast.LENGTH_SHORT
                    ).show();

                    nextRound();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                    Toast.makeText(
                        this,
                        e.getMessage(),
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        }).start();
    }

    void updateScore() {

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
                    data.toString()
                );

            } catch (Exception ignored) {
            }
        }).start();
    }

    void nextRound() {

        round++;

        if (round >= questions.length) {
            leaderboard();
        } else {
            answerScreen();
        }
    }

    void leaderboard() {

        base();

        content.addView(
            text(
                "🏆 النتائج النهائية",
                30
            )
        );

        TextView board =
            text(
                "جاري تحميل النتائج...",
                21
            );

        content.addView(board);

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

                JSONArray arr =
                    new JSONArray(
                        request("GET", url, null)
                    );

                StringBuilder result =
                    new StringBuilder();

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    JSONObject p =
                        arr.getJSONObject(i);

                    result.append(
                        i + 1
                    )
                    .append(". ")
                    .append(
                        p.optString("name")
                    )
                    .append(" — ")
                    .append(
                        p.optInt("score")
                    )
                    .append(" نقطة\n");
                }

                runOnUiThread(() ->
                    board.setText(
                        result.toString()
                    )
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                    board.setText(
                        "تعذر تحميل النتائج"
                    )
                );
            }
        }).start();

        Button again =
            button("🔄 Play Again");

        Button room =
            button("🏠 Room");

        content.addView(again);
        content.addView(room);

        again.setOnClickListener(v -> {
            round = 0;
            score = 0;
            startGame();
        });

        room.setOnClickListener(v ->
            lobby()
        );
    }

    void chat() {

        base();

        content.addView(
            text("💬 Chat", 28)
        );

        TextView messages =
            text(
                "جاري تحميل الرسائل...",
                18
            );

        content.addView(messages);

        EditText input =
            new EditText(this);

        input.setHint(
            "اكتب رسالة..."
        );

        content.addView(input);

        Button send =
            button("Send");

        Button back =
            button("← Back");

        content.addView(send);
        content.addView(back);

        loadMessages(messages);

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

            handler.postDelayed(
                () -> loadMessages(messages),
                500
            );
        });

        back.setOnClickListener(v ->
            lobby()
        );
    }

    void sendMessage(String message) {

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
                    data.toString()
                );

            } catch (Exception ignored) {
            }
        }).start();
    }

    void loadMessages(TextView view) {

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

                JSONArray arr =
                    new JSONArray(
                        request("GET", url, null)
                    );

                StringBuilder out =
                    new StringBuilder();

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    JSONObject m =
                        arr.getJSONObject(i);

                    out.append("• ")
                        .append(
                            m.optString(
                                "player_name"
                            )
                        )
                        .append(": ")
                        .append(
                            m.optString(
                                "message"
                            )
                        )
                        .append("\n\n");
                }

                if (out.length() == 0) {
                    out.append(
                        "لا توجد رسائل بعد."
                    );
                }

                runOnUiThread(() ->
                    view.setText(
                        out.toString()
                    )
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                    view.setText(
                        "تعذر تحميل الرسائل"
                    )
                );
            }
        }).start();
    }

    void settings() {

        base();

        content.addView(
            text("⚙ Settings", 28)
        );

        Switch darkSwitch =
            new Switch(this);

        darkSwitch.setText(
            "Dark Mode"
        );

        darkSwitch.setChecked(
            dark
        );

        content.addView(
            darkSwitch
        );

        Button back =
            button("← Back");

        content.addView(back);

        darkSwitch.setOnCheckedChangeListener(
            (b, checked) -> {
                dark = checked;
                settings();
            }
        );

        back.setOnClickListener(v ->
            home()
        );
    }

    String request(
        String method,
        String urlString,
        String body
    ) throws Exception {

        return request(
            method,
            urlString,
            body,
            false
        );
    }

    String request(
        String method,
        String urlString,
        String body,
        boolean returnBody
    ) throws Exception {

        URL url =
            new URL(urlString);

        HttpURLConnection conn =
            (HttpURLConnection)
            url.openConnection();

        conn.setRequestMethod(method);

        conn.setRequestProperty(
            "apikey",
            BuildConfig.SUPABASE_KEY
        );

        conn.setRequestProperty(
            "Authorization",
            "Bearer " +
            BuildConfig.SUPABASE_KEY
        );

        conn.setRequestProperty(
            "Content-Type",
            "application/json"
        );

        conn.setRequestProperty(
            "Accept",
            "application/json"
        );

        if (returnBody) {
            conn.setRequestProperty(
                "Prefer",
                "return=representation"
            );
        } else {
            conn.setRequestProperty(
                "Prefer",
                "return=minimal"
            );
        }

        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (body != null) {

            conn.setDoOutput(true);

            OutputStream out =
                conn.getOutputStream();

            out.write(
                body.getBytes("UTF-8")
            );

            out.flush();
            out.close();
        }

        int response =
            conn.getResponseCode();

        InputStream stream =
            response >= 200 &&
            response < 400
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            throw new IOException(
                "HTTP " + response
            );
        }

        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(stream)
            );

        StringBuilder result =
            new StringBuilder();

        String line;

        while (
            (line = reader.readLine()) != null
        ) {
            result.append(line);
        }

        reader.close();
        conn.disconnect();

        if (response < 200 ||
            response >= 300) {

            throw new IOException(
                "HTTP " +
                response +
                ": " +
                result
            );
        }

        return result.toString();
    }
                  }
