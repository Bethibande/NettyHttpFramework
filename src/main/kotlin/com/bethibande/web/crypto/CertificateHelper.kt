package com.bethibande.web.crypto

import java.io.IOException
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import kotlin.io.path.inputStream

object CertificateHelper {

    /**
     * Used to load a X509 certificate from a .pem file
     */
    fun getCertificateFromString(path: Path): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        path.inputStream().use { stream ->
            return cf.generateCertificate(stream) as X509Certificate
        }
    }

    /**
     * Used to load <b>unencrypted</b> rsa .pem private keys
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPrivateKeyFromString(key: String): RSAPrivateKey {
        var privateKeyPEM = key

        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "")
        privateKeyPEM = privateKeyPEM.replace(Regex("\r\n"), "")
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "")

        val encoded = Base64.getDecoder().decode(privateKeyPEM)
        val encodedKeySpec = PKCS8EncodedKeySpec(encoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(encodedKeySpec) as RSAPrivateKey
    }

}