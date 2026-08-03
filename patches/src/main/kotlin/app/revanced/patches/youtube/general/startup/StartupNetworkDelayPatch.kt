package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/patches/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // Chỉ cần kích hoạt đồng hồ đếm ngược 3 giây khi Activity chính được khởi tạo
        mainActivityOnCreateFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }
        
        // Không quét mù toàn bộ APK nữa để đảm bảo sự ổn định tuyệt đối cho các luồng xử lý ngầm của YouTube.
    }
}