package app.revanced.extension.youtube.patches;

public class StartupNetworkDelayPatch {
    // Ghi nhận thời điểm class này được nạp vào bộ nhớ (lúc app khởi động)
    private static final long START_TIME = System.currentTimeMillis();
    private static final long DELAY_MS = 9000; // 5000 mili-giây = 5 giây

    /**
     * @return true nếu chưa qua 5 giây (đang giả mạo ngắt mạng)
     *         false nếu đã qua 5 giây (trả lại luồng chạy bình thường)
     */
    public static boolean isSpoofing() {
        return (System.currentTimeMillis() - START_TIME) < DELAY_MS;
    }
}