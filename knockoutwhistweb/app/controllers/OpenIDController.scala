package controllers

import logic.user.{SessionManager, UserManager}
import model.users.User
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Cookie.SameSite.Lax
import services.{OpenIDConnectService, OpenIDUserInfo}

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OpenIDController @Inject()(
                                  val controllerComponents: ControllerComponents,
                                  val openIDService: OpenIDConnectService,
                                  val sessionManager: SessionManager,
                                  val userManager: UserManager,
                                  val config: Configuration
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
          openIDService.exchangeCodeForTokens(provider, authCode, sessionState.get).flatMap {
            case Some(tokenResponse) =>
              openIDService.getUserInfo(provider, tokenResponse.accessToken).flatMap {
                case Some(userInfo) =>
                  // Check if user already exists
                  userManager.authenticateOpenID(provider, userInfo.id) match {
                    case Some(user) =>
                      logger.info(s"User ${userInfo.name} (${userInfo.id}) already exists, logging them in")
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
                      logger.info(s"User ${userInfo.name} (${userInfo.id}) not found, creating new user")
                      // New user, redirect to username selection
                      Future.successful(Redirect(config.get[String]("openid.selectUserRoute"))
                        .withSession(
                          "oauth_user_info" -> Json.toJson(userInfo).toString(),
                          "oauth_provider" -> provider,
                          "oauth_access_token" -> tokenResponse.accessToken
                        ))
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
    request.session.get("oauth_user_info") match {
      case Some(userInfoJson) =>
        val userInfo = Json.parse(userInfoJson).as[OpenIDUserInfo]
        Future.successful(Ok(Json.obj(
          "id" -> userInfo.id,
          "email" -> userInfo.email,
          "name" -> userInfo.name,
          "picture" -> userInfo.picture,
          "provider" -> userInfo.provider,
          "providerName" -> userInfo.providerName
        )))
      case None =>
        Future.successful(Redirect("/login").flashing("error" -> "No authentication information found"))
    }
  }

  def submitUsername(): Action[AnyContent] = Action.async { implicit request =>
    val username = request.body.asJson.flatMap(json => (json \ "username").asOpt[String])
      .orElse(request.body.asFormUrlEncoded.flatMap(_.get("username").flatMap(_.headOption)))
    val userInfoJson = request.session.get("oauth_user_info")
    val provider = request.session.get("oauth_provider").getOrElse("unknown")
    
    (username, userInfoJson) match {
      case (Some(uname), Some(userInfoJson)) =>
        val userInfo = Json.parse(userInfoJson).as[OpenIDUserInfo]
        
        // Check if username already exists
        val trimmedUsername = uname.trim
        userManager.userExists(trimmedUsername) match {
          case Some(_) =>
            Future.successful(Conflict(Json.obj("error" -> "Username already taken")))
          case None =>
            // Create new user with OpenID info (no password needed)
            val success = userManager.addOpenIDUser(trimmedUsername, userInfo)
            if (success) {
              // Get the created user and create session
              userManager.userExists(trimmedUsername) match {
                case Some(user) =>
                  val sessionToken = sessionManager.createSession(user)
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
                  )).removingFromSession("oauth_user_info", "oauth_provider", "oauth_access_token"))
                case None =>
                  Future.successful(InternalServerError(Json.obj("error" -> "Failed to create user session")))
              }
            } else {
              Future.successful(InternalServerError(Json.obj("error" -> "Failed to create user")))
            }
        }
      case _ =>
        Future.successful(BadRequest(Json.obj("error" -> "Username is required")))
    }
  }
}
