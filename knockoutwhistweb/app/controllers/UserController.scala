package controllers

import auth.{AuthAction, AuthenticatedRequest}
import dto.subDTO.UserDTO
import logic.user.{SessionManager, UserManager}
import model.users.User
import play.api.*
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Cookie.SameSite.{Lax, None, Strict}

import javax.inject.*


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class UserController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val sessionManager: SessionManager,
                                val userManager: UserManager,
                                val authAction: AuthAction
                              ) extends BaseController {

  def login_Post(): Action[AnyContent] = {
    Action { implicit request =>
      val jsonBody = request.body.asJson
      val username: Option[String] = jsonBody.flatMap { jsValue =>
        (jsValue \ "username").asOpt[String]
      }
      val password: Option[String] = jsonBody.flatMap { jsValue =>
        (jsValue \ "password").asOpt[String]
      }
      if (username.isDefined && password.isDefined) {
        // Extract username and password from form data
        val possibleUser = userManager.authenticate(username.get, password.get)
        if (possibleUser.isDefined) {
          Ok(Json.obj(
            "user" -> Json.obj(
              "id" -> possibleUser.get.id,
              "username" -> possibleUser.get.name
            )
          )).withCookies(Cookie(
            name = "accessToken",
            value = sessionManager.createSession(possibleUser.get),
            httpOnly = true,
            secure = false,
            sameSite = Some(Lax)
          ))
        } else {
          Unauthorized("Invalid username or password")
        }
      } else {
        BadRequest("Invalid form submission")
      }
    }
  }
  
  def getUserInfo(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val user: User = request.user
    Ok(Json.obj(
      "id" -> user.id,
      "username" -> user.name
    ))
  }

  def register(): Action[AnyContent] = {
    Action { implicit request =>
      val jsonBody = request.body.asJson
      val username: Option[String] = jsonBody.flatMap { jsValue =>
        (jsValue \ "username").asOpt[String]
      }
      val password: Option[String] = jsonBody.flatMap { jsValue =>
        (jsValue \ "password").asOpt[String]
      }
      
      if (username.isDefined && password.isDefined) {
        // Validate input
        if (username.get.trim.isEmpty || password.get.length < 6) {
          BadRequest(Json.obj(
            "error" -> "Invalid input",
            "message" -> "Username must not be empty and password must be at least 6 characters"
          ))
        } else {
          // Try to register user
          val registrationSuccess = userManager.addUser(username.get.trim, password.get)
          if (registrationSuccess) {
            Created(Json.obj(
              "message" -> "User registered successfully",
              "username" -> username.get.trim
            ))
          } else {
            Conflict(Json.obj(
              "error" -> "User already exists",
              "message" -> "Username is already taken"
            ))
          }
        }
      } else {
        BadRequest(Json.obj(
          "error" -> "Invalid request",
          "message" -> "Username and password are required"
        ))
      }
    }
  }

  def logoutPost(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val sessionCookie = request.cookies.get("accessToken")
    if (sessionCookie.isDefined) {
      sessionManager.invalidateSession(sessionCookie.get.value)
    }
    NoContent.discardingCookies(DiscardingCookie("accessToken"))
  }

}