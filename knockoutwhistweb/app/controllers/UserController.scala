package controllers

import auth.{AuthAction, AuthenticatedRequest}
import logic.user.{SessionManager, UserManager}
import play.api.*
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
          Ok(views.html.login.login())
        }
      } else {
        Ok(views.html.login.login())
      }
    }
  }

  def login_Post(): Action[AnyContent] = {
    Action { implicit request =>
      val postData = request.body.asFormUrlEncoded
      if (postData.isDefined) {
        // Extract username and password from form data
        val username = postData.get.get("username").flatMap(_.headOption).getOrElse("")
        val password = postData.get.get("password").flatMap(_.headOption).getOrElse("")
        val possibleUser = userManager.authenticate(username, password)
        if (possibleUser.isDefined) {
          Redirect(routes.MainMenuController.mainMenu()).withCookies(
            Cookie("sessionId", sessionManager.createSession(possibleUser.get))
          )
        } else {
          println("Failed login attempt for user: " + username)
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
    NoContent.discardingCookies(DiscardingCookie("sessionId"))
  }

}