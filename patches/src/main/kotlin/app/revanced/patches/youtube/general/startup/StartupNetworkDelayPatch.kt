package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Đường dẫn trỏ tới file Java bên project Extension (Lapp/revanced/extension/youtube/StartupNetworkDelayPatch;)
private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/patches/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // 1. Hook vào onCreate
        mainActivityOnCreateFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }

        // 2. Hook vào hàm kiểm tra mạng cực kỳ cẩn thận
        networkInfoFingerprint.methodOrThrow().apply {
            val instructions = implementation!!.instructions.toList()

            // Tìm đích xác vị trí dòng lệnh gọi hàm mạng
            val invokeIndex = instructions.indexOfFirst {
                it.toString().contains("ConnectivityManager;->getActiveNetworkInfo")
            }
            if (invokeIndex == -1) error("Không tìm thấy lệnh getActiveNetworkInfo trong hàm này")

            // Trong cấu trúc Dalvik, lệnh nhận kết quả (move-result-object) luôn nằm NGAY SAU lệnh invoke
            val resultIndex = invokeIndex + 1
            val moveInstruction = instructions[resultIndex]

            // Double-check xem nó có đúng là lệnh move-result-object không
            if (moveInstruction.opcode != Opcode.MOVE_RESULT_OBJECT) {
                error("Lệnh theo sau không phải move-result-object, mà là: ${moveInstruction.opcode}")
            }

            // Trích xuất thanh ghi
            val registerName = "v${(moveInstruction as OneRegisterInstruction).registerA}"

            // Chèn lệnh giả lập mạng vào sau nó
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