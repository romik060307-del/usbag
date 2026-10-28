package com.rk.tunnel;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText originInput;
    private TextView stateText;
    private TextView publicUrlText;
    private TextView logText;
    private Button startButton;
    private Button stopButton;
    private BroadcastReceiver receiver;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(TunnelService.EXTRA_STATUS);
                String url = intent.getStringExtra(TunnelService.EXTRA_URL);
                String log = intent.getStringExtra(TunnelService.EXTRA_LOG);
                if (status != null) stateText.setText(status);
                if (url != null) publicUrlText.setText(url);
                if (log != null) logText.setText(log);
                boolean active = intent.getBooleanExtra(TunnelService.EXTRA_ACTIVE, false);
                startButton.setEnabled(!active);
                stopButton.setEnabled(active);
            }
        };
        IntentFilter filter = new IntentFilter(TunnelService.ACTION_STATUS);
        registerReceiver(receiver, filter);
        sendCommand(TunnelService.ACTION_QUERY, null);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(244, 247, 246));
        scroll.addView(root);

        TextView title = text("RK TUNNEL LITE", 26, Color.rgb(18, 61, 53), true);
        root.addView(title);
        TextView subtitle = text("Publikasikan server lokal ke internet", 15, Color.rgb(89, 104, 99), false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(4);
        root.addView(subtitle, subLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.topMargin = dp(20);
        root.addView(card, cardLp);

        card.addView(text("1. Alamat layanan lokal", 16, Color.rgb(29, 48, 43), true));
        TextView hint = text("Alamat awal diambil dari Wi-Fi tablet ini. Cocokkan dengan alamat IP yang tampil pada aplikasi RKM Server; server harus aktif di port 8080.", 13, Color.rgb(99, 112, 107), false);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.topMargin = dp(7);
        card.addView(hint, hintLp);
        originInput = new EditText(this);
        originInput.setSingleLine(true);
        originInput.setText(suggestLocalOrigin());
        originInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        originInput.setTextSize(15);
        originInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, -2);
        inputLp.topMargin = dp(10);
        card.addView(originInput, inputLp);

        TextView step2 = text("2. Link publik", 16, Color.rgb(29, 48, 43), true);
        LinearLayout.LayoutParams stepLp = new LinearLayout.LayoutParams(-1, -2);
        stepLp.topMargin = dp(22);
        card.addView(step2, stepLp);

        stateText = text("Status: siap", 14, Color.rgb(60, 81, 73), true);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(8);
        card.addView(stateText, statusLp);

        publicUrlText = text("Link publik akan muncul di sini", 15, Color.rgb(24, 125, 103), true);
        publicUrlText.setTextIsSelectable(true);
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(-1, -2);
        urlLp.topMargin = dp(9);
        card.addView(publicUrlText, urlLp);

        startButton = button("AKTIFKAN SERVER PUBLIK", Color.rgb(24, 125, 103));
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(-1, dp(50));
        startLp.topMargin = dp(18);
        card.addView(startButton, startLp);
        startButton.setOnClickListener(v -> startTunnel());

        stopButton = button("MATIKAN TUNNEL", Color.rgb(87, 99, 94));
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(-1, dp(46));
        stopLp.topMargin = dp(8);
        card.addView(stopButton, stopLp);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> sendCommand(TunnelService.ACTION_STOP, null));

        Button copyButton = button("SALIN LINK PUBLIK", Color.rgb(34, 53, 47));
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(-1, dp(46));
        copyLp.topMargin = dp(8);
        card.addView(copyButton, copyLp);
        copyButton.setOnClickListener(v -> {
            String link = publicUrlText.getText().toString().trim();
            if (!link.startsWith("https://") || !link.contains("trycloudflare.com")) {
                Toast.makeText(this, "Link publik belum tersedia", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("RK Tunnel public URL", link));
            Toast.makeText(this, "Link publik disalin", Toast.LENGTH_SHORT).show();
        });

        LinearLayout logCard = new LinearLayout(this);
        logCard.setOrientation(LinearLayout.VERTICAL);
        logCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        logCard.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams logCardLp = new LinearLayout.LayoutParams(-1, -2);
        logCardLp.topMargin = dp(14);
        root.addView(logCard, logCardLp);
        logCard.addView(text("Keterangan", 15, Color.rgb(29, 48, 43), true));
        logText = text("Server lokal tidak dibuat oleh aplikasi ini. Jalankan RK Server lebih dahulu, lalu aktifkan tunnel. Link Quick Tunnel bersifat sementara dan dapat berubah.", 13, Color.rgb(99, 112, 107), false);
        logText.setTextIsSelectable(true);
        LinearLayout.LayoutParams logLp = new LinearLayout.LayoutParams(-1, -2);
        logLp.topMargin = dp(8);
        logCard.addView(logText, logLp);

        TextView footer = text("RK Tunnel Lite • untuk Android 4.4.2/API 19 ke atas • tunnel sementara untuk pengujian", 12, Color.rgb(114, 127, 121), false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2);
        footerLp.topMargin = dp(18);
        root.addView(footer, footerLp);
        setContentView(scroll);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button button(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundColor(color);
        return b;
    }


    private String suggestLocalOrigin() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null && wifi.getConnectionInfo() != null) {
                int ip = wifi.getConnectionInfo().getIpAddress();
                if (ip != 0) {
                    String address = (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
                    return "http://" + address + ":8080/";
                }
            }
        } catch (Exception ignored) { }
        return "http://192.168.1.5:8080/";
    }

    private void startTunnel() {
        String url = originInput.getText().toString().trim();
        Uri parsed = Uri.parse(url);
        if (!("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme())) || parsed.getHost() == null) {
            Toast.makeText(this, "Masukkan alamat lokal yang valid, misalnya http://192.168.1.5:8080/", Toast.LENGTH_LONG).show();
            return;
        }
        publicUrlText.setText("Menghubungkan… link akan muncul jika tunnel berhasil");
        logText.setText("Sedang memulai cloudflared dan menghubungkan ke layanan lokal.");
        sendCommand(TunnelService.ACTION_START, url);
    }

    private void sendCommand(String action, String url) {
        Intent intent = new Intent(this, TunnelService.class);
        intent.setAction(action);
        if (url != null) intent.putExtra(TunnelService.EXTRA_ORIGIN, url);
        if (Build.VERSION.SDK_INT >= 26 && TunnelService.ACTION_START.equals(action)) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override public void onDestroy() {
        try { if (receiver != null) unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
