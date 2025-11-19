package controllers

import auth.{AuthAction, AuthenticatedRequest}
import controllers.PollingController.{scheduler, timeoutDuration}
import de.knockoutwhist.cards.Hand
import de.knockoutwhist.player.AbstractPlayer
import logic.PodManager
import logic.game.{GameLobby, PollingEvents}
import logic.game.PollingEvents.{CardPlayed, LobbyCreation, LobbyUpdate, NewRound, NewTrick, ReloadEvent}
import model.sessions.UserSession
import model.users.User
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Result}
import util.WebUIUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.concurrent.duration.*
object PollingController {
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private val timeoutDuration = 25.seconds
}
@Singleton
class PollingController @Inject() (
                                    val cc: ControllerComponents,
                                    val podManager: PodManager,
                                    val authAction: AuthAction,
                                    val ingameController: IngameController,
                                    implicit val ec: ExecutionContext
                                  ) extends AbstractController(cc) {

  private def buildCardPlayResponse(game: GameLobby, hand: Option[Hand], player: AbstractPlayer, newRound: Boolean): JsValue = {
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
      "dog" -> player.isInDogLife,
      "currentPlayerName" -> game.logic.getCurrentPlayer.get.name,
      "trumpSuit" -> currentRound.trumpSuit.toString,
      "trickCards" -> trickCardsJson,
      "scoreTable" -> scoreTableJson,
      "firstCardId" -> firstCardId,
      "nextPlayer" -> nextPlayer,
      "yourTurn" -> (game.logic.getCurrentPlayer.get == player)
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
        val jsonResponse = buildCardPlayResponse(game, hand, player, true)
        Ok(jsonResponse)
      case NewTrick =>
        val player = game.getPlayerByUser(userSession.user)
        val hand = player.currentHand()
        val jsonResponse = buildCardPlayResponse(game, hand, player, false)
        Ok(jsonResponse)
      case CardPlayed =>
        val player = game.getPlayerByUser(userSession.user)
        val hand = player.currentHand()
        val jsonResponse = buildCardPlayResponse(game, hand, player, false)
        Ok(jsonResponse)
      case LobbyUpdate =>
        Ok(buildLobbyUsersResponse(game, userSession))
      case ReloadEvent =>
        val jsonResponse = Json.obj(
          "status" -> "reloadEvent",
          "redirectUrl" -> routes.IngameController.game(game.id).url,
          "content" -> ingameController.returnInnerHTML(game, userSession.user).toString
        )
        Ok(jsonResponse)
    }
  }

  def polling(gameId: String): Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[AnyContent] =>

    val playerId = request.user.id

    podManager.getGame(gameId) match {
      case Some(game) =>
        val playerEventQueue = game.getEventsOfPlayer(playerId)
        if (playerEventQueue.nonEmpty) {
          val event = playerEventQueue.dequeue()
          Future.successful(handleEvent(event, game, game.getUserSession(playerId)))
        } else {
          val eventPromise = game.registerWaiter(playerId)
          val scheduledFuture = scheduler.schedule(
            new Runnable {
              override def run(): Unit =
                eventPromise.tryFailure(new java.util.concurrent.TimeoutException("Polling Timeout"))
            },
            timeoutDuration.toMillis,
            TimeUnit.MILLISECONDS
          )
          eventPromise.future.map { event =>
            scheduledFuture.cancel(false)
            game.removeWaiter(playerId)
            handleEvent(event, game, game.getUserSession(playerId))
          }.recover {
            case _: Throwable =>
              game.removeWaiter(playerId)
              NoContent
          }
        }

      case None =>
        Future.successful(NotFound("Game not found."))
    }
  }


}
