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

  def joinGame(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = PodManager.getGame(gameId)
    game match {
      case Some(g) =>
        g.addUser(request.user)
        Ok(Json.obj(
          "status" -> "success"
        ))
      case None =>
        NotFound(Json.obj(
          "status" -> "failure",
          "errorMessage" -> "No Game found"
        ))
    }
  }
}