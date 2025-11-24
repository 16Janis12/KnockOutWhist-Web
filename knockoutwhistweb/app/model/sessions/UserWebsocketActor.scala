package model.sessions

import de.knockoutwhist.utils.events.SimpleEvent
import org.apache.pekko.actor.{Actor, ActorRef}
import play.api.libs.json.{JsObject, JsValue, Json}
import util.WebsocketEventMapper

import scala.util.{Failure, Success, Try}

class UserWebsocketActor(
                          out: ActorRef,
                          session: UserSession
                        ) extends Actor {

  if (session.websocketActor.isDefined) {
    session.websocketActor.foreach(actor => actor.transmitTextToClient("Error: Multiple websocket connections detected. Closing this connection."))
    context.stop(self)
  } else {
    session.websocketActor = Some(this)
  }

  override def receive: Receive = {
    case msg: String =>
      val jsonObject = Try {
        Json.parse(msg)
      }
      Try {
        jsonObject match {
          case Success(value) =>
            handle(value)
          case Failure(exception) =>
            transmitTextToClient(s"Error parsing JSON: ${exception.getMessage}")
        }
      }.failed.foreach(
        ex => transmitTextToClient(s"Error handling message: ${ex.getMessage}")
      )
    case other =>
  }

  private def transmitTextToClient(text: String): Unit = {
    out ! text
  }

  private def handle(json: JsValue): Unit = {
    val idOpt = (json \ "id").asOpt[String]
    if (idOpt.isEmpty) {
      transmitJsonToClient(Json.obj(
        "status" -> "error",
        "error" -> "Missing 'id' field"
      ))
      return
    }
    val id = idOpt.get
    val eventOpt = (json \ "event").asOpt[String]
    if (eventOpt.isEmpty) {
      transmitJsonToClient(Json.obj(
        "id" -> id,
        "event" -> null,
        "status" -> "error",
        "error" -> "Missing 'event' field"
      ))
      return
    }
    val statusOpt = (json \ "status").asOpt[String]
    if (statusOpt.isDefined) {
      return
    }
    val event = eventOpt.get
    val data = (json \ "data").asOpt[JsObject].getOrElse(Json.obj())
    val result = Try {
      session.handleWebResponse(event, data)
    }
    if (result.isSuccess) {
      transmitJsonToClient(Json.obj(
        "id" -> id,
        "event" -> event,
        "status" -> "success"
      ))
    } else {
      transmitJsonToClient(Json.obj(
        "id" -> id,
        "event" -> event,
        "status" -> "error",
        "error" -> result.failed.get.getMessage
      ))
    }
  }

  def transmitJsonToClient(jsonObj: JsObject): Unit = {
    transmitTextToClient(jsonObj.toString())
  }

  def transmitEventToClient(event: SimpleEvent): Unit = {
    transmitJsonToClient(WebsocketEventMapper.toJson(event))
  }

}
