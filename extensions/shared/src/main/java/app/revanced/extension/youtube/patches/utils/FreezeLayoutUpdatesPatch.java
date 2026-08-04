package app.revanced.extension.youtube.patches.utils;

// import app.revanced.extension.youtube.settings.Settings;
import android.os.SystemClock;

@SuppressWarnings("unused")
public class FreezeLayoutUpdatesPatch {

    // Dùng elapsedRealtime an toàn hơn currentTimeMillis cho việc đếm giờ lúc khởi động
    private static final long START_TIME = SystemClock.elapsedRealtime();
    private static final long DELAY_MS = 15000; // 15 giây

    // Bộ nhớ đệm (Cache) để lưu trữ config lúc app mới mở
    private static String cachedHotConfigGroup = null;
    private static String cachedHotHashData = null;
    private static String cachedColdConfigGroup = null;
    private static String cachedColdHashData = null;

    /**
     * Kiểm tra xem đã qua 15 giây chưa
     */
    private static boolean isReadyToFreeze() {
        return (SystemClock.elapsedRealtime() - START_TIME) >= DELAY_MS;
    }

    public static String getHotConfigGroup(String original) {
        if (!isReadyToFreeze()) {
            // Trong 15s đầu: LƯU LẠI config gốc chuẩn bị cho việc đóng băng
            if (original != null && !original.isEmpty()) {
                cachedHotConfigGroup = original;
            }
            return original; // Vẫn cho app load bình thường
        }
        
        // Sau 15s: Bắt đầu đóng băng! 
        // Trả về dữ liệu đã lưu, nếu không có thì mới đành dùng đồ zin
        return (cachedHotConfigGroup != null) ? cachedHotConfigGroup : original;
    }

    public static String getHotHashData(String original) {
        if (!isReadyToFreeze()) {
            if (original != null && !original.isEmpty()) {
                cachedHotHashData = original;
            }
            return original;
        }
        return (cachedHotHashData != null) ? cachedHotHashData : original;
    }

    public static String getColdConfigGroup(String original) {
        if (!isReadyToFreeze()) {
            if (original != null && !original.isEmpty()) {
                cachedColdConfigGroup = original;
            }
            return original;
        }
        return (cachedColdConfigGroup != null) ? cachedColdConfigGroup : original;
    }

    public static String getColdHashData(String original) {
        if (!isReadyToFreeze()) {
            if (original != null && !original.isEmpty()) {
                cachedColdHashData = original;
            }
            return original;
        }
        return (cachedColdHashData != null) ? cachedColdHashData : original;
    }
}