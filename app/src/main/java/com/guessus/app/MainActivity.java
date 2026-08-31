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
    boolean dark = false;
    int q = 0;
    int score = 0;

    String roomCode = "";
    String playerName = "Player";
    Handler handler = new Handler();

    String[] qs = {
            "من أكثر واحد يضحك في وقت غلط؟",
            "لو ربحت مليون، شنو أول حاجة تعملها؟",
            "من أكثر واحد يتأخر على المواعيد؟",
            "لو تسافر غدوة، وين تمشي؟",
            "شنو أسوأ عادة عندك؟",
            "من أكثر واحد يفهمك من نظرة؟"
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        home();
    }

    TextView t(String s, int z) {
        TextView x = new TextView(this);
        x.setText(s);
        x.setTextSize(z);
        x.setPadding(24, 18, 24, 18);
        return x;
    }

    Button b(String s) {
        Button x = new Button(this);
        x.setText(s);
        x.setAllCaps(false);
        return x;
    }

    void base() {
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 25, 20, 20);
        content.setBackgroundColor(
                dark ? Color.rgb(25, 25, 30) : Color.WHITE
        );

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    void home() {
        base();

        TextView h = t("GuessUs 🎮", 32);
        h.setGravity(Gravity.CENTER);
        content.addView(h);

        content.addView(t("أسئلة • توقعات • أصدقاء", 18));

        Button play = b("▶ Play");
        Button create = b("🏠 Create Room");
        Button join = b("🔑 Join Room");
        Button settings = b("⚙ Settings");

        content.addView(play);
        content.addView(create);
        content.addView(join);
        content.addView(settings);

        play.setOnClickListener(v -> {
            if (roomCode.length() == 0) {
                Toast.makeText(this,
                        "أنشئ غرفة أو انضم لغرفة أولاً",
                        Toast.LENGTH_SHORT).show();
            } else {
                game();
            }
        });

        create.setOnClickListener(v -> createRoom());
        join.setOnClickListener(v -> joinRoomScreen());
        settings.setOnClickListener(v -> settings());
    }

    void createRoom() {
        base();

        content.addView(t("🏠 Create Room", 28));

        final EditText name = new EditText(this);
        name.setHint("اسمك");
        content.addView(name);

        Button create = b("Create");
        Button back = b("← Back");

        content.addView(create);
        content.addView(back);

        create.setOnClickListener(v -> {
            playerName = name.getText().toString().trim();

            if (playerName.length() == 0) {
                playerName = "Player";
            }

            String code =
                    String.valueOf(1000 + new Random().nextInt(9000));

            Toast.makeText(this,
                    "جاري إنشاء الغرفة...",
                    Toast.LENGTH_SHORT).show();

            apiPostRoom(code);
        });

        back.setOnClickListener(v -> home());
    }

    void apiPostRoom(final String code) {
        new Thread(() -> {
            try {
                JSONObject room = new JSONObject();
                room.put("code", code);

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL + "/rest/v1/rooms",
                        room.toString()
                );

                addPlayer(code, playerName, () -> {
                    roomCode = code;
                    roomScreen();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "خطأ في إنشاء الغرفة",
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    void joinRoomScreen() {
        base();

        content.addView(t("🔑 Join Room", 28));

        final EditText name = new EditText(this);
        name.setHint("اسمك");
        content.addView(name);

        final EditText code = new EditText(this);
        code.setHint("كود الغرفة");
        code.setInputType(2);
        content.addView(code);

        Button join = b("Join");
        Button back = b("← Back");

        content.addView(join);
        content.addView(back);

        join.setOnClickListener(v -> {
            playerName = name.getText().toString().trim();
            roomCode = code.getText().toString().trim();

            if (playerName.length() == 0) {
                playerName = "Player";
            }

            if (roomCode.length() != 4) {
                Toast.makeText(this,
                        "أدخل كود من 4 أرقام",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            checkRoom();
        });

        back.setOnClickListener(v -> home());
    }

    void checkRoom() {
        final String code = roomCode;

        new Thread(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL +
                        "/rest/v1/rooms?code=eq." +
                        URLEncoder.encode(code, "UTF-8") +
                        "&select=*";

                String result = request("GET", url, null);

                JSONArray arr = new JSONArray(result);

                if (arr.length() == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "الغرفة غير موجودة",
                                    Toast.LENGTH_SHORT).show()
                    );
                } else {
                    addPlayer(code, playerName, () -> roomScreen());
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "تعذر الاتصال بالسيرفر",
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    void addPlayer(final String code,
                   final String name,
                   final Runnable after) {

        new Thread(() -> {
            try {
                JSONObject player = new JSONObject();
                player.put("room_code", code);
                player.put("name", name);
                player.put("score", 0);
                player.put("ready", false);

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL + "/rest/v1/players",
                        player.toString()
                );

                runOnUiThread(after);

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "تعذر إضافة اللاعب",
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    void roomScreen() {
        base();

        content.addView(t("🎮 Room: " + roomCode, 28));

        final TextView players =
                t("جاري تحميل اللاعبين...", 20);

        content.addView(players);

        Button ready = b("✓ Ready");
        Button start = b("🚀 Start Game");
        Button chat = b("💬 Chat");
        Button back = b("← Back");

        content.addView(ready);
        content.addView(start);
        content.addView(chat);
        content.addView(back);

        loadPlayers(players);

        ready.setOnClickListener(v -> setReady());
        start.setOnClickListener(v -> game());
        chat.setOnClickListener(v -> chat());
        back.setOnClickListener(v -> home());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (roomCode.length() > 0) {
                    loadPlayers(players);
                    handler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }

    void loadPlayers(final TextView players) {
        new Thread(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL +
                        "/rest/v1/players?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&select=name,score,ready";

                final String result =
                        request("GET", url, null);

                runOnUiThread(() -> {
                    try {
                        JSONArray arr = new JSONArray(result);

                        String text =
                                "👥 Players • " +
                                arr.length() + "/6\n\n";

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject p =
                                    arr.getJSONObject(i);

                            text += "• " +
                                    p.optString("name") +
                                    "  " +
                                    (p.optBoolean("ready")
                                            ? "✓"
                                            : "○") +
                                    "\n";
                        }

                        players.setText(text);

                    } catch (Exception e) {
                        players.setText(
                                "تعذر تحميل اللاعبين"
                        );
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        players.setText("تعذر الاتصال")
                );
            }
        }).start();
    }

    void setReady() {
        new Thread(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL +
                        "/rest/v1/players?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&name=eq." +
                        URLEncoder.encode(playerName, "UTF-8");

                JSONObject obj = new JSONObject();
                obj.put("ready", true);

                request("PATCH", url, obj.toString());

                runOnUiThread(() ->
                        Toast.makeText(this,
                                "أنت Ready ✓",
                                Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "خطأ",
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    void game() {
        base();

        TextView tm = t("⏱ 20", 20);
        tm.setGravity(Gravity.CENTER);
        content.addView(tm);

        TextView qu =
                t(qs[q % qs.length], 25);

        qu.setGravity(Gravity.CENTER);
        content.addView(qu);

        RadioGroup rg = new RadioGroup(this);

        for (String o : new String[]{"A", "B", "C", "D"}) {
            RadioButton r = new RadioButton(this);
            r.setText(o);
            r.setTextSize(18);
            rg.addView(r);
        }

        content.addView(rg);

        Button predict = b("🎯 Predict");
        Button skip = b("⏭ Skip");
        Button chat = b("💬 Chat");

        content.addView(predict);
        content.addView(skip);
        content.addView(chat);

        final int[] left = {20};

        final Runnable timer = new Runnable() {
            @Override
            public void run() {
                tm.setText("⏱ " + left[0]);

                if (left[0] > 0) {
                    left[0]--;
                    handler.postDelayed(this, 1000);
                } else {
                    results();
                }
            }
        };

        handler.post(timer);

        predict.setOnClickListener(v -> {
            if (rg.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this,
                        "اختار إجابة",
                        Toast.LENGTH_SHORT).show();
            } else {
                score += 3;
                results();
            }
        });

        skip.setOnClickListener(v -> {
            q++;
            game();
        });

        chat.setOnClickListener(v -> chat());
    }

    void results() {
        base();

        content.addView(t("✨ Results", 30));

        content.addView(
                t("الإجابة: A\n\n+3 نقاط 🎉", 21)
        );

        content.addView(
                t("🏆 نقاطك: " + score, 22)
        );

        Button next = b("➡ Next Question");
        Button rematch = b("🔄 Rematch");

        content.addView(next);
        content.addView(rematch);

        next.setOnClickListener(v -> {
            q++;
            game();
        });

        rematch.setOnClickListener(v -> {
            q = 0;
            score = 0;
            game();
        });
    }

    void chat() {
        base();

        content.addView(t("💬 Chat", 28));

        final TextView messages =
                t("جاري تحميل الرسائل...", 18);

        content.addView(messages);

        final EditText input =
                new EditText(this);

        input.setHint("اكتب رسالة...");
        content.addView(input);

        Button send = b("Send");
        Button back = b("← Back");

        content.addView(send);
        content.addView(back);

        loadMessages(messages);

        send.setOnClickListener(v -> {
            String msg =
                    input.getText().toString().trim();

            if (msg.length() == 0) return;

            sendMessage(msg);
            input.setText("");
        });

        back.setOnClickListener(v -> roomScreen());
    }

    void sendMessage(final String msg) {
        new Thread(() -> {
            try {
                JSONObject obj = new JSONObject();
                obj.put("room_code", roomCode);
                obj.put("player_name", playerName);
                obj.put("message", msg);

                request(
                        "POST",
                        BuildConfig.SUPABASE_URL +
                                "/rest/v1/messages",
                        obj.toString()
                );

            } catch (Exception ignored) {
            }
        }).start();
    }

    void loadMessages(final TextView messages) {
        new Thread(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL +
                        "/rest/v1/messages?room_code=eq." +
                        URLEncoder.encode(roomCode, "UTF-8") +
                        "&select=player_name,message,created_at" +
                        "&order=created_at.asc";

                final String result =
                        request("GET", url, null);

                runOnUiThread(() -> {
                    try {
                        JSONArray arr =
                                new JSONArray(result);

                        String text = "";

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject m =
                                    arr.getJSONObject(i);

                            text += m.optString("player_name") +
                                    ": " +
                                    m.optString("message") +
                                    "\n\n";
                        }

                        if (text.length() == 0) {
                            text = "لا توجد رسائل بعد.";
                        }

                        messages.setText(text);

                    } catch (Exception e) {
                        messages.setText(
                                "تعذر تحميل الرسائل"
                        );
                    }
                });

            } catch (Exception ignored) {
            }
        }).start();
    }

    void settings() {
        base();

        content.addView(t("⚙ Settings", 28));

        Switch darkSwitch = new Switch(this);
        darkSwitch.setText("Dark Mode");
        darkSwitch.setChecked(dark);

        content.addView(darkSwitch);

        content.addView(
                t("Language: العربية / Français / English", 18)
        );

        Button back = b("← Back");
        content.addView(back);

        darkSwitch.setOnCheckedChangeListener(
                (button, checked) -> {
                    dark = checked;
                    settings();
                }
        );

        back.setOnClickListener(v -> home());
    }

    String request(String method,
                   String urlString,
                   String body) throws Exception {

        URL url = new URL(urlString);

        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

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
                "Prefer",
                "return=minimal"
        );

        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (body != null) {
            conn.setDoOutput(true);

            OutputStream os =
                    conn.getOutputStream();

            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();
        }

        int code = conn.getResponseCode();

        InputStream stream;

        if (code >= 200 && code < 400) {
            stream = conn.getInputStream();
        } else {
            stream = conn.getErrorStream();
        }

        if (stream == null) {
            return "";
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(stream)
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new IOException(
                    "HTTP " + code + ": " + response
            );
        }

        return response.toString();
    }
  }
