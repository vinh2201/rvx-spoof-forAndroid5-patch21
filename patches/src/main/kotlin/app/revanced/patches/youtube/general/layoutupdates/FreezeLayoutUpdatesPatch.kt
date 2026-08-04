package app.revanced.patches.youtube.general.layoutupdates

// BẮT BUỘC PHẢI THÊM IMPORT NÀY VÀO ĐỂ DÙNG RADAR TÌM HÀM
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.FREEZE_LAYOUT_UPDATES
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/FreezeLayoutUpdatesPatch;"

@Suppress("unused")
val freezeLayoutUpdatesPatch = bytecodePatch(
    FREEZE_LAYOUT_UPDATES.title,
    FREEZE_LAYOUT_UPDATES.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)
    // dependsOn(settingsPatch)

    execute {
        // ==========================================
        // 1. XỬ LÝ HOT CONFIG (TÌM ĐÚNG HÀM GETSTRING)
        // ==========================================
        hotConfigPreferenceFingerprint.matchOrThrow().let { match ->
            val instructions = match.method.instructions.toList()
            
            // Dò radar tìm đích danh hàm getString()
            val getStringIndex = instructions.indexOfFirst {
                it is ReferenceInstruction && it.reference.toString().contains("Landroid/content/SharedPreferences;->getString")
            }
            
            if (getStringIndex != -1) {
                // Tìm move-result-object v1 ngay sau getString
                val hotConfigGroupResultIndex = indexOfFirstInstructionOrThrow(getStringIndex, Opcode.MOVE_RESULT_OBJECT)
                
                addInstructions(
                    hotConfigGroupResultIndex + 1,
                    """
                        invoke-static {v1}, $EXTENSION_CLASS_DESCRIPTOR->getHotConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v1
                    """
                )
            }
        }

        // ==========================================
        // 2. XỬ LÝ COLD CONFIG (TÌM ĐÚNG BASE64 VÀ NÉ TRY/CATCH)
        // ==========================================
        coldConfigPreferenceFingerprint.matchOrThrow().let { match ->
            val instructions = match.method.instructions.toList()
            
            // --- Xử lý cho cold_hash_data ---
            // Tránh chèn vào giữa block try/catch, ta chèn NGAY TRƯỚC dòng const-string (khi v3 đã lấy xong chuỗi nhưng chưa lưu)
            val coldHashDataStringIndex = match.stringMatches!![2].index
            addInstructions(
                coldHashDataStringIndex, // Chú ý: Không có + 1 ở đây, chèn đè lên trước
                """
                    invoke-static {v3}, $EXTENSION_CLASS_DESCRIPTOR->getColdHashData(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v3
                """
            )

            // --- Xử lý cho cold_config_group ---
            // Dò radar tìm đích danh hàm dịch Base64
            val base64Index = instructions.indexOfFirst {
                it is ReferenceInstruction && it.reference.toString().contains("Landroid/util/Base64;->encodeToString")
            }
            
            if (base64Index != -1) {
                // Tìm move-result-object p1 ngay sau Base64
                val p1ResultIndex = indexOfFirstInstructionOrThrow(base64Index, Opcode.MOVE_RESULT_OBJECT)
                
                addInstructions(
                    p1ResultIndex + 1,
                    """
                        invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->getColdConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p1
                    """
                )
            }
        }

/*
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SPOOFING",
                "SETTINGS: FREEZE_LAYOUT_UPDATES"
            ),
            FREEZE_LAYOUT_UPDATES
        )
*/

    }
}