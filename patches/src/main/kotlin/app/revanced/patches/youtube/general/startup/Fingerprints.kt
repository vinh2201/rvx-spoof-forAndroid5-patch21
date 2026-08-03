package app.revanced.patches.youtube.general.startup

import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val networkCheckFingerprint = legacyFingerprint(
    name = "networkCheckFingerprint",
    returnType = "Z", // Hàm kiểm tra mạng chắc chắn phải trả về Boolean
    customFingerprint = { method, _ ->
        // Lọc qua các lệnh smali trong hàm
        method.implementation?.instructions?.any { instruction ->
            // Chỉ tìm các lệnh có tham chiếu đến phương thức khác
            if (instruction !is ReferenceInstruction) return@any false
            val ref = instruction.reference
            if (ref !is MethodReference) return@any false
            
            // Nếu đúng là gọi hàm isConnected() của NetworkInfo thì chốt đơn!
            ref.definingClass == "Landroid/net/NetworkInfo;" && ref.name == "isConnected"
        } ?: false
    }
)