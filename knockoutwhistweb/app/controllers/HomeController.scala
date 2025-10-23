package controllers

import controllers.sessions.AdvancedSession
import com.google.inject.{Guice, Injector}
import de.knockoutwhist.KnockOutWhist
import de.knockoutwhist.components.Configuration
import de.knockoutwhist.control.GameState.{InGame, Lobby, SelectTrump, TieBreak}
import de.knockoutwhist.control.controllerBaseImpl.BaseGameLogic
import di.KnockOutWebConfigurationModule
import play.api.{controllers, *}
import play.api.mvc.*
import play.twirl.api.Html

import java.util.UUID
import javax.inject.*


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  private var initial = false
  private val injector: Injector = Guice.createInjector(KnockOutWebConfigurationModule())

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
      KnockOutWhist.entry(injector.getInstance(classOf[Configuration]))
    }
    Action { implicit request =>
      Redirect("/sessions")
    }
  }

  def sessions(): Action[AnyContent] = {
    Action { implicit request =>
      Ok(views.html.sessions.apply(PodGameManager.listSessions()))
    }
  }

  def ingame(id: String): Action[AnyContent] = {
    val uuid: UUID = UUID.fromString(id)
    if (PodGameManager.identify(uuid).isEmpty) {
      return Action { implicit request =>
        NotFound(views.html.tui.apply(List(Html(s"<p>Session with id $id not found!</p>"))))
      }
    } else {
      val session = PodGameManager.identify(uuid).get
      val player = session.asInstanceOf[AdvancedSession].player
      val logic = WebUI.logic.get.asInstanceOf[BaseGameLogic]
      if (logic.getCurrentState == Lobby) {

      } else if (logic.getCurrentState == InGame) {
        return Action { implicit request =>
          Ok(views.html.ingame.apply(player, logic))
        }
      } else if (logic.getCurrentState == SelectTrump) {
        return Action { implicit request =>
          Ok(views.html.selecttrump.apply(player, logic))
        }
      } else if (logic.getCurrentState == TieBreak) {
        return Action { implicit request =>
          Ok(views.html.tie.apply(player, logic))
        }
      }
    }
    Action { implicit request =>
      InternalServerError("Oops")
    }
    //if (logic.getCurrentState == Lobby) {
    //Action { implicit request =>
    //Ok(views.html.tui.apply(player, logic))
    //}
    //} else {
    //Action { implicit request =>
    //Ok(views.html.tui.apply(player, logic))
    //}
  }
}