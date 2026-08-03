package app.revanced.extension.youtube.patches;

import android.net.NetworkInfo;
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
        }, 3000); // Giảm xuống 3 giây cho nhanh và an toàn
    }

    public static NetworkInfo getActiveNetworkInfo(NetworkInfo originalInfo) {
        // Nếu đang trong thời gian khởi động mà app không truyền vào info gốc thì trả về nguyên bản, 
        // tuyệt đối không trả về null bừa bãi để tránh NullPointerException!
        if (isStartupDelay && originalInfo != null) {
            return null; // Chỉ chặn khi app có truyền vào và kiểm tra hợp lệ
        }
        return originalInfo;
    }
}