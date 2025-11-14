package controllers

import auth.{AuthAction, AuthenticatedRequest}
import de.knockoutwhist.cards.Hand
import logic.PodManager
import logic.game.{GameLobby, PollingEvents}
import logic.game.PollingEvents.{CardPlayed, LobbyUpdate, NewRound, ReloadEvent}
import model.sessions.UserSession
import model.users.User
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Result}
import util.WebUIUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PollingController @Inject() (
                                    val cc: ControllerComponents,
                                    val podManager: PodManager,
                                    val authAction: AuthAction,
                                    implicit val ec: ExecutionContext
                                  ) extends AbstractController(cc) {

  private def buildCardPlayResponse(game: GameLobby, hand: Option[Hand], newRound: Boolean): JsValue = {
    val currentRound = game.logic.getCurrentRound.get
    val currentTrick = game.logic.getCurrentTrick.get

    val trickCardsJson = Json.toJson(
      currentTrick.cards.map { case (card, player) =>
        Json.obj("cardId" -> WebUIUtils.cardtoString(card), "player" -> player.name)
      }
    )
    val scoreTableJson = Json.toJson(
      game.getLogic.getPlayerQueue.get.toList.map { player =>
        Json.obj(
          "name" -> player.name,
          "tricks" -> currentRound.tricklist.count(_.winner.contains(player))
        )
      }
    )

    val stringHand = hand.map { h =>
      val cardStrings = h.cards.map(WebUIUtils.cardtoString)
      Json.toJson(cardStrings).as[JsArray]
    }.getOrElse(Json.arr())

    val firstCardId = currentTrick.firstCard.map(WebUIUtils.cardtoString).getOrElse("BLANK")
    val nextPlayer = game.getLogic.getPlayerQueue.get.duplicate().nextPlayer().name
    Json.obj(
      "status" -> "cardPlayed",
      "animation" -> newRound,
      "handData" -> stringHand,
      "currentPlayerName" -> game.logic.getCurrentPlayer.get.name,
      "trumpSuit" -> currentRound.trumpSuit.toString,
      "trickCards" -> trickCardsJson,
      "scoreTable" -> scoreTableJson,
      "firstCardId" -> firstCardId,
      "nextPlayer" -> nextPlayer
    )
  }

  private def buildLobbyUsersResponse(game: GameLobby, userSession: UserSession): JsValue = {
    Json.obj(
      "status" -> "lobbyUpdate",
      "host" ->  userSession.host,
      "users" -> game.getUsers.map(u => Json.obj(
        "name" -> u.name,
        "id" -> u.id,
        "self" -> (u.id == userSession.id)
      ))
    )
  }


  def handleEvent(event: PollingEvents, game: GameLobby, userSession: UserSession): Result = {
    event match {
      case NewRound =>
        val player = game.getPlayerByUser(userSession.user)
        val hand = player.currentHand()
        val jsonResponse = buildCardPlayResponse(game, hand, true)
        Ok(jsonResponse)
      case CardPlayed =>
        val player = game.getPlayerByUser(userSession.user)
        val hand = player.currentHand()
        val jsonResponse = buildCardPlayResponse(game, hand, false)
        Ok(jsonResponse)
      case LobbyUpdate =>
        Ok(buildLobbyUsersResponse(game, userSession))
      case ReloadEvent =>
        val jsonResponse = Json.obj(
          "status" -> "reloadEvent",
          "redirectUrl" -> routes.IngameController.game(game.id).url
        )
        Ok(jsonResponse)
    }
  }

  // --- Main Polling Action ---
  def polling(gameId: String): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>

    val playerId = request.user.id

    // 1. Safely look up the game
    podManager.getGame(gameId) match {
      case Some(game) =>

        // 2. Short-Poll Check (Check for missed events)
        if (game.getPollingState.nonEmpty) {
          val event = game.getPollingState.dequeue()

          Future.successful(handleEvent(event, game, game.getUserSession(playerId)))
        } else {

          val eventPromise = game.registerWaiter(playerId)

          eventPromise.future.map { event =>
            game.removeWaiter(playerId)
            handleEvent(event, game, game.getUserSession(playerId))
          }.recover {
            case _: Throwable =>
              game.removeWaiter(playerId)
              NoContent
          }
        }

      case None =>
        // Game not found
        Future.successful(NotFound("Game not found."))
    }
  }


}
