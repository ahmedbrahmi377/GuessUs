package com.guessus.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
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

    boolean dark = false;

    String roomCode = "";
    String playerName = "";
    int round = 0;
    int score = 0;

    String[] questions = {
            "من أكثر واحد يضحك في وقت غلط؟",
            "لو ربحت مليون، شنو أول حاجة تعملها؟",
            "من أكثر واحد يتأخر على المواعيد؟",
            "لو تسافر غدوة، وين تمشي؟",
            "شنو أسوأ عادة عندك؟",
            "من أكثر واحد يفهمك من نظرة؟",
            "شنو أكثر شيء يخليك تفرح بسرعة؟",
            "من أكثر واحد ممكن ينسى حاجة مهمة؟"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        home();
    }

    TextView text(String s, int size) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setPadding(20, 18, 20, 18);
        return v;
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

        if (dark) {
            content.setBackgroundColor(Color.rgb(25, 25, 30));
        } else {
            content.setBackgroundColor(Color.WHITE);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    void home() {
        base();

        TextView title = text("GuessUs 🎮", 32);
        title.setGravity(Gravity.CENTER);
        content.addView(title);

        TextView sub = text(
                "أسئلة • توقعات • أصدقاء\n\nلعب أونلاين مع أصحابك",
                18
        );
        sub.setGravity(Gravity.CENTER);
        content.addView(sub);

        Button create = button("🏠 Create Room");
        Button join = button("🔑 Join Room");
        Button play = button("▶ Play");
        Button settings = button("⚙ Settings");

        content.addView(create);
        content.addView(join);
        content.addView(play);
        content.addView(settings);

        create.setOnClickListener(v -> createScreen());
        join.setOnClickListener(v -> joinScreen());

        play.setOnClickListener(v -> {
            if (roomCode.isEmpty()) {
                Toast.makeText(
                        this,
                        "أنشئ غرفة أو انضم لغرفة أولاً",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                lobby();
            }
        });

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

        Toast.makeText(
                this,
                "جاري إنشاء الغرفة...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {
            try {

                String check =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/rooms?code=eq." +
                        code +
                        "&select=code";

                String result =
                        request("GET", check, null);

                JSONArray existing =
                        new JSONArray(result);

                if (existing.length() > 0) {
                    createRoom();
                    return;
                }

                JSONObject room = new JSONObject();
                room.put("code", code);

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL +
                                "/rest/v1/rooms",
                        room.toString()
                );

                JSONObject state = new JSONObject();
                state.put("room_code", code);
                state.put("question_index", 0);
                state.put("status", "waiting");

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL +
                                "/rest/v1/game_state",
                        state.toString()
                );

                addPlayer(code, playerName);

                runOnUiThread(() -> {
                    roomCode = code;
                    lobby();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "فشل إنشاء الغرفة:\n" +
                                        e.getMessage(),
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
        content.addView(name);

        EditText code = new EditText(this);
        code.setHint("كود الغرفة");
        code.setInputType(2);
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
                        "أدخل اسمك وكود من 4 أرقام",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            playerName = n;
            roomCode = c;

            checkRoom();
        });

        back.setOnClickListener(v -> home());
    }

    void checkRoom() {

        new Thread(() -> {
            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/rooms?code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=code";

                String result =
                        request("GET", url, null);

                JSONArray rooms =
                        new JSONArray(result);

                if (rooms.length() == 0) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "الغرفة غير موجودة",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                String playersUrl =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players?room_code=eq." +
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
                                        null
                                )
                        );

                if (players.length() >= 6) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "الغرفة ممتلئة",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                    return;
                }

                addPlayer(
                        roomCode,
                        playerName
                );

                runOnUiThread(() -> lobby());

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "تعذر الاتصال بالسيرفر",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    void addPlayer(
            String code,
            String name
    ) throws Exception {

        JSONObject player =
                new JSONObject();

        player.put("room_code", code);
        player.put("name", name);
        player.put("score", 0);
        player.put("ready", false);

        request(
                "POST",
                BuildConfig.SUPABASE_URL +
                        "/rest/v1/players",
                player.toString()
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
                button("✓ Ready");

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
            home();
        });

        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {

                        if (!roomCode.isEmpty()) {
                            loadPlayers(players);

                            handler.postDelayed(
                                    this,
                                    2500
                            );
                        }
                    }
                },
                2500
        );
    }

    void loadPlayers(
            final TextView view
    ) {

        new Thread(() -> {
            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/players?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=name,score,ready";

                String result =
                        request(
                                "GET",
                                url,
                                null
                        );

                JSONArray arr =
                        new JSONArray(result);

                StringBuilder text =
                        new StringBuilder();

                text.append(
                        "👥 Players " +
                        arr.length() +
                        "/6\n\n"
                );

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    JSONObject p =
                            arr.getJSONObject(i);

                    text.append("• ")
                            .append(
                                    p.optString(
                                            "name"
                                    )
                            )
                            .append("  ");

                    if (p.optBoolean("ready")) {
                        text.append("✓ Ready");
                    } else {
                        text.append("○ Waiting");
                    }

                    text.append("\n");
                }

                runOnUiThread(() ->
                        view.setText(text.toString())
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
                                "خطأ",
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
                        "answering"
                );

                data.put(
                        "question_index",
                        0
                );

                data.put(
                        "updated_at",
                        "now()"
                );

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/game_state" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        );

                request(
                        "PATCH",
                        url,
                        data.toString()
                );

                runOnUiThread(() -> {
                    round = 0;
                    answerScreen();
                });

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
                        "❓ السؤال " +
                                (round + 1),
                        27
                )
        );

        TextView q =
                text(
                        questions[
                                round %
                                questions.length
                        ],
                        23
                );

        q.setGravity(Gravity.CENTER);
        content.addView(q);

        RadioGroup group =
                new RadioGroup(this);

        String[] options = {
                "A",
                "B",
                "C",
                "D"
        };

        for (String option : options) {

            RadioButton r =
                    new RadioButton(this);

            r.setText(option);
            r.setTextSize(20);
            group.addView(r);
        }

        content.addView(group);

        Button send =
                button("✓ Submit Answer");

        Button chat =
                button("💬 Chat");

        content.addView(send);
        content.addView(chat);

        send.setOnClickListener(v -> {

            int id =
                    group.getCheckedRadioButtonId();

            if (id == -1) {

                Toast.makeText(
                        this,
                        "اختار إجابة",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            RadioButton selected =
                    findViewById(id);

            String answer =
                    selected.getText()
                            .toString();

            submitAnswer(answer);
        });

        chat.setOnClickListener(v ->
                chat()
        );
    }

    void submitAnswer(
            String answer
    ) {

        final String a = answer;

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
                        a
                );

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL +
                                "/rest/v1/round_answers",
                        data.toString()
                );

                runOnUiThread(() ->
                        waitingForAnswers()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "تعذر إرسال الإجابة",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        }).start();
    }

    void waitingForAnswers() {

        base();

        TextView wait =
                text(
                        "⏳ تم إرسال إجابتك!\n\n" +
                        "نستنى بقية اللاعبين...",
                        23
                );

        wait.setGravity(Gravity.CENTER);

        content.addView(wait);

        Button chat =
                button("💬 Chat");

        content.addView(chat);

        chat.setOnClickListener(v ->
                chat()
        );

        checkAnswers(wait);
    }

    void checkAnswers(
            final TextView wait
    ) {

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (roomCode.isEmpty()) {
                            return;
                        }

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
                                        "&select=player_name";

                                JSONArray arr =
                                        new JSONArray(
                                                request(
                                                        "GET",
                                                        url,
                                                        null
                                                )
                                        );

                                String pUrl =
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
                                                        pUrl,
                                                        null
                                                )
                                        );

                                if (arr.length() >=
                                        players.length()) {

                                    runOnUiThread(() ->
                                            predictionScreen()
                                    );

                                } else {

                                    runOnUiThread(() ->
                                            wait.setText(
                                                    "⏳ تم إرسال إجابتك!\n\n" +
                                                    arr.length() +
                                                    "/" +
                                                    players.length() +
                                                    " أجابوا..."
                                            )
                                    );

                                    handler.postDelayed(
                                            this,
                                            2000
                                    );
                                }

                            } catch (Exception e) {

                                handler.postDelayed(
                                        this,
                                        2500
                                );
                            }
                        }).start();
                    }
                },
                2000
        );
    }

    void predictionScreen() {

        base();

        content.addView(
                text(
                        "🎯 توقع إجابة لاعب",
                        27
                )
        );

        content.addView(
                text(
                        "اختر اللاعب ثم اختر الإجابة التي تتوقع أنه اختارها.",
                        18
                )
        );

        final Spinner targetSpinner =
                new Spinner(this);

        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item
                );

        content.addView(targetSpinner);

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
                                request(
                                        "GET",
                                        url,
                                        null
                                )
                        );

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    String name =
                            arr.getJSONObject(i)
                                    .optString(
                                            "name"
                                    );

                    if (!name.equals(playerName)) {
                        adapter.add(name);
                    }
                }

                runOnUiThread(() ->
                        targetSpinner.setAdapter(
                                adapter
                        )
                );

            } catch (Exception ignored) {
            }
        }).start();

        RadioGroup group =
                new RadioGroup(this);

        for (String option :
                new String[]{"A", "B", "C", "D"}) {

            RadioButton r =
                    new RadioButton(this);

            r.setText(option);
            r.setTextSize(20);

            group.addView(r);
        }

        content.addView(group);

        Button predict =
                button("🎯 Predict");

        Button skip =
                button("⏭ Skip");

        content.addView(predict);
        content.addView(skip);

        predict.setOnClickListener(v -> {

            if (targetSpinner.getSelectedItem() == null) {
                return;
            }

            if (group.getCheckedRadioButtonId()
                    == -1) {

                Toast.makeText(
                        this,
                        "اختار الإجابة المتوقعة",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String target =
                    targetSpinner
                            .getSelectedItem()
                            .toString();

            RadioButton selected =
                    findViewById(
                            group.getCheckedRadioButtonId()
                    );

            String predicted =
                    selected.getText()
                            .toString();

            submitPrediction(
                    target,
                    predicted
            );
        });

        skip.setOnClickListener(v ->
                nextRound()
        );
    }

    void submitPrediction(
            String target,
            String predicted
    ) {

        new Thread(() -> {
            try {

                String answerUrl =
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
                                        answerUrl,
                                        null
                                )
                        );

                boolean correct = false;

                if (answers.length() > 0) {

                    String actual =
                            answers
                                    .getJSONObject(0)
                                    .optString(
                                            "answer"
                                    );

                    correct =
                            actual.equals(
                                    predicted
                            );
                }

                int points =
                        correct ? 5 : 0;

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
                        prediction.toString()
                );

                if (correct) {
                    score += 5;
                    updateScore();
                }

                final boolean finalCorrect =
                        correct;

                runOnUiThread(() -> {

                    String msg =
                            finalCorrect
                                    ? "🎉 صحيح! +5 نقاط"
                                    : "❌ موش صحيح";

                    Toast.makeText(
                            this,
                            msg,
                            Toast.LENGTH_SHORT
                    ).show();

                    nextRound();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "تعذر تسجيل التوقع",
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

                data.put("score", score);

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
                        "🏆 Leaderboard",
                        30
                )
        );

        final TextView board =
                text(
                        "جاري تحميل الترتيب...",
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
                                request(
                                        "GET",
                                        url,
                                        null
                                )
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
                                    p.optString(
                                            "name"
                                    )
                            )
                            .append(" — ")
                            .append(
                                    p.optInt(
                                            "score"
                                    )
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
                                "تعذر تحميل الترتيب"
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
                text(
                        "💬 Chat",
                        28
                )
        );

        final TextView messages =
                text(
                        "جاري تحميل الرسائل...",
                        18
                );

        content.addView(messages);

        final EditText input =
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

        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {

                        if (!roomCode.isEmpty()) {
                            loadMessages(messages);

                            handler.postDelayed(
                                    this,
                                    2500
                            );
                        }
                    }
                },
                2500
        );
    }

    void sendMessage(
            String message
    ) {

        new Thread(() -> {
            try {

                JSONObject data =
                        new JSONObject();

                data.put(
                        "room_code",
                        roomCode
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

    void loadMessages(
            final TextView view
    ) {

        new Thread(() -> {
            try {

                String url =
                        BuildConfig.SUPABASE_URL +
                        "/rest/v1/messages" +
                        "?room_code=eq." +
                        URLEncoder.encode(
                                roomCode,
                                "UTF-8"
                        ) +
                        "&select=player_name,message,created_at" +
                        "&order=created_at.asc";

                JSONArray arr =
                        new JSONArray(
                                request(
                                        "GET",
                                        url,
                                        null
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                for (int i = 0;
                     i < arr.length();
                     i++) {

                    JSONObject m =
                            arr.getJSONObject(i);

                    result.append("• ")
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

                if (result.length() == 0) {
                    result.append(
                            "لا توجد رسائل بعد."
                    );
                }

                runOnUiThread(() ->
                        view.setText(
                                result.toString()
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
                text(
                        "⚙ Settings",
                        28
                )
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

        content.addView(
                text(
                        "العربية / Français / English",
                        18
                )
        );

        Button back =
                button("← Back");

        content.addView(back);

        darkSwitch.setOnCheckedChangeListener(
                (button, checked) -> {
                    dark = checked;
                    settings();
                }
        );

        back.setOnClickListener(
                v -> home()
        );
    }

    String request(
            String method,
            String urlString,
            String body
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

        conn.setRequestProperty(
                "Prefer",
                "return=minimal"
        );

        conn.setConnectTimeout(
                10000
        );

        conn.setReadTimeout(
                10000
        );

        if (body != null) {

            conn.setDoOutput(true);

            OutputStream output =
                    conn.getOutputStream();

            output.write(
                    body.getBytes("UTF-8")
            );

            output.flush();
            output.close();
        }

        int code =
                conn.getResponseCode();

        InputStream stream;

        if (code >= 200 &&
                code < 400) {

            stream =
                    conn.getInputStream();

        } else {

            stream =
                    conn.getErrorStream();
        }

        if (stream == null) {
            return "";
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
        conn.disconnect();

        if (code < 200 ||
                code >= 300) {

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
