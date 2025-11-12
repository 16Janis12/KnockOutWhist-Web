package controllers

import auth.{AuthAction, AuthenticatedRequest}
import logic.PodManager
import play.api.*
import play.api.libs.json.Json
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
    Ok(views.html.mainmenu.creategame(Some(request.user)))
  }

  def index(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Redirect(routes.MainMenuController.mainMenu())
  }

  def createGame(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val jsonBody = request.body.asJson
    if (jsonBody.isDefined) {
      val gamename: String = (jsonBody.get \ "lobbyname").asOpt[String]
        .getOrElse(s"${request.user.name}'s Game")

      val playeramount: String = (jsonBody.get \ "playeramount").asOpt[String]
        .getOrElse(throw new IllegalArgumentException("Player amount is required."))

      val gameLobby = podManager.createGame(
        host = request.user,
        name = gamename,
        maxPlayers = playeramount.toInt
      )
      Ok(Json.obj(
        "status" -> "success",
        "redirectUrl" -> routes.IngameController.game(gameLobby.id).url
      ))
    } else {
      BadRequest(Json.obj(
        "status" -> "failure",
        "errorMessage" -> "Invalid form submission"
      ))
    }
    
  }
  
  def joinGame(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val postData = request.body.asFormUrlEncoded
    if (postData.isDefined) {
      val gameId = postData.get.get("gameId").flatMap(_.headOption).getOrElse("")
      val game = podManager.getGame(gameId)
      game match {
        case Some(g) =>
          Redirect(routes.IngameController.joinGame(gameId))
        case None =>
          NotFound("Game not found")
      }
    } else {
      BadRequest("Invalid form submission")
    }
  }

  def rules(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Ok(views.html.mainmenu.rules(Some(request.user)))
  }
}