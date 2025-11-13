package controllers

import auth.{AuthAction, AuthenticatedRequest}
import de.knockoutwhist.control.GameState.{InGame, Lobby, SelectTrump, TieBreak}
import exceptions.{CantPlayCardException, GameFullException, NotEnoughPlayersException, NotHostException, NotInThisGameException}
import logic.PodManager
import model.sessions.{PlayerSession, UserSession}
import play.api.*
import play.api.libs.json.Json
import play.api.mvc.*

import java.util.UUID
import javax.inject.*
import scala.util.Try


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class IngameController @Inject()(
                                  val controllerComponents: ControllerComponents,
                                  val authAction: AuthAction,
                                  val podManager: PodManager
                                ) extends BaseController {

  def game(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    game match {
      case Some(g) =>
        g.logic.getCurrentState match {
          case Lobby => Ok(views.html.lobby.lobby(Some(request.user), g))
          case InGame =>
            Ok(views.html.ingame.ingame(
              g.getPlayerByUser(request.user),
              g
            ))
          case SelectTrump =>
            Ok(views.html.ingame.selecttrump(
              g.getPlayerByUser(request.user),
              g.logic,
              gameId
            ))
          case TieBreak =>
            Ok(views.html.ingame.tie(
              g.getPlayerByUser(request.user),
              g.logic,
              gameId
            ))
          case _ =>
            InternalServerError(s"Invalid game state for in-game view. GameId: $gameId" + s" State: ${g.logic.getCurrentState}")
        }
      case None =>
        NotFound("Game not found")
    }
    //NotFound(s"Reached end of game method unexpectedly. GameId: $gameId")
  }
  def startGame(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    val result = Try {
      game match {
        case Some(g) =>
          g.startGame(request.user)
        case None =>
          NotFound("Game not found")
      }
    }
    if (result.isSuccess) {
      Ok(Json.obj(
        "status" -> "success",
        "redirectUrl" -> routes.IngameController.game(gameId).url
      ))
    } else {
      val throwable = result.failed.get
      throwable match {
        case _: NotInThisGameException =>
          BadRequest(Json.obj(
            "status" -> "failure",
            "errorMessage" -> throwable.getMessage
          ))
        case _: NotHostException =>
          Forbidden(Json.obj(
            "status" -> "failure",
            "errorMessage" -> throwable.getMessage
          ))
        case _: NotEnoughPlayersException =>
          BadRequest(Json.obj(
            "status" -> "failure",
            "errorMessage" -> throwable.getMessage
          ))
        case _ =>
          InternalServerError(Json.obj(
            "status" -> "failure",
            "errorMessage" -> throwable.getMessage
          ))
      }
    }
  }
  def kickPlayer(gameId: String, playerToKick: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    val playerToKickUUID = UUID.fromString(playerToKick)
    val result = Try {
      game.get.leaveGame(playerToKickUUID)
    }
    if(result.isSuccess) {
      Ok(Json.obj(
        "status" -> "success",
        "redirectUrl" -> routes.IngameController.game(gameId).url
      ))
    } else {
      InternalServerError(Json.obj(
        "status" -> "failure",
        "errorMessage" -> "Something went wrong."
      ))
    }
  }
  def leaveGame(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    val result = Try {
      game.get.leaveGame(request.user.id)
    }
    if (result.isSuccess) {
      Ok(Json.obj(
        "status" -> "success",
        "redirectUrl" -> routes.MainMenuController.mainMenu().url
      ))
    } else {
      InternalServerError(Json.obj(
        "status" -> "failure",
        "errorMessage" -> "Something went wrong."
      ))
    }
  }
  def joinGame(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    val result = Try {
      game match {
        case Some(g) =>
          g.addUser(request.user)
        case None =>
          NotFound("Game not found")
      }
    }
    if (result.isSuccess) {
      Redirect(routes.IngameController.game(gameId))
    } else {
      val throwable = result.failed.get
      throwable match {
        case _: GameFullException =>
          BadRequest(throwable.getMessage)
        case _: IllegalArgumentException =>
          BadRequest(throwable.getMessage)
        case _: IllegalStateException =>
          BadRequest(throwable.getMessage)
        case _ =>
          InternalServerError(throwable.getMessage)
      }
    }
  }
  def playCard(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] => {
    val game = podManager.getGame(gameId)
    game match {
      case Some(g) =>
        val jsonBody = request.body.asJson
        val cardIdOpt: Option[String] = jsonBody.flatMap { jsValue =>
          (jsValue \ "cardID").asOpt[String]
        }
        cardIdOpt match {
          case Some(cardId) =>
            var optSession: Option[UserSession] = None
            val result = Try {
              val session = g.getUserSession(request.user.id)
              optSession = Some(session)
              session.lock.lock()
              g.playCard(session, cardId.toInt)
            }
            optSession.foreach(_.lock.unlock())
            if (result.isSuccess) {
              Ok(Json.obj(
                "status" -> "success",
                "redirectUrl" -> routes.IngameController.game(gameId).url
              ))
            } else {
              val throwable = result.failed.get
              throwable match {
                case _: CantPlayCardException =>
                  BadRequest(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
                case _: NotInThisGameException =>
                  BadRequest(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
                case _: IllegalArgumentException =>
                  BadRequest(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
                case _: IllegalStateException =>
                  BadRequest(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
                case _ =>
                  InternalServerError(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
              }
            }
          case None =>
            BadRequest(Json.obj(
              "status" -> "failure",
              "errorMessage" -> "cardId Parameter is missing"
            ))
        }
      case None =>
        NotFound(Json.obj(
          "status" -> "failure",
          "errorMessage" -> "Game not found"
        ))
    }
  }
  }
  def playDogCard(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] => {
    val game = podManager.getGame(gameId)
    game match {
      case Some(g) => {
        val cardIdOpt = request.body.asFormUrlEncoded.flatMap(_.get("cardId").flatMap(_.headOption))
        var optSession: Option[UserSession] = None
        val result = Try {
          cardIdOpt match {
            case Some(cardId) if cardId == "skip" =>
              val session = g.getUserSession(request.user.id)
              optSession = Some(session)
              session.lock.lock()
              g.playDogCard(session, -1)
            case Some(cardId) =>
              val session = g.getUserSession(request.user.id)
              optSession = Some(session)
              session.lock.lock()
              g.playDogCard(session, cardId.toInt)
            case None =>
              throw new IllegalArgumentException("cardId parameter is missing")
          }
        }
        optSession.foreach(_.lock.unlock())
        if (result.isSuccess) {
          NoContent
        } else {
          val throwable = result.failed.get
          throwable match {
            case _: CantPlayCardException =>
              BadRequest(throwable.getMessage)
            case _: NotInThisGameException =>
              BadRequest(throwable.getMessage)
            case _: IllegalArgumentException =>
              BadRequest(throwable.getMessage)
            case _: IllegalStateException =>
              BadRequest(throwable.getMessage)
            case _ =>
              InternalServerError(throwable.getMessage)
          }
        }
      }
      case None =>
        NotFound("Game not found")
    }
  }
  }
  def playTrump(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    game match {
      case Some(g) =>
        val trumpOpt = request.body.asFormUrlEncoded.flatMap(_.get("trump").flatMap(_.headOption))
        trumpOpt match {
          case Some(trump) =>
            var optSession: Option[UserSession] = None
            val result = Try {
              val session = g.getUserSession(request.user.id)
              optSession = Some(session)
              session.lock.lock()
              g.selectTrump(session, trump.toInt)
            }
            optSession.foreach(_.lock.unlock())
            if (result.isSuccess) {
              NoContent
            } else {
              val throwable = result.failed.get
              throwable match {
                case _: IllegalArgumentException =>
                  BadRequest(throwable.getMessage)
                case _: NotInThisGameException =>
                  BadRequest(throwable.getMessage)
                case _: IllegalStateException =>
                  BadRequest(throwable.getMessage)
                case _ =>
                  InternalServerError(throwable.getMessage)
              }
            }
          case None =>
            BadRequest("trump parameter is missing")
        }
      case None =>
        NotFound("Game not found")
    }
  }
  def playTie(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = podManager.getGame(gameId)
    game match {
      case Some(g) =>
        val tieOpt = request.body.asFormUrlEncoded.flatMap(_.get("tie").flatMap(_.headOption))
        tieOpt match {
          case Some(tie) =>
            var optSession: Option[UserSession] = None
            val result = Try {
              val session = g.getUserSession(request.user.id)
              optSession = Some(session)
              session.lock.lock()
              g.selectTie(g.getUserSession(request.user.id), tie.toInt - 1)
            }
            optSession.foreach(_.lock.unlock())
            if (result.isSuccess) {
              NoContent
            } else {
              val throwable = result.failed.get
              throwable match {
                case _: IllegalArgumentException =>
                  BadRequest(throwable.getMessage)
                case _: NotInThisGameException =>
                  BadRequest(throwable.getMessage)
                case _: IllegalStateException =>
                  BadRequest(throwable.getMessage)
                case _ =>
                  InternalServerError(throwable.getMessage)
              }
            }
          case None =>
            BadRequest("tie parameter is missing")
        }
      case None =>
        NotFound("Game not found")
    }
  }

}