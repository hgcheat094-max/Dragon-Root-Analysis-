package com.dragonroot.app.update

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** ECDSA P-256 / SHA-256 (DER signature) verification of signed database releases. */
object SignatureVerifier {
    fun verify(payload: ByteArray, signatureB64: String, publicKeyB64: String): Boolean {
        if (publicKeyB64.isBlank() || signatureB64.isBlank()) return false
        return try {
            val key = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64.trim())))
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(key)
            sig.update(payload)
            sig.verify(Base64.getDecoder().decode(signatureB64.trim()))
        } catch (e: Exception) {
            false
        }
    }
}
