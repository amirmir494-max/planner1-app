package app.lifeplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** با رسیدن زمان یادآور، سرویس زنگ‌زدن (صدا + لرزش + صفحه‌ی تمام‌صفحه) را روشن می‌کند. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String id = i.getStringExtra("id");
        if (id == null) return;
        String title = i.getStringExtra("title");
        String body = i.getStringExtra("body");

        Intent svc = new Intent(c, AlarmRingService.class);
        svc.setAction(AlarmRingService.ACTION_RING);
        svc.putExtra("id", id);
        svc.putExtra("title", title == null || title.isEmpty() ? "یادآوری" : title);
        svc.putExtra("body", body == null ? "" : body);
        if (Build.VERSION.SDK_INT >= 26) {
            c.startForegroundService(svc);
        } else {
            c.startService(svc);
        }
    }
}

