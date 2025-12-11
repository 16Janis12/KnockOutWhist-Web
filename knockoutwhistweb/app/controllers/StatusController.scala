package controllers

import auth.AuthAction
import logic.PodManager
import logic.game.GameLobby
import logic.user.SessionManager
import model.users.User
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import util.WebsocketEventMapper

import javax.inject.Inject

class StatusController @Inject()(
                                  val controllerComponents: ControllerComponents,
                                  val sessionManager: SessionManager,
                                  val authAction: AuthAction
                                ) extends BaseController {

  def requestStatus(): Action[AnyContent] = {
    Action { implicit request =>
      val userOpt = getUserFromSession(request)
      if (userOpt.isEmpty) {
        Ok(
          Json.obj(
            "status" -> "unauthenticated"
          )
        )
      } else {
        val user = userOpt.get
        val gameOpt = PodManager.identifyGameOfUser(user)
        if (gameOpt.isEmpty) {
          Ok(
            Json.obj(
              "status" -> "authenticated",
              "username" -> user.name,
              "inGame" -> false
            )
          )
        } else {
          val game = gameOpt.get
          Ok(
            Json.obj(
              "status" -> "authenticated",
              "username" -> user.name,
              "inGame" -> true,
              "gameId" -> game.id
            )
          )
        }
      }
    }
  }

  def game(gameId: String): Action[AnyContent] = {
    Action { implicit request =>
    val userOpt = getUserFromSession(request)
    if (userOpt.isEmpty) {
      Unauthorized("User not authenticated")
    } else {
      val user = userOpt.get
      val gameOpt = PodManager.getGame(gameId)
      if (gameOpt.isEmpty) {
        NotFound("Game not found")
      } else {
        val game = gameOpt.get
        if (!game.getPlayers.contains(user.id)) {
          Forbidden("User not part of this game")
        } else {
          Ok(
            Json.obj(
              "gameId" -> game.id,
              "state" -> game.logic.getCurrentState.toString,
              "data" -> mapGameState(game, user) 
            )
          )
        }
      }
    }
  }}

  private def getUserFromSession(request: RequestHeader): Option[User] = {
    val session = request.cookies.get("accessToken")
    if (session.isDefined)
      return sessionManager.getUserBySession(session.get.value)
    None
  }
  
  private def mapGameState(gameLobby: GameLobby, user: User): JsValue = {
    val userSession = gameLobby.getUserSession(user.id)
    WebsocketEventMapper.stateToJson(userSession)
  }

}
