package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
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
        // 1. Kích hoạt đồng hồ đếm ngược 5 giây khi MainActivity khởi tạo
        mainActivityOnCreateFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }

        // 2. Quét và đánh lừa các hàm kiểm tra isConnected() trong toàn bộ ứng dụng
        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach
                val instructions = implementation.instructions.toList()

                val matchIndices = mutableListOf<Int>()
                instructions.forEachIndexed { index, instruction ->
                    if (instruction.toString().contains("Landroid/net/NetworkInfo;->isConnected()Z")) {
                        matchIndices.add(index)
                    }
                }

                if (matchIndices.isNotEmpty()) {
                    val mutableMethod = method as MutableMethod

                    matchIndices.reversed().forEach { invokeIndex ->
                        val resultIndex = invokeIndex + 1
                        if (resultIndex < instructions.size) {
                            val moveInstruction = instructions[resultIndex]

                            // Bắt kết quả trả về kiểu boolean (Z) của lệnh isConnected()
                            if (moveInstruction.opcode == Opcode.MOVE_RESULT) {
                                val registerName = "v${(moveInstruction as OneRegisterInstruction).registerA}"

                                mutableMethod.addInstructions(
                                    resultIndex + 1,
                                    """
                                        invoke-static {$registerName}, $EXTENSION_CLASS_DESCRIPTOR->isConnected(Z)Z
                                        move-result $registerName
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