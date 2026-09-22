package app.lifeplanner;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * برخلاف یک اعلان ساده، این سرویس مثل زنگ ساعت واقعی عمل می‌کند: صدای بلند و
 * تکرارشونده پخش می‌کند، گوشی را می‌لرزاند و صفحه‌ی هشدار را حتی روی صفحه‌ی
 * قفل باز می‌کند، تا وقتی که کاربر «توقف» یا «تعویق» را بزند (یا حداکثر ۶۰ ثانیه بگذرد).
 */
public class AlarmRingService extends Service {
    static final String ACTION_RING = "app.lifeplanner.RING";
    static final String ACTION_STOP = "app.lifeplanner.STOP";
    static final String ACTION_SNOOZE = "app.lifeplanner.SNOOZE";
    private static final int NOTIF_ID = 9001;
    private static final long AUTO_STOP_MS = 60_000;

    private MediaPlayer player;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String curId, curTitle, curBody;

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopRingingAndSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_SNOOZE.equals(action)) {
            if (curId != null) {
                Reminders.schedule(this, curId, curTitle, curBody, System.currentTimeMillis() + 10 * 60_000L);
            }
            stopRingingAndSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            curId = intent.getStringExtra("id");
            curTitle = intent.getStringExtra("title");
            curBody = intent.getStringExtra("body");
        }
        Reminders.ensureChannel(this);
        startForeground(NOTIF_ID, buildNotification());
        startRing();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::stopRingingAndSelf, AUTO_STOP_MS);
        return START_NOT_STICKY;
    }

    private PendingIntent serviceAction(String action, int req) {
        Intent i = new Intent(this, AlarmRingService.class).setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, req, i, flags);
    }

    private Notification buildNotification() {
        Intent full = new Intent(this, AlarmActivity.class);
        full.putExtra("title", curTitle);
        full.putExtra("body", curBody);
        full.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        int reqCode = curId == null ? 0 : curId.hashCode();
        PendingIntent fullPi = PendingIntent.getActivity(this, reqCode, full,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_bell)
                .setContentTitle(curTitle)
                .setContentText(curBody)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(fullPi, true)
                .setContentIntent(fullPi)
                .addAction(0, "۱۰ دقیقه دیگر", serviceAction(ACTION_SNOOZE, 2))
                .addAction(0, "توقف", serviceAction(ACTION_STOP, 1))
                .build();
    }

    private void startRing() {
        try {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player = new MediaPlayer();
            player.setDataSource(this, uri);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception ignored) {
        }
        try {
            vibrator = getSystemService(Vibrator.class);
            long[] pattern = {0, 500, 400};
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) {
        }
    }

    private void stopRingingAndSelf() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try {
                player.stop();
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopRingingAndSelf();
        super.onDestroy();
    }
}
