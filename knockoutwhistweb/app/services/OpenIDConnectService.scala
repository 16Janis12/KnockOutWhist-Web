package services

import com.typesafe.config.Config
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import play.api.libs.json.*

import java.net.URI
import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.id.*
import com.nimbusds.openid.connect.sdk.*
import play.api.libs.ws.DefaultBodyWritables.*

case class OpenIDUserInfo(
  id: String,
  email: Option[String],
  name: Option[String],
  picture: Option[String],
  provider: String,
  providerName: String
)

object OpenIDUserInfo {
  implicit val writes: Writes[OpenIDUserInfo] = Json.writes[OpenIDUserInfo]
  implicit val reads: Reads[OpenIDUserInfo] = Json.reads[OpenIDUserInfo]
}

case class OpenIDProvider(
  name: String,
  clientId: String,
  clientSecret: String,
  redirectUri: String,
  authorizationEndpoint: String,
  tokenEndpoint: String,
  userInfoEndpoint: String,
  scopes: Set[String] = Set("openid", "profile", "email"),
  idClaimName: String = "id"
)

case class TokenResponse(
  accessToken: String,
  tokenType: String,
  expiresIn: Option[Int],
  refreshToken: Option[String],
  idToken: Option[String]
)

@Singleton
class OpenIDConnectService@Inject(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  private val providers = Map(
    "discord" -> OpenIDProvider(
      name = "Discord",
      clientId = config.get[String]("openid.discord.clientId"),
      clientSecret = config.get[String]("openid.discord.clientSecret"),
      redirectUri = config.get[String]("openid.discord.redirectUri"),
      authorizationEndpoint = "https://discord.com/oauth2/authorize",
      tokenEndpoint = "https://discord.com/api/oauth2/token",
      userInfoEndpoint = "https://discord.com/api/users/@me",
      scopes = Set("identify", "email")
    ),
    "keycloak" -> OpenIDProvider(
      name = "Identity",
      clientId = config.get[String]("openid.keycloak.clientId"),
      clientSecret = config.get[String]("openid.keycloak.clientSecret"),
      redirectUri = config.get[String]("openid.keycloak.redirectUri"),
      authorizationEndpoint = config.get[String]("openid.keycloak.authUrl") + "/protocol/openid-connect/auth",
      tokenEndpoint = config.get[String]("openid.keycloak.authUrl") + "/protocol/openid-connect/token",
      userInfoEndpoint = config.get[String]("openid.keycloak.authUrl") + "/protocol/openid-connect/userinfo",
      scopes = Set("openid", "profile", "email"),
      idClaimName = "sub"
    )
  )

  def getAuthorizationUrl(providerName: String, state: String, nonce: String): Option[String] = {
    providers.get(providerName).map { provider =>
      val authRequest = new AuthorizationRequest.Builder(
        new ResponseType(ResponseType.Value.CODE),
        new com.nimbusds.oauth2.sdk.id.ClientID(provider.clientId)
      )
        .scope(new com.nimbusds.oauth2.sdk.Scope(provider.scopes.mkString(" ")))
        .state(new com.nimbusds.oauth2.sdk.id.State(state))
        .redirectionURI(URI.create(provider.redirectUri))
        .endpointURI(URI.create(provider.authorizationEndpoint))
        .build()

      authRequest.toURI.toString
    }
  }

  def exchangeCodeForTokens(providerName: String, code: String, state: String): Future[Option[TokenResponse]] = {
    providers.get(providerName) match {
      case Some(provider) =>
        ws.url(provider.tokenEndpoint)
          .withHttpHeaders(
            "Accept" -> "application/json",
            "Content-Type" -> "application/x-www-form-urlencoded"
          )
          .post(
            Map(
              "client_id" -> Seq(provider.clientId),
              "client_secret" -> Seq(provider.clientSecret),
              "code" -> Seq(code),
              "grant_type" -> Seq("authorization_code"),
              "redirect_uri" -> Seq(provider.redirectUri)
            )
          )
          .map { response =>
            if (response.status == 200) {
              val json = response.json
              Some(TokenResponse(
                accessToken = (json \ "access_token").as[String],
                tokenType = (json \ "token_type").as[String],
                expiresIn = (json \ "expires_in").asOpt[Int],
                refreshToken = (json \ "refresh_token").asOpt[String],
                idToken = (json \ "id_token").asOpt[String]
              ))
            } else {
              None
            }
          }
          .recover { case _ => None }
      case None => Future.successful(None)
    }
  }

  def getUserInfo(providerName: String, accessToken: String): Future[Option[OpenIDUserInfo]] = {
    providers.get(providerName) match {
      case Some(provider) =>
        ws.url(provider.userInfoEndpoint)
          .withHttpHeaders("Authorization" -> s"Bearer $accessToken")
          .get()
          .map { response =>
            if (response.status == 200) {
              val json = response.json
              Some(OpenIDUserInfo(
                id = (json \ provider.idClaimName).as[String],
                email = (json \ "email").asOpt[String],
                name = (json \ "name").asOpt[String].orElse((json \ "login").asOpt[String]),
                picture = (json \ "picture").asOpt[String].orElse((json \ "avatar_url").asOpt[String]),
                provider = providerName,
                providerName = provider.name
              ))
            } else {
              logger.error(s"Failed to retrieve user info from ${provider.userInfoEndpoint}, status code ${response.status}")
              None
            }
          }
          .recover { case e => {
            logger.error(s"Failed to retrieve user info from ${provider.userInfoEndpoint}", e)
            None
          }
          }
      case None => Future.successful(None)
    }
  }

  def validateState(sessionState: String, returnedState: String): Boolean = {
    sessionState == returnedState
  }

  def generateState(): String = {
    java.util.UUID.randomUUID().toString.replace("-", "")
  }

  def generateNonce(): String = {
    java.util.UUID.randomUUID().toString.replace("-", "")
  }
}