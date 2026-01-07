package controllers

import auth.{AuthAction, AuthenticatedRequest}
import dto.subDTO.UserDTO
import logic.user.{SessionManager, UserManager}
import model.users.User
import play.api.*
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Cookie.SameSite.{Lax, None, Strict}

import javax.inject.*


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HealthController @Inject()(
                                val controllerComponents: ControllerComponents,
                              ) extends BaseController {

  def simple(): Action[AnyContent] = {
    Action { implicit request =>
      Ok("OK")
    }
  }

}