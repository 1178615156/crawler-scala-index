package scalaindex


import akka.actor.{ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}
import scalaindex.DoSbtCache.Task
import scalaindex.crawler.ScalaIndexCrawler.{DoRun, RootTask}
import scalaindex.crawler._

object DoSbtCache {

  case class Task(scalaVersion: String, lib: String)

  case class TaskResult(task: Task, result: String)

  def cacheCmd(doCache: Task) = {
    s""" sbt '++${doCache.scalaVersion}' 'set libraryDependencies+=${doCache.lib}' 'update' """
  }

  def exec(cmd: String) = {
    def asWin = System.getProperty("os.name").toLowerCase.startsWith("win")
    if(asWin)
      Process(Seq("cmd.exe", "/c", cmd)).lineStream
    else
      Process(Seq("bash", "-c", cmd)).lineStream
  }
}

import scalaindex.DoSbtCache._

class DoSbtCache(scalaVersionList: Seq[String], rootTask: RootTask)
  extends PersistentActor
    with TaskPersistent[DoSbtCache.Task, DoSbtCache.TaskResult] {

  val log    = LoggerFactory getLogger "do-sbt-cache"
  val sbtLog = LoggerFactory getLogger "sbt-log"

  override def receiveRecover: Receive = taskRecover

  override def receiveCommand: Receive = taskCommand

  override def persistenceId: String = s"do-sbt-cache-$rootTask"

  override def runTask(task: Task): Unit = {
    Try {
      log.info(s"try to cache $task")
      exec(cacheCmd(task)).foreach(sbtLog.info(_))
    } match {
      case Success(e) => log.info(s"cache success $task")
      case Failure(e) => log.error(s"cache failure $task ::$e")
    }
    self ! TaskResult(task, cacheCmd(task))
  }

  override def runResult(result: TaskResult): Unit = ()

  override def result2task(result: TaskResult): Task = result.task
}


object Main {

  implicit lazy val actorSystem        = ActorSystem("scalaIndexCrawler")
  implicit lazy val materializer       = ActorMaterializer()
  implicit lazy val wsClient: WSClient = AhcWSClient()
  implicit lazy val environment        = new ScalaIndexCrawlerEnvironment {}
  implicit      val timeout            = Timeout(10.second)

  def main(args: Array[String]): Unit = {
    val rootTask: RootTask = RootTask(
      q = Some(CrawlerScalaIndexConfig.q),
      sort = Some(CrawlerScalaIndexConfig.sort),
      pageStart = Some(CrawlerScalaIndexConfig.pageStart),
      pageEnd = Some(CrawlerScalaIndexConfig.pageEnd)
    )
    val crawlerPage: CrawlerPage = new CrawlerPage(wsClient)
    val crawlerLib: CrawlerLib = new CrawlerLib(wsClient)
    val doSbtCache = actorSystem.actorOf(Props(new DoSbtCache(
      CrawlerScalaIndexConfig.scalaVersion, rootTask
    )))
    lazy val scalaIndexCrawler = actorSystem.actorOf(Props(new ScalaIndexCrawler(
      rootTask, crawlerPage, crawlerLib, doSbtCache
    )))
    scalaIndexCrawler ! DoRun()
  }

}
