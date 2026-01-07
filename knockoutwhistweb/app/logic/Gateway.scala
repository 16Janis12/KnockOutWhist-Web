package logic

import de.knockoutwhist.data.Pod
import de.knockoutwhist.data.redis.RedisManager
import org.apache.pekko.actor.ActorSystem
import org.redisson.config.Config
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import java.util
import java.util.UUID
import javax.inject.*
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

@Singleton
class Gateway @Inject()(
                         lifecycle: ApplicationLifecycle,
                         actorSystem: ActorSystem
                       )(implicit ec: ExecutionContext)  {

  private val logger = Logger(getClass.getName)
  
  val redis: RedisManager = {
    val config: Config = Config()
    val url = "redis://" + sys.env.getOrElse("REDIS_HOST", "localhost") + ":" + sys.env.getOrElse("REDIS_PORT", "6379")
    logger.info(s"Connecting to Redis at $url")
    config.useSingleServer.setAddress(url)
    RedisManager(config)
  }

  redis.continuousSyncPod(() => {
    logger.info("Syncing pod with Redis")
    createPod()
  })
  
  logger.info("Gateway started")

  def syncPod(): Unit = {
    redis.syncPod(createPod())
  }

  private def createPod(): Pod = {
    Pod(
      UUID.randomUUID().toString,
      PodManager.podName,
      PodManager.podIp,
      9000,
      new util.ArrayList[String](PodManager.getAllGameIds().asJava),
      new util.ArrayList[String](PodManager.allBoundUsers().asJava)
    )
  }

}
