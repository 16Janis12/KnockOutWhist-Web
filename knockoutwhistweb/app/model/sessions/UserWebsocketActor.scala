package model.sessions

import de.knockoutwhist.utils.events.SimpleEvent
import org.apache.pekko.actor.{Actor, ActorRef}
import play.api.libs.json.{JsObject, JsValue, Json}
import util.WebsocketEventMapper

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class UserWebsocketActor(
                          out: ActorRef,
                          session: UserSession
                        ) extends Actor {

  private val requests: mutable.Map[String, String] = mutable.Map()

  {
    session.lock.lock()
    if (session.websocketActor.isDefined) {
      val otherWebsocket = session.websocketActor.get
      otherWebsocket.transmitTextToClient("Error: Multiple websocket connections detected. Closing your connection.")
      context.stop(otherWebsocket.self)
      transmitTextToClient("Previous websocket connection closed. You are now connected.")
    }
    session.websocketActor = Some(this)
    session.lock.unlock()
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
    session.lock.lock()
    val idOpt = (json \ "id").asOpt[String]
    if (idOpt.isEmpty) {
      transmitJsonToClient(Json.obj(
        "status" -> "error",
        "error" -> "Missing 'id' field"
      ))
      session.lock.unlock()
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
      session.lock.unlock()
      return
    }
    val statusOpt = (json \ "status").asOpt[String]
    if (statusOpt.isDefined) {
      session.lock.unlock()
      return
    }
    val event = eventOpt.get
    val data = (json \ "data").asOpt[JsObject].getOrElse(Json.obj())
    requests += (id -> event)
    val result = Try {
      session.handleWebResponse(event, data)
    }
    if (!requests.contains(id)) {
      session.lock.unlock()
      return
    }
    requests -= id
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
    session.lock.unlock()
  }

  def transmitJsonToClient(jsonObj: JsValue): Unit = {
    transmitTextToClient(jsonObj.toString())
  }

  def transmitEventToClient(event: SimpleEvent): Unit = {
    transmitJsonToClient(WebsocketEventMapper.toJson(event, session))
  }

  def solveRequests(): Unit = {
    if (!session.lock.isHeldByCurrentThread)
      return;
    if (requests.isEmpty)
      return;
    val pendingRequests = requests.toMap
    requests.clear()
    pendingRequests.foreach { case (id, event) =>
      transmitJsonToClient(Json.obj(
        "id" -> id,
        "event" -> event,
        "status" -> "success"
      ))
    }
  }

}
