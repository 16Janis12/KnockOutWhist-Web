package controllers

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import logic.user.{SessionManager, UserManager}
import model.users.User
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Cookie.SameSite.Lax
import services.{OpenIDConnectService, OpenIDUserInfo, OAuthCacheService}

import java.util.concurrent.TimeUnit
import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OpenIDController @Inject()(
                                  val controllerComponents: ControllerComponents,
                                  val openIDService: OpenIDConnectService,
                                  val sessionManager: SessionManager,
                                  val userManager: UserManager,
                                  val config: Configuration,
                                  val oauthCache: OAuthCacheService
                                )(implicit ec: ExecutionContext) extends BaseController {

  private val logger = Logger(this.getClass)
  
  def loginWithProvider(provider: String): Action[AnyContent] = Action.async { implicit request =>
    val state = openIDService.generateState()
    val nonce = openIDService.generateNonce()
    
    // Store state and nonce in session
    openIDService.getAuthorizationUrl(provider, state, nonce) match {
      case Some(authUrl) =>
        Future.successful(Redirect(authUrl)
          .withSession(
            "oauth_state" -> state,
            "oauth_nonce" -> nonce,
            "oauth_provider" -> provider
          ))
      case None =>
        Future.successful(BadRequest(Json.obj("error" -> "Unsupported provider")))
    }
  }

  def callback(provider: String): Action[AnyContent] = Action.async { implicit request =>
    val sessionState = request.session.get("oauth_state")
    val sessionNonce = request.session.get("oauth_nonce")
    val sessionProvider = request.session.get("oauth_provider")
    
    val returnedState = request.getQueryString("state")
    val code = request.getQueryString("code")
    val error = request.getQueryString("error")

    logger.warn(s"Received callback from $provider with state $sessionState, nonce $sessionNonce, provider $sessionProvider, returned state $returnedState, code $code, error $error")

    error match {
      case Some(err) =>
        logger.error(s"Authentication failed: $err")
        Future.successful(Redirect("/login").flashing("error" -> s"Authentication failed: $err"))
      case None =>
        (for {
          _ <- Option(sessionState.contains(returnedState.getOrElse("")))
          _ <- Option(sessionProvider.contains(provider))
          authCode <- code
        } yield {
          logger.warn(s"Authentication successful for $provider")
          openIDService.exchangeCodeForTokens(provider, authCode, sessionState.get).flatMap {
            case Some(tokenResponse) =>
              openIDService.getUserInfo(provider, tokenResponse.accessToken).flatMap {
                case Some(userInfo) =>
                  // Check if user already exists
                  userManager.authenticateOpenID(provider, userInfo.id) match {
                    case Some(user) =>
                      logger.warn(s"User ${userInfo.name} (${userInfo.id}) already exists, logging them in")
                      // User already exists, log them in
                      val sessionToken = sessionManager.createSession(user)
                      Future.successful(Redirect(config.getOptional[String]("openid.mainRoute").getOrElse("/"))
                        .withCookies(Cookie(
                          name = "accessToken",
                          value = sessionToken,
                          httpOnly = true,
                          secure = false,
                          sameSite = Some(Lax)
                        ))
                        .removingFromSession("oauth_state", "oauth_nonce", "oauth_provider", "oauth_access_token"))
                    case None =>
                      logger.warn(s"User ${userInfo.name} (${userInfo.id}) not found, creating new user")
                      // Store OAuth data in cache and get session ID
                      val oauthSessionId = oauthCache.storeOAuthData(userInfo, tokenResponse.accessToken, provider)
                      // New user, redirect to username selection with only session ID
                      Future.successful(Redirect(config.get[String]("openid.selectUserRoute"))
                        .withSession("oauth_session_id" -> oauthSessionId))
                  }
                case None =>
                  logger.error("Failed to retrieve user information")
                  Future.successful(Redirect("/login").flashing("error" -> "Failed to retrieve user information"))
              }
            case None =>
              logger.error("Failed to exchange authorization code")
              Future.successful(Redirect("/login").flashing("error" -> "Failed to exchange authorization code"))
          }
        }).getOrElse {
          logger.error("Invalid state parameter")
          Future.successful(Redirect("/login").flashing("error" -> "Invalid state parameter"))
        }
    }
  }

  def selectUsername(): Action[AnyContent] = Action.async { implicit request =>
    request.session.get("oauth_session_id") match {
      case Some(sessionId) =>
        oauthCache.getOAuthData(sessionId) match {
          case Some((userInfo, _, _)) =>
            Future.successful(Ok(Json.obj(
              "id" -> userInfo.id,
              "email" -> userInfo.email,
              "name" -> userInfo.name,
              "picture" -> userInfo.picture,
              "provider" -> userInfo.provider,
              "providerName" -> userInfo.providerName
            )))
          case None =>
            logger.error(s"OAuth session data not found for session ID: $sessionId")
            Future.successful(Redirect("/login").flashing("error" -> "Session expired"))
        }
      case None =>
        logger.error("No OAuth session ID found")
        Future.successful(Redirect("/login").flashing("error" -> "No authentication information found"))
    }
  }

  def submitUsername(): Action[AnyContent] = Action.async { implicit request =>
    val username = request.body.asJson.flatMap(json => (json \ "username").asOpt[String])
      .orElse(request.body.asFormUrlEncoded.flatMap(_.get("username").flatMap(_.headOption)))
    val sessionId = request.session.get("oauth_session_id")
    
    (username, sessionId) match {
      case (Some(uname), Some(sid)) =>
        oauthCache.getOAuthData(sid) match {
          case Some((userInfo, accessToken, provider)) =>
            // Check if username already exists
            val trimmedUsername = uname.trim
            userManager.userExists(trimmedUsername) match {
              case Some(_) =>
                Future.successful(Conflict(Json.obj("error" -> "Username already taken")))
              case None =>
                // Create new user with OpenID info (no password needed)
                val success = userManager.addOpenIDUser(trimmedUsername, userInfo)
                if (success) {
                  // Get created user and create session
                  userManager.userExists(trimmedUsername) match {
                    case Some(user) =>
                      val sessionToken = sessionManager.createSession(user)
                      // Clean up cache after successful user creation
                      oauthCache.removeOAuthData(sid)
                      Future.successful(Ok(Json.obj(
                        "message" -> "User created successfully",
                        "user" -> Json.obj(
                          "id" -> user.id,
                          "username" -> user.name
                        )
                      )).withCookies(Cookie(
                        name = "accessToken",
                        value = sessionToken,
                        httpOnly = true,
                        secure = false,
                        sameSite = Some(Lax)
                      )).removingFromSession("oauth_session_id"))
                    case None =>
                      Future.successful(InternalServerError(Json.obj("error" -> "Failed to create user session")))
                  }
                } else {
                  Future.successful(InternalServerError(Json.obj("error" -> "Failed to create user")))
                }
            }
          case None =>
            logger.error(s"OAuth session data not found for session ID: $sid")
            Future.successful(Redirect("/login").flashing("error" -> "Session expired"))
        }
      case _ =>
        Future.successful(BadRequest(Json.obj("error" -> "Username and valid session required")))
    }
  }
}
