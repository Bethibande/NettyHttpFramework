package com.bethibande.web.crypto

import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

object KeyHelper {

    /**
     * Used to load <b>unencrypted</b> rsa .pem private keys
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPrivateKeyFromString(key: String): RSAPrivateKey {
        var privateKeyPEM = key

        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "")
        privateKeyPEM = privateKeyPEM.replace(Regex("\r\n"), "")
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "")

        val encoded = Base64.decodeBase64(privateKeyPEM)
        val encodedKeySpec = PKCS8EncodedKeySpec(encoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(encodedKeySpec) as RSAPrivateKey
    }

}