package controllers

import auth.{AuthAction, AuthenticatedRequest}
import logic.PodManager
import play.api.*
import play.api.mvc.*

import javax.inject.*


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MainMenuController @Inject()(
                                    val controllerComponents: ControllerComponents,
                                    val authAction: AuthAction,
                                    val podManager: PodManager
                                  ) extends BaseController {

  // Pass the request-handling function directly to authAction (no nested Action)
  def mainMenu(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Ok("Main Menu for user: " + request.user.name)
  }

  def index(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Redirect(routes.MainMenuController.mainMenu())
  }

  def createGame(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val gameLobby = podManager.createGame(
      host = request.user,
      name = s"${request.user.name}'s Game",
      maxPlayers = 4
    )
    Redirect(routes.IngameController.game(gameLobby.id))
  }

  def rules(): Action[AnyContent] = {
    Action { implicit request =>
      Ok(views.html.mainmenu.rules())
    }
  }
}