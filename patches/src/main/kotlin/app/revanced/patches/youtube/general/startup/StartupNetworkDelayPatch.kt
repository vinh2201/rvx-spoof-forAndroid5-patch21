package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/patches/StartupNetworkDelayPatch;"

@Suppress("unused")
val delayStartupNetworkPatch = bytecodePatch(
    STARTUP_NETWORK_DELAYED.title,
    STARTUP_NETWORK_DELAYED.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // 1. Hook vào onCreate để kích hoạt bộ đếm 5 giây
        mainActivityOnCreateFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }

        // 2. Quét an toàn toàn bộ các class/method trong APK tìm điểm nghẽn mạng
        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach
                val instructions = implementation.instructions.toList()

                // Tìm tất cả các vị trí gọi lệnh getActiveNetworkInfo
                val invokeIndices = mutableListOf<Int>()
                instructions.forEachIndexed { index, instruction ->
                    if (instruction.toString().contains("ConnectivityManager;->getActiveNetworkInfo")) {
                        invokeIndices.add(index)
                    }
                }

                if (invokeIndices.isNotEmpty()) {
                    // Duyệt ngược từ dưới lên để chèn code không bị lệch chỉ số index
                    invokeIndices.reversed().forEach { invokeIndex ->
                        val resultIndex = invokeIndex + 1
                        if (resultIndex < instructions.size) {
                            val moveInstruction = instructions[resultIndex]

                            if (moveInstruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                                val registerName = "v${(moveInstruction as OneRegisterInstruction).registerA}"

                                // Thực hiện add instructions trực tiếp trên method hiện tại
                                method.addInstructions(
                                    resultIndex + 1,
                                    """
                                        invoke-static {$registerName}, $EXTENSION_CLASS_DESCRIPTOR->getActiveNetworkInfo(Landroid/net/NetworkInfo;)Landroid/net/NetworkInfo;
                                        move-result-object $registerName
                                    """
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}