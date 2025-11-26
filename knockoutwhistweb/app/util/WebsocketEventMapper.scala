package util

import de.knockoutwhist.utils.events.SimpleEvent
import logic.game.GameLobby
import model.sessions.UserSession
import play.api.libs.json.{JsValue, Json}
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.ScalaModule
import util.mapper.{GameStateEventMapper, ReceivedHandEventMapper, SimpleEventMapper}

object WebsocketEventMapper {

  private val scalaModule = ScalaModule.builder()
    .addAllBuiltinModules()
    .supportScala3Classes(true)
    .build()

  private val mapper = JsonMapper.builder().addModule(scalaModule).build()

  private var customMappers: Map[String,SimpleEventMapper[SimpleEvent]] = Map()

  private def registerCustomMapper[T <: SimpleEvent](mapper: SimpleEventMapper[T]): Unit = {
    customMappers = customMappers + (mapper.id -> mapper.asInstanceOf[SimpleEventMapper[SimpleEvent]])
  }
  
  // Register all custom mappers here
  registerCustomMapper(ReceivedHandEventMapper)
  registerCustomMapper(GameStateEventMapper)
  
  def toJson(obj: SimpleEvent, session: UserSession): JsValue = {
    val data: Option[JsValue] = if (customMappers.contains(obj.id)) {
      Some(customMappers(obj.id).toJson(obj, session))
    }else {
      None
    }
    if (data.isEmpty) {
      return Json.obj()
    }
    Json.obj(
      "id" -> ("request-" + java.util.UUID.randomUUID().toString),
      "event" -> obj.id,
      "data" -> data
    )
  }

}
