package com.dragonroot.app

import com.dragonroot.app.update.SignatureVerifier
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class SignatureVerifierTest {
    private val kp = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val pub = Base64.getEncoder().encodeToString(kp.public.encoded)
    private val payload = """{"version":1}""".toByteArray()

    private fun sign(data: ByteArray): String {
        val s = Signature.getInstance("SHA256withECDSA"); s.initSign(kp.private); s.update(data)
        return Base64.getEncoder().encodeToString(s.sign())
    }

    @Test fun validSignaturePasses() = assertTrue(SignatureVerifier.verify(payload, sign(payload), pub))

    @Test fun tamperedPayloadFails() = assertFalse(SignatureVerifier.verify("""{"version":2}""".toByteArray(), sign(payload), pub))

    @Test fun blankKeyAlwaysFails() = assertFalse(SignatureVerifier.verify(payload, sign(payload), ""))

    @Test fun garbageSignatureFails() = assertFalse(SignatureVerifier.verify(payload, "not-base64!!", pub))

    @Test fun wrongKeyFails() {
        val other = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        assertFalse(SignatureVerifier.verify(payload, sign(payload), Base64.getEncoder().encodeToString(other.public.encoded)))
    }
}
