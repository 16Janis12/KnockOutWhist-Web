package controllers

import javax.inject.*
import play.api.*
import play.api.mvc.*
import de.knockoutwhist.KnockOutWhist
import de.knockoutwhist.control.ControlHandler
import de.knockoutwhist.ui.tui.TUIMain

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  private var initial = false

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = {
    if (!initial) {
      initial = true
      ControlHandler.addListener(WebUI)
      KnockOutWhist.main(new Array[String](_length = 0))
    }
    Action { implicit request =>
      Ok(views.html.index.apply())
    }
  }

  def ingame(): Action[AnyContent] = {
    Action { implicit request =>
      Ok(views.html.tui.apply(WebUI.latestOutput))
    }
  }

  def showTUI(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.tui.render(WebUI.latestOutput))
  }
}