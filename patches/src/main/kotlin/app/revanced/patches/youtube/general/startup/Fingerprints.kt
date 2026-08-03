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
    customFingerprint = { method, classDef ->
        // Ưu tiên class Chromium cố định HOẶC bất kỳ hàm smali nào có gọi getActiveNetworkInfo
        val isChromiumClass = classDef.type == "Lorg/chromium/net/NetworkChangeNotifierAutoDetect;" ||
                              classDef.type == "Lorg/chromium/net/AndroidNetworkLibrary;"

        val containsGetActiveNetworkInfo = method.implementation?.instructions?.any { instruction ->
            instruction.toString().contains("ConnectivityManager;->getActiveNetworkInfo")
        } == true

        isChromiumClass || containsGetActiveNetworkInfo
    }
)