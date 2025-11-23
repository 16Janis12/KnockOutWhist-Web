package util

import de.knockoutwhist.utils.events.SimpleEvent
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.ScalaModule

object WebsocketEventMapper {

  private val scalaModule = ScalaModule.builder()
    .addAllBuiltinModules()
    .supportScala3Classes(true)
    .build()

  private val mapper = JsonMapper.builder().addModule(scalaModule).build()

  def toJsonString(obj: SimpleEvent): String = {
    mapper.writeValueAsString(obj)
  }

}
