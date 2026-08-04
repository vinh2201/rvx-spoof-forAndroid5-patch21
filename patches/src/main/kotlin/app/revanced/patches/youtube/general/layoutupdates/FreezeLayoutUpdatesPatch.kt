package app.revanced.patches.youtube.general.layoutupdates

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
        // 1. XỬ LÝ HOT CONFIG (Chặn lúc ĐỌC - READ)
        // ==========================================
        hotConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                // Trong aauq.smali (hàm run):
                // Lệnh move-result-object thứ 1 là của Lazyx->get() (thanh ghi v2)
                // Lệnh move-result-object thứ 2 là của SharedPreferences->getString() (thanh ghi v1)
                val firstMoveResult = indexOfFirstInstructionOrThrow(0, Opcode.MOVE_RESULT_OBJECT)
                val hotConfigGroupResultIndex = indexOfFirstInstructionOrThrow(firstMoveResult + 1, Opcode.MOVE_RESULT_OBJECT)

                // Inject giả mạo vào thanh ghi v1
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
        // 2. XỬ LÝ COLD CONFIG (Chặn lúc GHI - WRITE)
        // ==========================================
        coldConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                
                // --- Xử lý cho cold_hash_data ---
                // Smali: const-string v5, "cold_hash_data". Dữ liệu ghi đang nằm ở v3.
                // Vị trí chuỗi "cold_hash_data" là index 2 trong list strings khai báo ở fingerprint.
                val coldHashDataStringIndex = match.stringMatches!![2].index
                
                // Inject tráo dữ liệu v3 ngay sau khi nó được chuẩn bị
                addInstructions(
                    coldHashDataStringIndex + 1,
                    """
                        invoke-static {v3}, $EXTENSION_CLASS_DESCRIPTOR->getColdHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v3
                    """
                )

                // --- Xử lý cho cold_config_group ---
                // Smali: Dữ liệu được encode Base64 và trả về lệnh "move-result-object p1".
                // Vị trí chuỗi "cold_config_group" là index 0 trong list.
                val coldConfigGroupStringIndex = match.stringMatches!![0].index
                
                // Tính từ dòng khai báo chuỗi "cold_config_group", sẽ có 2 lệnh trả về Object:
                // 1: parseFrom (trả về Laqdy; -> v3)
                // 2: Base64.encodeToString (trả về String -> p1)
                val firstObjectResult = indexOfFirstInstructionOrThrow(coldConfigGroupStringIndex, Opcode.MOVE_RESULT_OBJECT)
                val secondObjectResult = indexOfFirstInstructionOrThrow(firstObjectResult + 1, Opcode.MOVE_RESULT_OBJECT)
                
                // Inject tráo chuỗi p1 sau khi decode Base64 thành công và trước khi ghi
                addInstructions(
                    secondObjectResult + 1,
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