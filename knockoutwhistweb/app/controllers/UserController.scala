package controllers

import com.google.inject.{Guice, Injector}
import de.knockoutwhist.KnockOutWhist
import de.knockoutwhist.components.Configuration
import de.knockoutwhist.control.GameState.{InGame, Lobby, SelectTrump, TieBreak}
import de.knockoutwhist.control.controllerBaseImpl.BaseGameLogic
import di.KnockOutWebConfigurationModule
import logic.PodManager
import logic.user.{SessionManager, UserManager}
import model.sessions.SimpleSession
import play.api.*
import play.api.mvc.*
import play.twirl.api.Html

import java.util.UUID
import javax.inject.*


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents, val sessionManager: SessionManager, val userManager: UserManager) extends BaseController {

  def mainMenu() : Action[AnyContent] = {
    Action { implicit request =>
      val session = request.cookies.get("sessionId")
      if (session.isDefined) {
        val possibleUser = sessionManager.getUserBySession(session.get.value)
        if (possibleUser.isDefined) {
          Ok("Main Menu for user: " + possibleUser.get.name)
        } else
        {
          println("Invalid session, redirecting to login")
          Redirect("/login")
        }
      } else {
        Redirect("/login")
      }
    }
  }

  def login(): Action[AnyContent] = {
    Action { implicit request =>
      val session = request.cookies.get("sessionId")
      if (session.isDefined) {
        val possibleUser = sessionManager.getUserBySession(session.get.value)
        if (possibleUser.isDefined) {
          Redirect("/mainmenu")
        } else
        {
          Ok(views.html.login())
        }
      } else {
        Ok(views.html.login())
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
          Redirect("/mainmenu").withCookies(
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

  def logout(): Action[AnyContent] = {
    Action { implicit request =>
      val sessionCookie = request.cookies.get("sessionId")
      if (sessionCookie.isDefined) {
        sessionManager.invalidateSession(sessionCookie.get.value)
      }
      NoContent.discardingCookies(DiscardingCookie("sessionId"))
    }
  }

}