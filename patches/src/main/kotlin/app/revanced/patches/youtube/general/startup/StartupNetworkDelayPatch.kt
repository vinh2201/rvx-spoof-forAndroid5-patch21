package app.revanced.patches.youtube.general.startup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.STARTUP_NETWORK_DELAYED
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Đã thêm /patches/ vào đường dẫn để khớp với thư mục Java của bạn
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

        // 2. Tự động quét toàn bộ APK để patch TẤT CẢ các chỗ gọi hàm mạng (Thay thế cho Fingerprint)
        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach
                val instructions = implementation.instructions.toList()

                // Dò xem hàm này có gọi getActiveNetworkInfo không?
                val invokeIndices = instructions.mapIndexedNotNull { index, instruction ->
                    if (instruction.toString().contains("ConnectivityManager;->getActiveNetworkInfo")) index else null
                }

                // Nếu có, duyệt ngược từ dưới lên để chèn code (chống lệch dòng)
                invokeIndices.reversed().forEach { invokeIndex ->
                    val resultIndex = invokeIndex + 1
                    
                    if (resultIndex < instructions.size) {
                        val moveInstruction = instructions[resultIndex]

                        // Đảm bảo lệnh tiếp theo đúng là lưu kết quả mạng
                        if (moveInstruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                            // Trích xuất đúng thanh ghi (v0, v1, v8...) của từng chỗ
                            val registerName = "v${(moveInstruction as OneRegisterInstruction).registerA}"

                            // Bơm code giả lập offline 5s vào
                            method.apply {
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
                }
            }
        }
    }
}