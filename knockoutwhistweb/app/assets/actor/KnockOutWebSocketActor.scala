package actor

import org.apache.pekko.actor.{Actor, ActorRef}
import org.apache.pekko.http.scaladsl.model.ContentRange.Other


class KnockOutWebSocketActor(
                              out: ActorRef,
                            ) extends Actor {
    def receive: Receive = {
      case msg: String =>
        out ! s"Received your message: ${msg}"
      case other: Other =>
        println(s"Received unknown message: $other")
    }

    def sendJsonToClient(json: String): Unit = {
      println("Received event from Controller")
      out ! json
    }
}
