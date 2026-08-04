package app.revanced.patches.youtube.general.layoutupdates

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.FREEZE_LAYOUT_UPDATES
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/FreezeLayoutUpdatesPatch;"

@Suppress("unused")
val freezeLayoutUpdatesPatch = bytecodePatch(
    FREEZE_LAYOUT_UPDATES.title,
    FREEZE_LAYOUT_UPDATES.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        hotConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                // Lấy danh sách toàn bộ các lệnh (instructions) trong method
                val instructions = implementation!!.instructions.toList()
                
                // --- 1. HOT CONFIG GROUP ---
                val hotStoredTimestampStrIndex = match.stringMatches!!.first().index
                
                // Tìm ngược từ vị trí chuỗi "hot_stored_timestamp", lấy lệnh move-result-object gần nhất (chính là getString của hot_config)
                val hotConfigGroupResultIndex = instructions.subList(0, hotStoredTimestampStrIndex)
                    .indexOfLast { it.opcode == Opcode.MOVE_RESULT_OBJECT }
                
                // Đọc thanh ghi đích động (VD: nó sẽ tự động ra v1, v4... tùy theo smali)
                val hotConfigReg = "v" + (instructions[hotConfigGroupResultIndex] as OneRegisterInstruction).registerA

                addInstructions(
                    hotConfigGroupResultIndex + 1,
                    """
                        invoke-static {$hotConfigReg}, $EXTENSION_CLASS_DESCRIPTOR->getHotConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $hotConfigReg
                    """
                )

                // --- 2. HOT HASH DATA ---
                val hotStoredTimestampResultIndex = indexOfFirstInstructionOrThrow(hotConfigGroupResultIndex, Opcode.MOVE_RESULT_WIDE)
                val hotHashDataInvokeIndex = indexOfFirstInstructionOrThrow(hotStoredTimestampResultIndex, Opcode.INVOKE_INTERFACE)
                // Thay vì +1, ta tìm chính xác lệnh MOVE_RESULT_OBJECT để lấy thanh ghi động
                val hotHashDataResultIndex = indexOfFirstInstructionOrThrow(hotHashDataInvokeIndex, Opcode.MOVE_RESULT_OBJECT)
                
                val hotHashReg = "v" + (instructions[hotHashDataResultIndex] as OneRegisterInstruction).registerA

                addInstructions(hotHashDataResultIndex + 1,
                    """
                        invoke-static {$hotHashReg}, $EXTENSION_CLASS_DESCRIPTOR->getHotHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $hotHashReg
                    """)
            }
        }

        coldConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                val instructions = implementation!!.instructions.toList()

                // --- 3. COLD HASH DATA ---
                val coldHashDataStrIndex = match.stringMatches!![2].index
                
                // Tìm ngược từ vị trí "cold_hash_data", lấy lệnh iget-object gần nhất (chính là lệnh gán data vào v3 trong smali của bạn)
                val coldHashIgetObjectIndex = instructions.subList(0, coldHashDataStrIndex)
                    .indexOfLast { it.opcode == Opcode.IGET_OBJECT }
                
                val coldHashReg = "v" + (instructions[coldHashIgetObjectIndex] as OneRegisterInstruction).registerA

                // Inject ngay sau khi lấy được chuỗi, để ghi đè nó bằng data của ta trước khi bị check textUtils.isEmpty
                addInstructions(coldHashIgetObjectIndex + 1,
                    """
                        invoke-static {$coldHashReg}, $EXTENSION_CLASS_DESCRIPTOR->getColdHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $coldHashReg
                    """)

                // --- 4. COLD CONFIG GROUP ---
                val encodeToStringIndex = indexOfFirstInstructionOrThrow(match.stringMatches!![0].index) {
                    if (opcode != Opcode.INVOKE_STATIC && opcode != Opcode.INVOKE_STATIC_RANGE) return@indexOfFirstInstructionOrThrow false
                    val methodRef = (this as? ReferenceInstruction)?.reference as? MethodReference
                    methodRef?.definingClass == "Landroid/util/Base64;" && methodRef.name == "encodeToString"
                }

                val coldConfigGroupResultIndex = indexOfFirstInstructionOrThrow(encodeToStringIndex, Opcode.MOVE_RESULT_OBJECT)
                
                // Thanh ghi này sẽ tự động bắt được "p1" (được biểu diễn dưới dạng vX theo dexlib2)
                val coldConfigReg = "v" + (instructions[coldConfigGroupResultIndex] as OneRegisterInstruction).registerA

                addInstructions(coldConfigGroupResultIndex + 1,
                    """
                        invoke-static {$coldConfigReg}, $EXTENSION_CLASS_DESCRIPTOR->getColdConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $coldConfigReg
                    """)
            }
        }
    }
}