package auth

import controllers.routes
import logic.user.SessionManager
import model.users.User
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

class AuthAction @Inject()(val sessionManager: SessionManager, val parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def executionContext: ExecutionContext = ec

  protected def getUserFromSession(request: RequestHeader): Option[User] = {
    val session = request.cookies.get("sessionId")
    if (session.isDefined)
      return sessionManager.getUserBySession(session.get.value)
    None
  }
  
  override def invokeBlock[A](
                               request: Request[A],
                               block: AuthenticatedRequest[A] => Future[Result]
                             ): Future[Result] = {
    getUserFromSession(request) match {
      case Some(user) =>
        block(new AuthenticatedRequest(user, request))
      case None =>
        Future.successful(Results.Redirect(routes.UserController.login()))
    }
  }
}

