package util

import de.knockoutwhist.control.GameState
import de.knockoutwhist.control.GameState.{FinishedMatch, InGame, Lobby, SelectTrump, TieBreak}
import de.knockoutwhist.utils.events.SimpleEvent
import dto.subDTO.{CardDTO, HandDTO, PlayerDTO, PlayerQueueDTO, PodiumPlayerDTO, RoundDTO, TrickDTO, UserDTO}
import dto.{GameInfoDTO, LobbyInfoDTO, TieInfoDTO, TrumpInfoDTO, WonInfoDTO}
import model.sessions.UserSession
import play.api.libs.json.{JsValue, Json, OFormat}
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.ScalaModule
import util.mapper.*

object WebsocketEventMapper {

  implicit val cardFormat: OFormat[CardDTO] = Json.format[CardDTO]
  implicit val handFormat: OFormat[HandDTO] = Json.format[HandDTO]
  implicit val playerFormat: OFormat[PlayerDTO] = Json.format[PlayerDTO]
  implicit val queueFormat: OFormat[PlayerQueueDTO] = Json.format[PlayerQueueDTO]
  implicit val podiumPlayerFormat: OFormat[PodiumPlayerDTO] = Json.format[PodiumPlayerDTO]
  implicit val roundFormat: OFormat[RoundDTO] = Json.format[RoundDTO]
  implicit val trickFormat: OFormat[TrickDTO] = Json.format[TrickDTO]
  implicit val userFormat: OFormat[UserDTO] = Json.format[UserDTO]

  implicit val gameInfoDTOFormat: OFormat[GameInfoDTO] = Json.format[GameInfoDTO]
  implicit val lobbyFormat: OFormat[LobbyInfoDTO] = Json.format[LobbyInfoDTO]
  implicit val tieInfoFormat: OFormat[TieInfoDTO] = Json.format[TieInfoDTO]
  implicit val trumpInfoFormat: OFormat[TrumpInfoDTO] = Json.format[TrumpInfoDTO]
  implicit val wonInfoDTOFormat: OFormat[WonInfoDTO] = Json.format[WonInfoDTO]
  
  private var specialMappers: Map[String,SimpleEventMapper[SimpleEvent]] = Map()

  private def registerCustomMapper[T <: SimpleEvent](mapper: SimpleEventMapper[T]): Unit = {
    specialMappers = specialMappers + (mapper.id -> mapper.asInstanceOf[SimpleEventMapper[SimpleEvent]])
  }
  
  // Register all custom mappers here
  registerCustomMapper(ReceivedHandEventMapper)
  registerCustomMapper(GameStateEventMapper)
  registerCustomMapper(CardPlayedEventMapper)
  registerCustomMapper(NewRoundEventMapper)
  registerCustomMapper(NewTrickEventMapper)
  registerCustomMapper(TrickEndEventMapper)
  registerCustomMapper(RequestCardEventMapper)
  registerCustomMapper(LobbyUpdateEventMapper)
  registerCustomMapper(LeftEventMapper)
  registerCustomMapper(KickEventMapper)
  registerCustomMapper(SessionClosedMapper)
  registerCustomMapper(TurnEventMapper)

  def toJson(obj: SimpleEvent, session: UserSession): JsValue = {
    val data: Option[JsValue] = if (specialMappers.contains(obj.id)) {
      Some(specialMappers(obj.id).toJson(obj, session))
    }else {
      None
    }
    Json.obj(
      "id" -> ("request-" + java.util.UUID.randomUUID().toString),
      "event" -> obj.id,
      "state" -> toJson(session),
      "data" -> data
    )
  }

  def toJson(session: UserSession): JsValue = {
    session.gameLobby.getLogic.getCurrentState match {
      case Lobby => Json.toJson(LobbyInfoDTO(session.gameLobby, session.user))
      case InGame => Json.toJson(GameInfoDTO(session.gameLobby, session.user))
      case SelectTrump => Json.toJson(TrumpInfoDTO(session.gameLobby, session.user))
      case TieBreak => Json.toJson(TieInfoDTO(session.gameLobby, session.user))
      case FinishedMatch => Json.toJson(WonInfoDTO(session.gameLobby, session.user))
      case _ => Json.obj()
    }
  }

}
