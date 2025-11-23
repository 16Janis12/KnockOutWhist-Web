package services

import play.api.Configuration

import java.nio.file.{Files, Paths}
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, RSAPublicKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPair, PrivateKey, PublicKey}
import java.util.Base64
import javax.inject.*

@Singleton
class JwtKeyProvider @Inject()(config: Configuration) {

  val publicKey: RSAPublicKey = {
    val pemOpt = config.getOptional[String]("auth.publicKeyPem")
    val fileOpt = config.getOptional[String]("auth.publicKeyFile")

    pemOpt.orElse(fileOpt.map { path =>
      new String(Files.readAllBytes(Paths.get(path)))
    }) match {
      case Some(pem) => loadPublicKeyFromPem(pem)
      case None => throw new RuntimeException("No RSA public key configured.")
    }
  }
  val privateKey: RSAPrivateKey = {
    val pemOpt = config.getOptional[String]("auth.privateKeyPem")
    val fileOpt = config.getOptional[String]("auth.privateKeyFile")

    pemOpt.orElse(fileOpt.map { path =>
      new String(Files.readAllBytes(Paths.get(path)))
    }) match {
      case Some(pem) => loadPrivateKeyFromPem(pem)
      case None => throw new RuntimeException("No RSA private key configured.")
    }
  }

  private def loadPublicKeyFromPem(pem: String): RSAPublicKey = {
    val decoded = Base64.getDecoder.decode(cleanPem(pem))
    val spec = new X509EncodedKeySpec(decoded)
    KeyFactory.getInstance("RSA").generatePublic(spec).asInstanceOf[RSAPublicKey]
  }

  private def loadPrivateKeyFromPem(pem: String): RSAPrivateKey = {
    val decoded = Base64.getDecoder.decode(cleanPem(pem))
    val spec = new PKCS8EncodedKeySpec(decoded)
    KeyFactory.getInstance("RSA").generatePrivate(spec).asInstanceOf[RSAPrivateKey]
  }

  private def cleanPem(pem: String): String =
    pem.replaceAll("-----BEGIN (.*)-----", "")
      .replaceAll("-----END (.*)-----", "")
      .replaceAll("\\s", "")

}
