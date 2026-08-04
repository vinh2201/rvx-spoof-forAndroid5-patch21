package app.revanced.patches.youtube.general.layoutupdates

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.FREEZE_LAYOUT_UPDATES
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/FreezeLayoutUpdatesPatch;"

/**
 * Hàm hỗ trợ lấy chính xác tên thanh ghi đích (VD: "v1", "v3", "v4") từ bất kỳ loại lệnh Smali nào.
 */
private fun Instruction.getRegister(): String {
    val reg = when (this) {
        is OneRegisterInstruction -> registerA
        is TwoRegisterInstruction -> registerA
        is ThreeRegisterInstruction -> registerA
        is FiveRegisterInstruction -> registerC
        is RegisterRangeInstruction -> startRegister
        else -> error("Lệnh ${opcode.name} không hỗ trợ trích xuất thanh ghi A.")
    }
    return "v$reg"
}

@Suppress("unused")
val freezeLayoutUpdatesPatch = bytecodePatch(
    FREEZE_LAYOUT_UPDATES.title,
    FREEZE_LAYOUT_UPDATES.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        hotConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                // --- 1. HOT CONFIG GROUP ---
                val hotStoredTimestampStrIndex = match.stringMatches!!.first().index
                
                // Tìm ngược từ vị trí "hot_stored_timestamp", lấy lệnh move-result-object gần nhất
                val hotConfigGroupResultIndex = implementation!!.instructions.take(hotStoredTimestampStrIndex)
                    .indexOfLast { it.opcode == Opcode.MOVE_RESULT_OBJECT }
                
                val hotConfigReg = implementation!!.instructions.elementAt(hotConfigGroupResultIndex).getRegister()

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
                val hotHashDataResultIndex = indexOfFirstInstructionOrThrow(hotHashDataInvokeIndex, Opcode.MOVE_RESULT_OBJECT)
                
                val hotHashReg = implementation!!.instructions.elementAt(hotHashDataResultIndex).getRegister()

                addInstructions(
                    hotHashDataResultIndex + 1,
                    """
                        invoke-static {$hotHashReg}, $EXTENSION_CLASS_DESCRIPTOR->getHotHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $hotHashReg
                    """
                )
            }
        }

        coldConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                // --- 3. COLD HASH DATA ---
                val coldHashDataStrIndex = match.stringMatches!![2].index
                
                // Lấy lệnh iget-object gần nhất phía trước chuỗi cold_hash_data
                val coldHashIgetObjectIndex = implementation!!.instructions.take(coldHashDataStrIndex)
                    .indexOfLast { it.opcode == Opcode.IGET_OBJECT }
                
                val coldHashReg = implementation!!.instructions.elementAt(coldHashIgetObjectIndex).getRegister()

                addInstructions(
                    coldHashIgetObjectIndex + 1,
                    """
                        invoke-static {$coldHashReg}, $EXTENSION_CLASS_DESCRIPTOR->getColdHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $coldHashReg
                    """
                )

                // --- 4. COLD CONFIG GROUP ---
                val encodeToStringIndex = indexOfFirstInstructionOrThrow(match.stringMatches!![0].index) {
                    if (opcode != Opcode.INVOKE_STATIC && opcode != Opcode.INVOKE_STATIC_RANGE) return@indexOfFirstInstructionOrThrow false
                    val methodRef = (this as? ReferenceInstruction)?.reference as? MethodReference
                    methodRef?.definingClass == "Landroid/util/Base64;" && methodRef.name == "encodeToString"
                }

                val coldConfigGroupResultIndex = indexOfFirstInstructionOrThrow(encodeToStringIndex, Opcode.MOVE_RESULT_OBJECT)
                
                val coldConfigReg = implementation!!.instructions.elementAt(coldConfigGroupResultIndex).getRegister()

                addInstructions(
                    coldConfigGroupResultIndex + 1,
                    """
                        invoke-static {$coldConfigReg}, $EXTENSION_CLASS_DESCRIPTOR->getColdConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object $coldConfigReg
                    """
                )
            }
        }
    }
}