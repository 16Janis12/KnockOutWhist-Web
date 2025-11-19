package controllers

import auth.{AuthAction, AuthenticatedRequest}
import logic.user.{SessionManager, UserManager}
import play.api.*
import play.api.libs.json.Json
import play.api.mvc.*

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

  def login(): Action[AnyContent] = {
    Action { implicit request =>
      val session = request.cookies.get("sessionId")
      if (session.isDefined) {
        val possibleUser = sessionManager.getUserBySession(session.get.value)
        if (possibleUser.isDefined) {
          Redirect(routes.MainMenuController.mainMenu())
        } else {
          Ok(views.html.main("Login")(views.html.login.login()))
        }
      } else {
        Ok(views.html.main("Login")(views.html.login.login()))
      }
    }
  }

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
            "status" -> "success",
            "redirectUrl" -> routes.MainMenuController.mainMenu().url,
            "content" -> views.html.mainmenu.creategame(possibleUser).toString
          )).withCookies(
            Cookie("sessionId", sessionManager.createSession(possibleUser.get))
          )
        } else {
          Unauthorized("Invalid username or password")
        }
      } else {
        BadRequest("Invalid form submission")
      }
    }
  }

  // Pass the request-handling function directly to authAction (no nested Action)
  def logout(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val sessionCookie = request.cookies.get("sessionId")
    if (sessionCookie.isDefined) {
      sessionManager.invalidateSession(sessionCookie.get.value)
    }
    Redirect(routes.UserController.login()).discardingCookies(DiscardingCookie("sessionId"))
  }

}