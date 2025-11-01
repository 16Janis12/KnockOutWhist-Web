package controllers

import auth.{AuthAction, AuthenticatedRequest}
import de.knockoutwhist.control.GameState.{InGame, Lobby, SelectTrump, TieBreak}
import exceptions.{CantPlayCardException, GameFullException, NotEnoughPlayersException, NotHostException, NotInThisGameException}
import logic.PodManager
import play.api.*
import play.api.mvc.*

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
          case Lobby => Ok("Lobby: " + gameId)
          case InGame =>
            Ok(views.html.ingame.ingame(
              g.getPlayerByUser(request.user),
              g.logic
            ))
          case SelectTrump =>
            Ok(views.html.ingame.selecttrump(
              g.getPlayerByUser(request.user),
              g.logic
            ))
          case TieBreak =>
            Ok(views.html.ingame.tie(
              g.getPlayerByUser(request.user),
              g.logic
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
      NoContent
    } else {
      val throwable = result.failed.get
      throwable match {
        case _: NotInThisGameException =>
          BadRequest(throwable.getMessage)
        case _: NotHostException =>
          Forbidden(throwable.getMessage)
        case _: NotEnoughPlayersException =>
          BadRequest(throwable.getMessage)
        case _ =>
          InternalServerError(throwable.getMessage)
      }
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
        val cardIdOpt = request.body.asFormUrlEncoded.flatMap(_.get("cardId").flatMap(_.headOption))
        cardIdOpt match {
          case Some(cardId) =>
            val result = Try {
              g.playCard(g.getUserSession(request.user.id), cardId.toInt)
            }
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
          case None =>
            BadRequest("cardId parameter is missing")
        }
      case None =>
        NotFound("Game not found")
    }
  }
  }
  def playDogCard(gameId: String): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] => {
    val game = podManager.getGame(gameId)
    game match {
      case Some(g) => {
        val cardIdOpt = request.body.asFormUrlEncoded.flatMap(_.get("cardId").flatMap(_.headOption))
        val result = Try {
          cardIdOpt match {
            case Some(cardId) if cardId == "skip" =>
              g.playDogCard(g.getUserSession(request.user.id), -1)
            case Some(cardId) =>
              g.playDogCard(g.getUserSession(request.user.id), cardId.toInt)
            case None =>
              throw new IllegalArgumentException("cardId parameter is missing")
          }
        }
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
            val result = Try {
              g.selectTrump(g.getUserSession(request.user.id), trump.toInt)
            }
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
            val result = Try {
              g.selectTie(g.getUserSession(request.user.id), tie.toInt)
            }
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