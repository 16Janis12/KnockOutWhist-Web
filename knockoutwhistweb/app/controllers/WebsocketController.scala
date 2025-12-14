package controllers


import auth.AuthAction
import logic.PodManager
import logic.user.SessionManager
import model.sessions.{UserSession, UserWebsocketActor}
import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.stream.Materializer
import play.api.*
import play.api.libs.streams.ActorFlow
import play.api.mvc.*

import javax.inject.*


@Singleton
class WebsocketController @Inject()(
                                     cc: ControllerComponents,
                                     val sessionManger: SessionManager,
                                   )(implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {

  def socket(): WebSocket = WebSocket.accept[String, String] { request =>
    val session = request.cookies.get("accessToken")
    if (session.isEmpty) throw new Exception("No session cookie found")
    val userOpt = sessionManger.getUserBySession(session.get.value)
    if (userOpt.isEmpty) throw new Exception("Invalid session")
    val user = userOpt.get
    val game = PodManager.identifyGameOfUser(user)
    if (game.isEmpty) throw new Exception("User is not in a game")
    val userSession = game.get.getUserSession(user.id)
    ActorFlow.actorRef { out =>
      println("Connect received")
      KnockOutWebSocketActorFactory.create(out, userSession)
    }
  }

  object KnockOutWebSocketActorFactory {
    def create(out: ActorRef, userSession: UserSession): Props = {
      Props(new UserWebsocketActor(out, userSession))
    }
  }


}