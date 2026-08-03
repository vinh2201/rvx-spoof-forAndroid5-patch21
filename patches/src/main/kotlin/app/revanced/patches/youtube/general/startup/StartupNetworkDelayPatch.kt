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
        // 1. Hook an toàn sau super.onCreate để kích hoạt bộ đếm 5 giây
        val onCreateMethod = mainActivityOnCreateFingerprint.methodOrThrow()
        val implementation = onCreateMethod.implementation
        
        if (implementation != null) {
            val instructions = implementation.instructions.toList()
            val superOnCreateIdx = instructions.indexOfFirst { it.toString().contains("->onCreate(") }
            val targetIndex = if (superOnCreateIdx != -1) superOnCreateIdx + 1 else 0
            
            (onCreateMethod as MutableMethod).addInstructions(
                targetIndex,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->startStartupTimer()V
                """
            )
        }

        // 2. CHỈ quét các class của riêng YouTube, loại bỏ hoàn toàn Play Services và thư viện ngoài để tránh VerifyError trên Android 5
        classes.filter { it.type.contains("youtube") }.forEach { classDef ->
            classDef.methods.forEach { method ->
                val methodImpl = method.implementation ?: return@forEach
                val instructions = methodImpl.instructions.toList()

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