package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod

// Đường dẫn trỏ tới class Java ta vừa viết ở trên
private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/patches/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // Tìm hàm kiểm tra mạng bằng Fingerprint
        networkCheckFingerprint.methodOrThrow().apply {
            (this as MutableMethod).addInstructions(
                0, // Tiêm ngay vào dòng đầu tiên của hàm
                """
                    # Gọi hàm Java của chúng ta để kiểm tra xem có đang trong 5 giây đầu không
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isSpoofing()Z
                    move-result v0
                    
                    # Nếu isSpoofing trả về 0 (false - đã qua 5s), nhảy tới nhãn :continue_flow để chạy mạng bình thường
                    if-nez v0, :continue_flow
                    
                    # Nếu chưa qua 5 giây, ép thanh ghi v0 thành 0 (false) và return luôn -> App tưởng mất mạng
                    const/4 v0, 0x0
                    return v0
                    
                    # Nhãn tiếp tục luồng bình thường của app
                    :continue_flow
                """
            )
        }
    }
}