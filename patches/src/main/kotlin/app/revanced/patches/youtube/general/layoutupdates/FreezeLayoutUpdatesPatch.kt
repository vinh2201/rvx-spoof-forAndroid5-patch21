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
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/FreezeLayoutUpdatesPatch;"

@Suppress("unused")
val freezeLayoutUpdatesPatch = bytecodePatch(
    FREEZE_LAYOUT_UPDATES.title,
    FREEZE_LAYOUT_UPDATES.summary
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    // dependsOn(settingsPatch)

    execute {
        hotConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                val hotConfigGroupResultIndex = match.stringMatches!!.first().index - 2
                addInstructions(
                    hotConfigGroupResultIndex + 1,
                    """
                        invoke-static {v1}, $EXTENSION_CLASS_DESCRIPTOR->getHotConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v1
                    """
                )

                val hotStoredTimestampResultIndex = indexOfFirstInstructionOrThrow(hotConfigGroupResultIndex, Opcode.MOVE_RESULT_WIDE)
                /* This looks like not necessary.
                addInstructions(hotStoredTimestampResultIndex + 1,
                    """
                        invoke-static {v4, v5}, $EXTENSION_CLASS_DESCRIPTOR->getHotStoredTimestamp(J)J
                        move-result-wide v4
                    """)
                 */

                val hotHashDataResultIndex = indexOfFirstInstructionOrThrow(hotStoredTimestampResultIndex, Opcode.INVOKE_INTERFACE) + 1
                addInstructions(hotHashDataResultIndex + 1,
                    """
                        invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->getHotHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v0
                    """)
            }
        }

        coldConfigPreferenceFingerprint.matchOrThrow().let { match ->
            match.method.apply {
                val coldHashDataResultIndex = match.stringMatches!![2].index - 2
                addInstructions(coldHashDataResultIndex,
                    """
                        invoke-static {v3}, $EXTENSION_CLASS_DESCRIPTOR->getColdHashData(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v3
                    """)

                // Tìm chính xác lệnh gọi Android Base64.encodeToString thông qua ReferenceInstruction
                val encodeToStringIndex = indexOfFirstInstructionOrThrow(match.stringMatches!![0].index) {
                    if (opcode != Opcode.INVOKE_STATIC && opcode != Opcode.INVOKE_STATIC_RANGE) return@indexOfFirstInstructionOrThrow false
                    val methodRef = (this as? ReferenceInstruction)?.reference as? MethodReference
                    methodRef?.definingClass == "Landroid/util/Base64;" && methodRef.name == "encodeToString"
                }

                val coldConfigGroupResultIndex = indexOfFirstInstructionOrThrow(encodeToStringIndex, Opcode.MOVE_RESULT_OBJECT)
                addInstructions(coldConfigGroupResultIndex + 1,
                    """
                        invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->getColdConfigGroup(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p1
                    """)

                /* This looks like not necessary.
                val coldStoredTimestampResultIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT_WIDE)
                addInstructions(coldStoredTimestampResultIndex + 1,
                    """
                        invoke-static {v6, v7}, $EXTENSION_CLASS_DESCRIPTOR->getColdStoredTimestamp(J)J
                        move-result-wide v6
                    """)
                 */
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
