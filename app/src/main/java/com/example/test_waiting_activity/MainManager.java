package com.example.test_waiting_activity;

import android.content.Context;
import android.os.Handler;

public class MainManager {

    private static MainManager instance;

    private Context mServiceContext;
    private Handler mHandler = new Handler();
    private static boolean sExited;

    public static synchronized void destroy() {
        if (instance == null) return;
//        getInstance().changeStatusToOffline();
        sExited = true;
        instance.doDestroy();
    }

    public static synchronized final MainManager getInstance() {
        if (instance != null) return instance;

        if (sExited) {
            throw new RuntimeException("MainManager was already destroyed. "
                    + "Better use getLcIfManagerNotDestroyed and check returned value");
        }

        throw new RuntimeException("Main Manager should be created before accessed");
    }

    public static final boolean isInstanciated() {
        return instance != null;
    }


    private void doDestroy() {
        if (MainService.isReady()) // indeed, no need to crash

        // TODO destroy
        try {
//            mTimer.cancel();
//            mLc.destroy();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        finally {
//            mServiceContext.unregisterReceiver(instance.mKeepAliveReceiver);
//            mLc = null;
            instance = null;
        }
    }

}
