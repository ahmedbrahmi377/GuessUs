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

        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (roomCode.length() == 0) {
                    Toast.makeText(MainActivity.this,
                            "أنشئ غرفة أو انضم لغرفة أولاً",
                            Toast.LENGTH_SHORT).show();
                } else {
                    game();
                }
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createRoom();
            }
        });

        join.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                joinRoomScreen();
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                settings();
            }
        });
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

        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                playerName = name.getText().toString().trim();

                if (playerName.length() == 0) {
                    playerName = "Player";
                }

                final String code =
                        String.valueOf(1000 + new Random().nextInt(9000));

                Toast.makeText(MainActivity.this,
                        "جاري إنشاء الغرفة...",
                        Toast.LENGTH_SHORT).show();

                apiPostRoom(code);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                home();
            }
        });
    }

    void apiPostRoom(final String code) {

        new Thread(new Runnable() {
            public void run() {

                try {

                    JSONObject room = new JSONObject();
                    room.put("code", code);

                    String result = request(
                            "POST",
                            BuildConfig.SUPABASE_URL +
                                    "/rest/v1/rooms",
                            room.toString()
                    );

                    addPlayer(code, playerName, new Runnable() {
                        public void run() {
                            roomCode = code;
                            roomScreen();
                        }
                    });

                } catch (final Exception e) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "خطأ في إنشاء الغرفة",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
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

        join.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                playerName = name.getText().toString().trim();
                roomCode = code.getText().toString().trim();

                if (playerName.length() == 0) {
                    playerName = "Player";
                }

                if (roomCode.length() != 4) {
                    Toast.makeText(MainActivity.this,
                            "أدخل كود من 4 أرقام",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                checkRoom();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                home();
            }
        });
    }

    void checkRoom() {

        final String code = roomCode;

        new Thread(new Runnable() {
            public void run() {

                try {

                    String url = BuildConfig.SUPABASE_URL +
                            "/rest/v1/rooms?code=eq." +
                            URLEncoder.encode(code, "UTF-8") +
                            "&select=*";

                    String result = request("GET", url, null);

                    JSONArray arr = new JSONArray(result);

                    if (arr.length() == 0) {

                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        "الغرفة غير موجودة",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {

                        addPlayer(code, playerName, new Runnable() {
                            public void run() {
                                roomScreen();
                            }
                        });
                    }

                } catch (Exception e) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "تعذر الاتصال بالسيرفر",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    void addPlayer(final String code,
                   final String name,
                   final Runnable after) {

        new Thread(new Runnable() {
            public void run() {

                try {

                    JSONObject player = new JSONObject();
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

                    runOnUiThread(after);

                } catch (final Exception e) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "تعذر إضافة اللاعب",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    void roomScreen() {

        base();

        content.addView(t("🎮 Room: " + roomCode, 28));

        final TextView players = t("جاري تحميل اللاعبين...", 20);
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

        ready.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setReady();
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                game();
            }
        });

        chat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chat();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                home();
            }
        });

        handler.postDelayed(new Runnable() {
            public void run() {
                if (roomCode.length() > 0) {
                    loadPlayers(players);
                    handler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }

    void loadPlayers(final TextView players) {

        new Thread(new Runnable() {
            public void run() {

                try {

                    String url = BuildConfig.SUPABASE_URL +
                            "/rest/v1/players?room_code=eq." +
                            URLEncoder.encode(roomCode, "UTF-8") +
                            "&select=name,score,ready";

                    final String result = request("GET", url, null);

                    runOnUiThread(new Runnable() {
                        public void run() {

                            try {

                                JSONArray arr = new JSONArray(result);

                                String text = "👥 Players • " +
                                        arr.length() + "/6\n\n";

                                for (int i = 0; i < arr.length(); i++) {

                                    JSONObject p = arr.getJSONObject(i);

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
                                players.setText("تعذر تحميل اللاعبين");
                            }
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            players.setText("تعذر الاتصال");
                        }
                    });
                }
            }
        }).start();
    }

    void setReady() {

        new Thread(new Runnable() {
            public void run() {

                try {

                    String url = BuildConfig.SUPABASE_URL +
                            "/rest/v1/players?room_code=eq." +
                            URLEncoder.encode(roomCode, "UTF-8") +
                            "&name=eq." +
                            URLEncoder.encode(playerName, "UTF-8");

                    JSONObject obj = new JSONObject();
                    obj.put("ready", true);

                    request("PATCH", url, obj.toString());

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "أنت Ready ✓",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "خطأ",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    void game() {

        base();

        TextView tm = t("⏱ 20", 20);
        tm.setGravity(Gravity.CENTER);
        content.addView(tm);

        TextView qu = t(qs[q % qs.length], 25);
        qu.setGravity(Gravity.CENTER);
        content.addView(qu);

        RadioGroup rg = new RadioGroup(this);

        String[] options = {"A", "B", "C", "D"};

        for (String o : options) {
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

        predict.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (rg.getCheckedRadioButtonId() == -1) {

                    Toast.makeText(MainActivity.this,
                            "اختار إجابة",
                            Toast.LENGTH_SHORT).show();

                } else {

                    score += 3;
                    results();
                }
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                q++;
                game();
            }
        });

        chat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chat();
            }
        });
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

        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                q++;
                game();
            }
        });

        rematch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                q = 0;
                score = 0;
                game();
            }
        });
    }

    void chat() {

        base();

        content.addView(t("💬 Chat", 28));

        final TextView messages = t("جاري تحميل الرسائل...", 18);
        content.addView(messages);

        final EditText input = new EditText(this);
        input.setHint("اكتب رسالة...");
        content.addView(input);

        Button send = b("Send");
        Button back = b("← Back");

        content.addView(send);
        content.addView(back);

        loadMessages(messages);

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String msg = input.getText().toString().trim();

                if (msg.length() == 0) return;

                sendMessage(msg);
                input.setText("");

                loadMessages(messages);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                roomScreen();
            }
        });

        handler.postDelayed(new Runnable() {
            public void run() {
                if (roomCode.length() > 0) {
                    loadMessages(messages);
                    handler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }

    void sendMessage(final String msg) {

        new Thread(new Runnable() {
            public void run() {

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
            }
        }).start();
    }

    void loadMessages(final TextView messages) {

        new Thread(new Runnable() {
            public void run() {

                try {

                    String url = BuildConfig.SUPABASE_URL +
                            "/rest/v1/messages?room_code=eq." +
                            URLEncoder.encode(roomCode, "UTF-8") +
                            "&select=player_name,message,created_at" +
                            "&order=created_at.asc";

                    final String result = request("GET", url, null);

                    runOnUiThread(new Runnable() {
                        public void run() {

                            try {

                                JSONArray arr = new JSONArray(result);

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
                                messages.setText("تعذر تحميل الرسائل");
                            }
                        }
                    });

                } catch (Exception ignored) {
                }
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
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(
                            CompoundButton button,
                            boolean checked) {
                        dark = checked;
                        settings();
                    }
                }
        );

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                home();
            }
        });
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
                "Bearer " + BuildConfig.SUPABASE_KEY
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

            OutputStream os = conn.getOutputStream();
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

        StringBuilder response = new StringBuilder();

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
