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
                                    val authAction: AuthAction
                                  ) extends BaseController {

  // Pass the request-handling function directly to authAction (no nested Action)
  def mainMenu(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Ok(views.html.main("Knockout Whist - Create Game")(views.html.mainmenu.creategame(Some(request.user))))
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

      val gameLobby = PodManager.createGame(
        host = request.user,
        name = gamename,
        maxPlayers = playeramount.toInt
      )
      Ok(Json.obj(
        "status" -> "success",
        "gameId" -> gameLobby.id,
      ))
    } else {
      BadRequest(Json.obj(
        "status" -> "failure",
        "errorMessage" -> "Invalid form submission"
      ))
    }

  }

  def joinGame(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val jsonBody = request.body.asJson
    val gameId: Option[String] = jsonBody.flatMap { jsValue =>
      (jsValue \ "gameId").asOpt[String]
    }
    if (gameId.isDefined) {
      val game = PodManager.getGame(gameId.get)
      game match {
        case Some(g) =>
          g.addUser(request.user)
          Ok(Json.obj(
            "status" -> "success",
            "redirectUrl" -> routes.IngameController.game(g.id).url,
            "content" -> IngameController.returnInnerHTML(g, g.logic.getCurrentState, request.user).toString
          ))
        case None =>
          NotFound(Json.obj(
            "status" -> "failure",
            "errorMessage" -> "No Game found"
          ))
      }
    } else {
      BadRequest(Json.obj(
        "status" -> "failure",
        "errorMessage" -> "Invalid form submission"
      ))
    }
  }

  def rules(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Ok(views.html.main("Knockout Whist - Rules")(views.html.mainmenu.rules(Some(request.user))))
  }

  def navSPA(location: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    location match {
      case "0" => // Main Menu
        Ok(Json.obj(
          "status" -> "success",
          "redirectUrl" -> routes.MainMenuController.mainMenu().url,
          "content" -> views.html.mainmenu.creategame(Some(request.user)).toString
        ))
      case "1" => // Rules
        Ok(Json.obj(
          "status" -> "success",
          "redirectUrl" -> routes.MainMenuController.rules().url,
          "content" -> views.html.mainmenu.rules(Some(request.user)).toString
        ))
      case _ =>
        BadRequest(Json.obj(
          "status" -> "failure",
          "errorMessage" -> "Invalid form submission"
        ))
    }
  }

}