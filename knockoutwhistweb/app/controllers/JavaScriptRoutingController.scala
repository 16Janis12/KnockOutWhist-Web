package controllers

import auth.AuthAction
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.routing.JavaScriptReverseRouter

import javax.inject.Inject

class JavaScriptRoutingController @Inject()(
                                             val controllerComponents: ControllerComponents,
                                             val authAction: AuthAction,
                                           ) extends BaseController {
  def javascriptRoutes(): Action[AnyContent] =
    Action { implicit request =>
      Ok(
        JavaScriptReverseRouter("jsRoutes")(
          routes.javascript.MainMenuController.createGame,
          routes.javascript.MainMenuController.joinGame,
          routes.javascript.MainMenuController.navSPA,
          routes.javascript.UserController.login_Post
        )
      ).as("text/javascript")
    }
}
