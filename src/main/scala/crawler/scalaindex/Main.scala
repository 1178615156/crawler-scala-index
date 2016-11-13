package crawler.scalaindex

import java.io.{BufferedReader, InputStreamReader}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import crawler.scalaindex.ScalaIndexCrawler.{DoRun, LibResult, RootTask}
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Success, Try}

/**
  * Created by yujieshui on 2016/11/13.
  */
class DoSbtCache(scalaVersion: Seq[String], rootTask: RootTask) extends PersistentActor {
  def asWin = System.getProperty("os.name").toLowerCase.startsWith("win")

  val log = LoggerFactory getLogger ("do-sbt-cache")

  def exec(cmd: String) = {
    if(asWin)
      Process(Seq("cmd.exe", "/c", cmd)) lineStream
    else
      Process(Seq("bash", "-c", cmd)) lineStream
  }

  var cacheStatus = Map[LibResult, Option[Seq[String]]]()

  override def receiveRecover: Receive = {
    case x@ScalaIndexCrawler.LibResult(query, result) =>
      mark(x)
    case RecoveryCompleted                            =>
      cacheStatus.collect { case (key, None) => key } foreach (self ! _)
  }

  override def receiveCommand: Receive = {
    case x@ScalaIndexCrawler.LibResult(query, result) => persist(x) { x =>
      mark(x)

      val cmds = for {
        version <- scalaVersion
        lib <- result.get.list
      } yield
        s""" sbt '++$version' 'set libraryDependencies+=$lib' 'update' """
      cmds.foreach(cmd => {
        Try {
          val out = exec(cmd)
          out.foreach(log.info(_))
        } match {
          case Success(x) => log.info(s"cache success : $cmd")
          case Failure(x) => log.error(x.toString)
        }
      })
      log.info(s"cache finish :${result.get.list}")
      cacheStatus += x -> Some(cmds)
    }

  }

  def mark(x: LibResult) = cacheStatus += x -> None

  override def persistenceId: String = s"do-sbt-cache-$rootTask"
}

object Main {
  def config = ConfigFactory load()

  implicit lazy val actorSystem        = ActorSystem("scalaIndexCrawler", config)
  implicit lazy val materializer       = ActorMaterializer()
  implicit lazy val wsClient: WSClient = AhcWSClient()
  implicit lazy val environment        = new ScalaIndexCrawlerEnvironment {}
  implicit      val timeout            = Timeout(10.second)

  def main(args: Array[String]): Unit = {
    val rootTask: RootTask = RootTask(Some("targets:scala_2.11"), Some("stars"), Some(1), Some(1))
    val crawlerPage: CrawlerPage = new CrawlerPage(wsClient)
    val crawlerLib: CrawlerLib = new CrawlerLib(wsClient)
    val doSbtCache = actorSystem.actorOf(Props(new DoSbtCache(
      Seq("2.11.8", "2.12.0"), rootTask
    )))
    lazy val scalaIndexCrawler = actorSystem.actorOf(Props(new ScalaIndexCrawler(
      rootTask, crawlerPage, crawlerLib, doSbtCache
    )))
    scalaIndexCrawler ! DoRun()
  }

}
