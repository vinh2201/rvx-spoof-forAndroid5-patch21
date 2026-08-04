package app.revanced.extension.youtube.patches.utils;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class FreezeLayoutUpdatesPatch {

    private static final boolean enabled = Settings.FREEZE_LAYOUT_UPDATES.get();
    private static final boolean disableLayoutUpdates = Settings.DISABLE_LAYOUT_UPDATES.get();
    //public static boolean freezeTimestamp = Settings.FREEZE_LAYOUT_UPDATES_TIMESTAMP.get();

    // Thêm mốc thời gian bắt đầu khi class này được nạp vào bộ nhớ
    private static final long START_TIME = System.currentTimeMillis();
    private static final long DELAY_MS = 15000; // 15 giây = 15000 mili-giây

    /**
     * Hàm kiểm tra xem đã qua 15 giây kể từ lúc mở app hay chưa.
     */
    private static boolean isReadyToSpoof() {
        return (System.currentTimeMillis() - START_TIME) >= DELAY_MS;
    }


    public static String getHotConfigGroup(String original) {
        // Chỉ spoof nếu setting bật VÀ đã qua 15 giây
        if (enabled && isReadyToSpoof()) {
            String savedValue = Settings.FROZEN_HOT_CONFIG_GROUP.get();
            // Default value of config groups are null, but ReVanced's StringSetting doesn't support saving null.
            if (disableLayoutUpdates || savedValue.isEmpty()) {
                return null;
            }
            return savedValue;
        }
        // Trong 15s đầu hoặc nếu tắt setting, trả về giá trị gốc của YouTube
        return original;
    }

    public static String getHotHashData(String original) {
        if (enabled && isReadyToSpoof()) {
            if (disableLayoutUpdates) {
                return "";
            }
            return Settings.FROZEN_HOT_HASH_DATA.get();
        }
        return original;
    }

    public static String getColdConfigGroup(String original) {
        if (enabled && isReadyToSpoof()) {
            String savedValue = Settings.FROZEN_COLD_CONFIG_GROUP.get();
            // Default value of config groups are null, but ReVanced's StringSetting doesn't support saving null.
            if (disableLayoutUpdates || savedValue.isEmpty()) {
                return null;
            }
            return savedValue;
        }
        return original;
    }

    public static String getColdHashData(String original) {
        if (enabled && isReadyToSpoof()) {
            if (disableLayoutUpdates) {
                return "";
            }
            return Settings.FROZEN_COLD_HASH_DATA.get();
        }
        return original;
    }

    /*
    public static long getHotStoredTimestamp(long original) {
        if (enabled && disableLayoutUpdates && isReadyToSpoof()) {
            return -1;
        }
        if (enabled && freezeTimestamp && isReadyToSpoof()) {
            return Settings.FROZEN_HOT_STORED_TIMESTAMP.get();
        }
        return original;
    }

    public static long getColdStoredTimestamp(long original) {
        if (enabled && disableLayoutUpdates && isReadyToSpoof()) {
            return -1;
        }
        if (enabled && freezeTimestamp && isReadyToSpoof()) {
            return Settings.FROZEN_COLD_STORED_TIMESTAMP.get();
        }
        return original;
    }
    */
}