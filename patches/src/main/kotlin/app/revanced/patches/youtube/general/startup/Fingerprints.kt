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