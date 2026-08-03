package app.revanced.patches.youtube.general.startup

import app.revanced.util.fingerprint.legacyFingerprint

internal val mainActivityOnCreateFingerprint = legacyFingerprint(
    name = "mainActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        method.name == "onCreate" && classDef.type.endsWith("/MainActivity;")
    }
)

internal val networkInfoFingerprint = legacyFingerprint(
    name = "networkInfoFingerprint",
    returnType = "Landroid/net/NetworkInfo;",
    parameters = emptyList(),
    strings = listOf("connectivity")
)