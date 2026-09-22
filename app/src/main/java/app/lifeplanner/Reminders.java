package app.lifeplanner;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** زمان‌بندی آلارم‌های سیستم اندروید برای یادآورها؛ حتی وقتی برنامه بسته است کار می‌کنند. */
final class Reminders {
    static final String CHANNEL = "reminders";
    private static final String PREFS = "lp_alarms";
    private static final String KEY = "list";

    private Reminders() {}

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "یادآورها", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("یادآورهای کارها و پایان جلسه‌ی تمرکز؛ صدا و لرزش را خود برنامه پخش می‌کند");
        // صدا و لرزش را AlarmRingService به‌صورت دستی (با تکرار) پخش می‌کند، پس کانال خودش چیزی پخش نمی‌کند.
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    private static PendingIntent pending(Context c, String id, String title, String body, int flag) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setData(Uri.parse("lp://reminder/" + Uri.encode(id)));
        i.putExtra("id", id);
        i.putExtra("title", title);
        i.putExtra("body", body);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, flag | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c, String id, String title, String body, long at) {
        if (at <= System.currentTimeMillis()) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent p = pending(c, id, title, body, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
    }

    /** لیست جدید آلارم‌ها را جایگزین لیست قبلی می‌کند. */
    static synchronized void sync(Context c, String json) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        try {
            JSONArray old = new JSONArray(sp.getString(KEY, "[]"));
            for (int k = 0; k < old.length(); k++) {
                String id = old.getJSONObject(k).getString("id");
                PendingIntent p = pending(c, id, "", "", PendingIntent.FLAG_NO_CREATE);
                if (p != null) {
                    am.cancel(p);
                    p.cancel();
                }
            }
        } catch (Exception ignored) {
        }
        sp.edit().putString(KEY, json).apply();
        scheduleAll(c, json);
    }

    static void scheduleAll(Context c, String json) {
        try {
            JSONArray a = new JSONArray(json);
            for (int k = 0; k < a.length(); k++) {
                JSONObject o = a.getJSONObject(k);
                schedule(c, o.getString("id"), o.optString("title"), o.optString("body"), (long) o.getDouble("at"));
            }
        } catch (Exception ignored) {
        }
    }

    static void rescheduleFromPrefs(Context c) {
        scheduleAll(c, c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
    }
}
