package controllers

import auth.{AuthAction, AuthenticatedRequest}
import de.knockoutwhist.control.GameState
import de.knockoutwhist.control.GameState.*
import exceptions.*
import logic.PodManager
import logic.game.GameLobby
import model.sessions.UserSession
import model.users.User
import play.api.*
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import play.twirl.api.Html
import util.GameUtil

import java.util.UUID
import javax.inject.*
import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class IngameController @Inject()(
                                  val cc: ControllerComponents,
                                  val authAction: AuthAction,
                                  implicit val ec: ExecutionContext
                                ) extends AbstractController(cc) {

  def game(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = PodManager.getGame(gameId)
    game match {
      case Some(g) =>
        val results = Try {
          IngameController.returnInnerHTML(g, g.logic.getCurrentState, request.user)
        }
        if (results.isSuccess) {
          Ok(views.html.main("Knockout Whist - " + GameUtil.stateToTitle(g.logic.getCurrentState))(results.get))
        } else {
          InternalServerError(results.failed.get.getMessage)
        }
      case None =>
        Redirect(routes.MainMenuController.mainMenu())
    }
  }

  def startGame(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = PodManager.getGame(gameId)
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
        "redirectUrl" -> routes.IngameController.game(gameId).url,
        "content" -> IngameController.returnInnerHTML(game.get, game.get.logic.getCurrentState, request.user).toString()
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
    val game = PodManager.getGame(gameId)
    val playerToKickUUID = UUID.fromString(playerToKick)
    val result = Try {
      game.get.leaveGame(playerToKickUUID, true)
    }
    if (result.isSuccess) {
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
    val game = PodManager.getGame(gameId)
    val result = Try {
      game.get.leaveGame(request.user.id, false)
    }
    if (result.isSuccess) {
      Ok(Json.obj(
        "status" -> "success",
        "redirectUrl" -> routes.MainMenuController.mainMenu().url,
        "content" -> views.html.mainmenu.creategame(Some(request.user)).toString
      ))
    } else {
      InternalServerError(Json.obj(
        "status" -> "failure",
        "errorMessage" -> "Something went wrong."
      ))
    }
  }

  def playCard(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] => {
    val game = PodManager.getGame(gameId)
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
                "status" -> "success"
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
                case _: NotInteractableException =>
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
    val game = PodManager.getGame(gameId)
    game match {
      case Some(g) => {
        val jsonBody = request.body.asJson
        val cardIdOpt: Option[String] = jsonBody.flatMap { jsValue =>
          (jsValue \ "cardID").asOpt[String]
        }
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
      }
      case None =>
        NotFound("Game not found")
    }
  }
  }

  def playTrump(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = PodManager.getGame(gameId)
    game match {
      case Some(g) =>
        val jsonBody = request.body.asJson
        val trumpOpt: Option[String] = jsonBody.flatMap { jsValue =>
          (jsValue \ "trump").asOpt[String]
        }
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
                  BadRequest(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
                case _: NotInThisGameException =>
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
            BadRequest("trump parameter is missing")
        }
      case None =>
        NotFound("Game not found")
    }
  }

  def playTie(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = PodManager.getGame(gameId)
    game match {
      case Some(g) =>
        val jsonBody = request.body.asJson
        val tieOpt: Option[String] = jsonBody.flatMap { jsValue =>
          (jsValue \ "tie").asOpt[String]
        }
        tieOpt match {
          case Some(tie) =>
            var optSession: Option[UserSession] = None
            val result = Try {
              val session = g.getUserSession(request.user.id)
              optSession = Some(session)
              session.lock.lock()
              g.selectTie(g.getUserSession(request.user.id), tie.toInt)
            }
            optSession.foreach(_.lock.unlock())
            if (result.isSuccess) {
              NoContent
            } else {
              val throwable = result.failed.get
              throwable match {
                case _: IllegalArgumentException =>
                  BadRequest(Json.obj(
                    "status" -> "failure",
                    "errorMessage" -> throwable.getMessage
                  ))
                case _: NotInThisGameException =>
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
            BadRequest("tie parameter is missing")
        }
      case None =>
        NotFound("Game not found")
    }
  }


  def returnToLobby(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    val game = PodManager.getGame(gameId)
    game match {
      case Some(g) =>
        val result = Try {
          val session = g.getUserSession(request.user.id)
          g.returnToLobby(session)
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
        NotFound(Json.obj(
          "status" -> "failure",
          "errorMessage" -> "Game not found"
        ))
    }
  }
  
}

object IngameController {

  def returnInnerHTML(gameLobby: GameLobby, gameState: GameState, user: User): Html = {
    gameState match {
      case Lobby => views.html.lobby.lobby(Some(user), gameLobby)
      case InGame =>
        views.html.ingame.ingame(
          gameLobby.getPlayerByUser(user),
          gameLobby
        )
      case SelectTrump =>
        views.html.ingame.selecttrump(
          gameLobby.getPlayerByUser(user),
          gameLobby
        )
      case TieBreak =>
        views.html.ingame.tie(
          gameLobby.getPlayerByUser(user),
          gameLobby
        )
      case FinishedMatch =>
        views.html.ingame.finishedMatch(
          Some(user),
          gameLobby
        )
      case _ =>
        throw new IllegalStateException(s"Invalid game state for in-game view. GameId: ${gameLobby.id}" + s" State: ${gameLobby.logic.getCurrentState}")
    }
  }
  
}
