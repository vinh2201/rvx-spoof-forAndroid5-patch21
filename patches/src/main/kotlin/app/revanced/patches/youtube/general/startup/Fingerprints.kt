package app.revanced.patches.youtube.general.startup

import app.revanced.util.fingerprint.legacyFingerprint

internal val mainActivityOnCreateFingerprint = legacyFingerprint(
    name = "mainActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        method.name == "onCreate" && (
            classDef.type.contains("WatchWhileActivity") ||
            classDef.type.contains("MainActivity") ||
            classDef.type.contains("YouTubeLauncherActivity")
        )
    }
)

internal val networkInfoFingerprint = legacyFingerprint(
    name = "networkInfoFingerprint",
    customFingerprint = { method, _ ->
        // Chỉ đích danh các hàm có chứa lệnh lấy thông tin mạng (bỏ qua các hàm rác của Chromium)
        method.implementation?.instructions?.any { instruction ->
            instruction.toString().contains("ConnectivityManager;->getActiveNetworkInfo")
        } == true
    }
)