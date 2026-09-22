package app.lifeplanner;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** پل بین جاوااسکریپت برنامه و امکانات اندروید (شیء Android در صفحه). */
final class Bridge {
    private final MainActivity a;

    Bridge(MainActivity a) {
        this.a = a;
    }

    @JavascriptInterface
    public void syncReminders(String json) {
        Reminders.sync(a, json);
    }

    @JavascriptInterface
    public void scheduleReminder(String id, String title, String body, double at) {
        Reminders.schedule(a, id, title, body, (long) at);
    }

    @JavascriptInterface
    public void requestPermissions() {
        a.runOnUiThread(a::askPermissions);
    }

    @JavascriptInterface
    public void openBatterySettings() {
        a.runOnUiThread(() -> {
            try {
                a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {
            }
        });
    }

    /** فایل را در پوشه‌ی Downloads ذخیره می‌کند؛ فایل‌های تقویم (ICS) بلافاصله در تقویم باز می‌شوند. */
    @JavascriptInterface
    public boolean saveFile(String name, String data) {
        try {
            String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
            String mime = safe.endsWith(".csv") ? "text/csv"
                    : safe.endsWith(".ics") ? "text/calendar" : "application/json";
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = a.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return false;
                try (OutputStream o = a.getContentResolver().openOutputStream(uri)) {
                    if (o == null) return false;
                    o.write(bytes);
                }
                if (safe.endsWith(".ics")) {
                    final Uri u = uri;
                    a.runOnUiThread(() -> {
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setDataAndType(u, "text/calendar");
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            a.startActivity(i);
                        } catch (Exception ignored) {
                        }
                    });
                }
            } else {
                File dir = a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) return false;
                dir.mkdirs();
                try (FileOutputStream o = new FileOutputStream(new File(dir, safe))) {
                    o.write(bytes);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
