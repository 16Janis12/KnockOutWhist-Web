package services

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.inject.*
import play.api.Configuration

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

@Singleton
class JwtKeyProvider @Inject()(config: Configuration) {

  private def cleanPem(pem: String): String =
    pem.replaceAll("-----BEGIN (.*)-----", "")
      .replaceAll("-----END (.*)-----", "")
      .replaceAll("\\s", "")

  private def loadPublicKeyFromPem(pem: String): RSAPublicKey = {
    val decoded = Base64.getDecoder.decode(cleanPem(pem))
    val spec = new X509EncodedKeySpec(decoded)
    KeyFactory.getInstance("RSA").generatePublic(spec).asInstanceOf[RSAPublicKey]
  }

  private def loadPrivateKeyFromPem(pem: String): RSAPrivateKey = {
    val decoded = Base64.getDecoder.decode(cleanPem(pem))
    val spec = new X509EncodedKeySpec(decoded)
    KeyFactory.getInstance("RSA").generatePrivate(spec).asInstanceOf[RSAPrivateKey]
  }

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
  
}
