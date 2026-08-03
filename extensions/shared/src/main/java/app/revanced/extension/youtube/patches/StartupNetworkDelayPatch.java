package app.revanced.extension.youtube.patches;

import android.os.Handler;
import android.os.Looper;

public class StartupNetworkDelayPatch {
    private static volatile boolean isStartupDelay = true;

    public static void startStartupTimer() {
        isStartupDelay = true;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                isStartupDelay = false;
            }
        }, 9000);
    }

    // Chặn kết quả kiểm tra mạng thành false trong 5 giây đầu, không làm sập app
    public static boolean isConnected(boolean originalResult) {
        if (isStartupDelay) {
            return false;
        }
        return originalResult;
    }
}