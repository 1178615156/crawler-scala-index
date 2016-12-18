package scalaindex

import akka.actor.{ActorSystem, Cancellable}
import akka.persistence.PersistentActor
import org.jsoup.Jsoup
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._
import scalaindex.DoCache.{Task, TaskResult}
import CrawlerConfig._
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import scalaindex.crawler.CrawlerLib
import scalaj.http.Http
import scala.concurrent.duration._


/**
  * Created by yujieshui on 2016/12/17.
  */
object RecoverFuture {
  private val recoverLog = LoggerFactory getLogger "recover"

  def recoverFuture[T](future: => Future[T], delay: FiniteDuration, maxRecoverNum: Int)
                      (implicit
                       actorSystem: ActorSystem, executionContextExecutor: ExecutionContextExecutor): Future[T] = {
    val p = Promise[T]

    def timer(delay: FiniteDuration, currentRecoverNum: Int): Cancellable = actorSystem.scheduler.scheduleOnce(delay) {
      future onComplete {
        case Success(x) => p success x
        case Failure(x) =>
          recoverLog.warn(s"recover failure , exeNum:$currentRecoverNum: " + x)
          if(currentRecoverNum < maxRecoverNum)
            timer(delay, currentRecoverNum + 1)
          else
            p.failure(x)
      }
    }

    timer(0.second, 0)
    p.future
  }

}
