package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // 1. Hook vào MainActivity.onCreate để kích hoạt bộ đếm 5 giây
        mainActivityOnCreateFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }

        // 2. Hook vào hàm kiểm tra mạng của YouTube
        networkInfoFingerprint.methodOrThrow().apply {
            val resultIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT_OBJECT)
            addInstructions(
                resultIndex + 1,
                """
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->getActiveNetworkInfo(Landroid/net/NetworkInfo;)Landroid/net/NetworkInfo;
                    move-result-object v0
                """
            )
        }
    }
}