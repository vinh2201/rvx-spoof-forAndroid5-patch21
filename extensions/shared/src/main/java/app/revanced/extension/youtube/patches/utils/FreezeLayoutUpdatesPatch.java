package app.revanced.extension.youtube.patches.utils;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class FreezeLayoutUpdatesPatch {

    // Chỉ lưu đúng thời gian bắt đầu
    private static final long START_TIME = System.currentTimeMillis();
    private static final long DELAY_MS = 15000; // 15 giây

    /**
     * Hàm kiểm tra an toàn
     */
    private static boolean isReadyToSpoof() {
        return (System.currentTimeMillis() - START_TIME) >= DELAY_MS;
    }

    public static String getHotConfigGroup(String original) {
        // Trả về gốc ngay lập tức, tránh đụng vào Settings khi app chưa sẵn sàng
        if (!isReadyToSpoof()) return original; 
        
        try {
            if (Settings.FREEZE_LAYOUT_UPDATES.get()) {
                boolean disableLayoutUpdates = Settings.DISABLE_LAYOUT_UPDATES.get();
                String savedValue = Settings.FROZEN_HOT_CONFIG_GROUP.get();
                
                if (disableLayoutUpdates || savedValue == null || savedValue.isEmpty()) {
                    return null;
                }
                return savedValue;
            }
        } catch (Exception e) {
            // Lỗi thì nuốt luôn, trả về original cho an toàn
        }
        return original;
    }

    public static String getHotHashData(String original) {
        if (!isReadyToSpoof()) return original;
        
        try {
            if (Settings.FREEZE_LAYOUT_UPDATES.get()) {
                if (Settings.DISABLE_LAYOUT_UPDATES.get()) {
                    return "";
                }
                return Settings.FROZEN_HOT_HASH_DATA.get();
            }
        } catch (Exception e) {}
        return original;
    }

    public static String getColdConfigGroup(String original) {
        if (!isReadyToSpoof()) return original;
        
        try {
            if (Settings.FREEZE_LAYOUT_UPDATES.get()) {
                boolean disableLayoutUpdates = Settings.DISABLE_LAYOUT_UPDATES.get();
                String savedValue = Settings.FROZEN_COLD_CONFIG_GROUP.get();
                
                if (disableLayoutUpdates || savedValue == null || savedValue.isEmpty()) {
                    return null;
                }
                return savedValue;
            }
        } catch (Exception e) {}
        return original;
    }

    public static String getColdHashData(String original) {
        if (!isReadyToSpoof()) return original;
        
        try {
            if (Settings.FREEZE_LAYOUT_UPDATES.get()) {
                if (Settings.DISABLE_LAYOUT_UPDATES.get()) {
                    return "";
                }
                return Settings.FROZEN_COLD_HASH_DATA.get();
            }
        } catch (Exception e) {}
        return original;
    }
}