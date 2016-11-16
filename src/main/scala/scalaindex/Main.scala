package scalaindex


import akka.actor.{ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}
import scalaindex.DoSbtCache.DoCache
import scalaindex.crawler.ScalaIndexCrawler.{DoRun, RootTask}
import scalaindex.crawler._

/**
  * Created by yujieshui on 2016/11/13.
  */
trait TaskPersistent[Task,TaskResult] {
  self: PersistentActor =>
  var taskMap: Map[Task, Option[TaskResult]] = Map()

  def runTask(task: Task): Unit

  def runResult(result: TaskResult): Unit

  def markResult(result: TaskResult): Unit

  def markTask(task: Task): Unit = taskMap += task -> None

  def redoTask(task: Task) = runTask(task)

  def taskCommand(implicit TaskClass: ClassTag[Task], TaskResultClass: ClassTag[TaskResult]): Receive = {
    case TaskClass(task)         => persist(task) { task =>
      markTask(task)
      runTask(task)
    }
    case TaskResultClass(result) => persist(result) { result =>
      runResult(result)
      markResult(result)
    }
  }

  def taskRecover(implicit TaskClass: ClassTag[Task], TaskResultClass: ClassTag[TaskResult]): Receive = {
    case TaskClass(task)         => markTask(task)
    case TaskResultClass(result) => markResult(result)
    case RecoveryCompleted       =>
      taskMap.collect { case (key, None) => key }.foreach(redoTask)
  }
}

object DoSbtCache {

  case class DoCache(scalaVersion: String, lib: String)

  case class DoCacheResult(doCache: DoCache, result: Option[String])

  def cacheCmd(doCache: DoCache) = {
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

class DoSbtCache(scalaVersionList: Seq[String], rootTask: RootTask) extends PersistentActor {

  val log         = LoggerFactory getLogger "do-sbt-cache"
  val sbtLog      = LoggerFactory getLogger "sbt-log"
  var cacheStatus = Map[DoCache, DoCacheResult]()

  override def receiveRecover: Receive = {
    case x@DoCache(scalaVersion, lib) =>
      mark(x)
    case x: DoCacheResult             =>
      cacheStatus += x.doCache -> x
    case RecoveryCompleted            =>
      cacheStatus.values.collect { case DoCacheResult(doCache, None) => self ! doCache }
  }

  override def receiveCommand: Receive = {
    case x@DoCache(scalaVersion, lib)                 => persist(x) { x =>
      mark(x)
      Try {
        log.info(s"try to cache $x")
        exec(cacheCmd(x)).foreach(sbtLog.info(_))
      } match {
        case Success(e) => log.info(s"cache success $x")
        case Failure(e) => log.error(s"cache failure $x ::$e")
      }
      self ! DoCacheResult(x, Some(cacheCmd(x)))
    }
    case x: DoCacheResult                             =>
      cacheStatus += x.doCache -> x
    case x@ScalaIndexCrawler.LibResult(query, result) => persist(x) { x =>
      log.info(s"receive: $result")
      val waitDoCache = for {
        version <- scalaVersionList
        lib <- result.get.list
      } yield
        DoCache(version, lib)
      waitDoCache foreach (self ! _)
    }

  }


  def mark(x: DoCache) = cacheStatus += x -> DoCacheResult(x, None)

  override def persistenceId: String = s"do-sbt-cache-$rootTask"
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
