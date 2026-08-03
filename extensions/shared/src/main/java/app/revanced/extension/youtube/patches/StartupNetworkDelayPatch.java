package app.revanced.extension.youtube.patches;

import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

public class StartupNetworkDelayPatch {
    private static volatile boolean isStartupDelay = true;

    // Gọi hàm này ngay khi MainActivity onCreate
    public static void startStartupTimer() {
        isStartupDelay = true;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                isStartupDelay = false;
            }
        }, 5000); // 5000ms = 5 giây
    }

    // Hook lồng vào kết quả của ConnectivityManager.getActiveNetworkInfo()
    public static NetworkInfo getActiveNetworkInfo(NetworkInfo originalInfo) {
        if (isStartupDelay) {
            return null; // Giả lập chưa có kết nối mạng trong 5s đầu
        }
        return originalInfo; // Hết 5s trả về kết nối mạng thực tế
    }
}