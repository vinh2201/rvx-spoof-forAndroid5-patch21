package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/patches/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // Hook an toàn sau super.onCreate để kích hoạt bộ đếm thời gian 5 giây
        val onCreateMethod = mainActivityOnCreateFingerprint.methodOrThrow()
        val implementation = onCreateMethod.implementation
        
        if (implementation != null) {
            val instructions = implementation.instructions.toList()
            val superOnCreateIdx = instructions.indexOfFirst { it.toString().contains("->onCreate(") }
            val targetIndex = if (superOnCreateIdx != -1) superOnCreateIdx + 1 else 0
            
            (onCreateMethod as MutableMethod).addInstructions(
                targetIndex,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }
        // Đã cắt bỏ hoàn toàn việc quét bytecode hàng loạt để cứu con máy Android 5 khỏi lỗi VerifyError.
    }
}