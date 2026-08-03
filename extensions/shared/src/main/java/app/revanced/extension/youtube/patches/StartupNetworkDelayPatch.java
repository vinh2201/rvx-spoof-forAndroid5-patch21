package app.revanced.extension.youtube.patches;

import android.os.SystemClock;

public class StartupNetworkDelayPatch {
    static {
        // Khối tĩnh này tự chạy ngay khi class được nạp vào bộ nhớ, 
        // tạm hoãn luồng khởi động 5 giây một cách an toàn mà không làm văng app.
        try {
            SystemClock.sleep(9000);
        } catch (Exception ignored) {}
    }

    public static void init() {
        // Chỉ dùng để kích hoạt khối static ở trên
    }
}