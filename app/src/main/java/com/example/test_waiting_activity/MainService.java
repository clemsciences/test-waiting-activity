package com.example.test_waiting_activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainService extends Service implements MainServiceListener {

    private static MainService instance;
    private boolean mTestDelayElapsed = false;

    private Notification mNotif;
    private Notification mIncallNotif;
    private Notification mMsgNotif;
    private Notification mCustomNotif;
    private NotificationManager mNM;
    private int mMsgNotifCount;
    private PendingIntent mNotifContentIntent;
    private PendingIntent mkeepAlivePendingIntent;
    private String mNotificationTitle;
    private boolean mDisableRegistrationStatus;

    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;

    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    private Class<? extends Activity> incomingReceivedActivity = MainActivity.class;


    public static boolean isReady() {
        return instance != null && instance.mTestDelayElapsed;
    }

    public static MainService instance() {
        if(isReady()) {
            return instance;
        }
        throw new RuntimeException("MainService not instantiated yet");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e("main service", "created");
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNM.cancel(1); // in case of crash the icon is not removed
        instance = this;

        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("1", "Ma chaîne", NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }


        mNotif = createNotification(this, mNotificationTitle, "Le message", mNotifContentIntent, true);

        new Handler().postDelayed(() -> {
            mTestDelayElapsed = true;

            Log.d("main service", "après 5s");
        }, 5000);

        startForeground(1, mNotif);
    }

    public Notification createNotification(Context context, String title, String message, PendingIntent intent, boolean isOngoingEvent) {
        Notification notif;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");

        notif = builder
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(intent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        if (isOngoingEvent) {
            notif.flags |= Notification.FLAG_ONGOING_EVENT;
        }

        return notif;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public synchronized void onDestroy() {
        instance = null;
        MainManager.destroy();

        // Make sure our notification is gone.
        stopForegroundCompat(1);
//        mNM.cancel(INCALL_NOTIF_ID);
//        mNM.cancel(MESSAGE_NOTIF_ID);

//        ((AlarmManager) this.getSystemService(Context.ALARM_SERVICE)).cancel(mkeepAlivePendingIntent);
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForegroundCompat(1);
        stopSelf();
    }

    /**
     * Wrap notifier to avoid setting the app icons while the service
     * is stopping. When the (rare) bug is triggered, the app icon is
     * present despite the service is not running. To trigger it one could
     * stop app as soon as it is started. Transport configured with TLS.
     */
    private synchronized void notifyWrapper(int id, Notification notification) {
        if (instance != null && notification != null) {
            mNM.notify(id, notification);
        }
    }


    public void setActivityToLaunchOnIncomingReceived(Class<? extends Activity> activity) {
        incomingReceivedActivity = activity;
        resetIntentLaunchedOnNotificationClick();
    }

    private void resetIntentLaunchedOnNotificationClick() {
        Intent notifIntent = new Intent(this, incomingReceivedActivity);
        mNotifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (mNotif != null) {
            mNotif.contentIntent = mNotifContentIntent;
        }
        notifyWrapper(1, mNotif);
    }

    void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(this, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
//            Log.w(e, "Unable to invoke method");
        } catch (IllegalAccessException e) {
            // Should not happen.
//            Log.w(e, "Unable to invoke method");
        }
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        if (mSetForeground != null) {
            mSetForegroundArgs[0] = Boolean.FALSE;
            invokeMethod(mSetForeground, mSetForegroundArgs);
        }
    }

}
