package controllers

import auth.{AuthAction, AuthenticatedRequest}
import logic.PodManager
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.routing.JavaScriptReverseRouter

import javax.inject.Inject

class JavaScriptRoutingController  @Inject()(
                                              val controllerComponents: ControllerComponents,
                                              val authAction: AuthAction,
                                              val podManager: PodManager
                                            ) extends BaseController {
  def javascriptRoutes(): Action[AnyContent] = authAction { implicit request: AuthenticatedRequest[AnyContent] =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
          routes.javascript.MainMenuController.createGame,
          routes.javascript.IngameController.startGame,
          routes.javascript.IngameController.kickPlayer,
          routes.javascript.IngameController.leaveGame,
          routes.javascript.IngameController.playCard,
          routes.javascript.IngameController.polling
      )
    ).as("text/javascript")
  }
}
