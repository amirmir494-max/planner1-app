package app.lifeplanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** صفحه‌ی تمام‌صفحه‌ی زنگ یادآور؛ حتی روی صفحه‌ی قفل باز می‌شود، شبیه اپ ساعت زنگ‌دار. */
public class AlarmActivity extends Activity {
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String title = getIntent().getStringExtra("title");
        String body = getIntent().getStringExtra("body");
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (28 * density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#5B5BF0"));
        root.setPadding(pad, pad, pad, pad);

        TextView bell = new TextView(this);
        bell.setText("\u23F0");
        bell.setTextSize(64);
        bell.setGravity(Gravity.CENTER);
        root.addView(bell);

        TextView t = new TextView(this);
        t.setText(title == null || title.isEmpty() ? "یادآوری" : title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(24);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, pad, 0, 8);
        root.addView(t);

        if (body != null && !body.isEmpty()) {
            TextView d = new TextView(this);
            d.setText(body);
            d.setTextColor(Color.parseColor("#E4E6FF"));
            d.setTextSize(16);
            d.setGravity(Gravity.CENTER);
            root.addView(d);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, pad * 2, 0, 0);

        Button snooze = new Button(this);
        snooze.setText("۱۰ دقیقه دیگر");
        snooze.setOnClickListener(v -> {
            sendAction(AlarmRingService.ACTION_SNOOZE);
            finish();
        });

        Button stop = new Button(this);
        stop.setText("خاموش کردن");
        stop.setOnClickListener(v -> {
            sendAction(AlarmRingService.ACTION_STOP);
            finish();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        int m = (int) (6 * density);
        lp.setMargins(m, 0, m, 0);
        row.addView(snooze, lp);
        row.addView(stop, lp);
        root.addView(row);

        setContentView(root);
    }

    private void sendAction(String action) {
        Intent i = new Intent(this, AlarmRingService.class).setAction(action);
        startService(i);
    }
}
