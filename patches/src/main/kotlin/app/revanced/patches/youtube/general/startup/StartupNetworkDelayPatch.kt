package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // 1. Hook vào onCreate của WatchWhileActivity / MainActivity để kích hoạt bộ đếm 5 giây
        mainActivityOnCreateFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }

        // 2. Hook vào hàm kiểm tra mạng của YouTube (xử lý động thanh ghi)
        networkInfoFingerprint.methodOrThrow().apply {
            // Tìm vị trí lệnh MOVE_RESULT_OBJECT (nơi lưu kết quả của getActiveNetworkInfo)
            val resultIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT_OBJECT)

            // Trích xuất chính xác thanh ghi thực tế (v0, v1, v8...) mà YouTube đang sử dụng
            val moveInstruction = implementation!!.instructions.elementAt(resultIndex) as OneRegisterInstruction
            val registerName = "v${moveInstruction.register}"

            // Chèn lệnh chẩn đoán mạng với đúng thanh ghi vừa bóc tách
            addInstructions(
                resultIndex + 1,
                """
                    invoke-static {$registerName}, $EXTENSION_CLASS_DESCRIPTOR->getActiveNetworkInfo(Landroid/net/NetworkInfo;)Landroid/net/NetworkInfo;
                    move-result-object $registerName
                """
            )
        }
    }
}