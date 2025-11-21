package controllers
import actor.KnockOutWebSocketActor
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.stream.Materializer
import play.api.*
import play.api.libs.streams.ActorFlow
import play.api.mvc.*

import javax.inject.*


@Singleton
class WebsocketController @Inject()(
                                     cc: ControllerComponents,
                                   )(implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  object KnockOutWebSocketActorFactory {
    def create(out: ActorRef) = {
      Props(new KnockOutWebSocketActor(out))
    }
  }
  def socket() = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      println("Connect received")
      KnockOutWebSocketActorFactory.create(out)
    }
  }


}