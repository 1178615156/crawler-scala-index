package scalaindex


import java.io.File

import akka.persistence.PersistentActor
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}
import scalaindex.CrawlerConfig.{withJavadoc, withSources}
import scalaindex.DoSbtCache.Task
import scalaindex.crawler.ScalaIndexCrawler.RootTask
import scalaindex.crawler._

object DoSbtCache {

  case class Task(scalaVersion: String, lib: String)

  case class TaskResult(task: Task, result: String)

  def cacheCmd(doCache: Task) = {
    new File("/tmp/sbt-cache").mkdir()
    val sv = s"++${doCache.scalaVersion}"
    val lib = s"(${doCache.lib}${if(false) " withSources() " else ""}${if(false) " withJavadoc() " else ""})"
    "cd /tmp/sbt-cache && " +
      s""" sbt '$sv' 'set libraryDependencies+=$lib' 'update' """
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

  val log              = LoggerFactory getLogger "do-sbt-cache"
  val sbtLog           = LoggerFactory getLogger "sbt-log"
  val executionContext =
    scala.concurrent.ExecutionContext.fromExecutor(java.util.concurrent.Executors.newFixedThreadPool(1))

  override def receiveRecover: Receive = taskRecover

  override def receiveCommand: Receive = taskCommand orElse {
    case CrawlerLib.Result(list) =>
      val tasks = for {
        sv <- scalaVersionList
        lib <- list
      } yield
        Task(sv, lib)
      tasks foreach (self ! _)
  }

  override def persistenceId: String = s"do-sbt-cache-$rootTask"

  override def runTask(task: Task): Unit = if(!isFinish(task) && !isDoing(task)) {
    val future = Future {
      Try {
        log.info(s"try to cache $task")
        exec(cacheCmd(task)).foreach(sbtLog.info(_))
      } match {
        case Success(e) => log.info(s"cache success $task")
        case Failure(e) => log.error(s"cache failure $task ::$e")
      }
      self ! TaskResult(task, cacheCmd(task))
    }(executionContext)
  }

  override def runResult(result: TaskResult): Unit = ()

  override def result2task(result: TaskResult): Task = result.task
}

//
//object Main {
//  implicit lazy val environment = new ScalaIndexCrawlerEnvironment {}
//
//  import environment._
//
//  def main(args: Array[String]): Unit = {
//    val rootTask: RootTask = RootTask(
//      q = Some(CrawlerConfig.q),
//      sort = Some(CrawlerConfig.sort),
//      pageStart = Some(CrawlerConfig.pageStart),
//      pageEnd = Some(CrawlerConfig.pageEnd)
//    )
//    val crawlerPage: CrawlerPage = new CrawlerPage(wsClient)
//    val crawlerLib: CrawlerLib = new CrawlerLib(wsClient)
//
//    val doSbtCache = actorSystem.actorOf(Props(new DoSbtCache(
//      CrawlerConfig.scalaVersion, rootTask
//    )))
//    lazy val scalaIndexCrawler = actorSystem.actorOf(Props(new ScalaIndexCrawler(
//      rootTask, crawlerPage, crawlerLib, doSbtCache
//    )))
//    scalaIndexCrawler ! DoRun()
//  }
//
//}
