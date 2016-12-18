package scalaindex

import java.util.concurrent.Executors

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor
import org.jsoup.Jsoup
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._
import scala.collection.immutable.Seq
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalaindex.crawler.ScalaIndexCrawler.{DoRun, RootTask}
import scalaindex.crawler.{CrawlerLib, CrawlerPage, ScalaIndexCrawler}
import scalaj.http.{Http, HttpResponse}


class DoCache(
               cacheConfig: CacheConfig
             ) extends PersistentActor with TaskPersistent[DoCache.Task, DoCache.TaskResult] {

  import DoCache._
  import context.system
  import cacheConfig._

  val log: Logger = LoggerFactory getLogger "do-cache"

  private val taskExecutionContext         = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
  private val taskExecutionContextCallBack = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  private val downloadLog = LoggerFactory getLogger "dowmload"
  private var doingFuture = Future.successful[Any](0)

  override def runTask(task: Task): Unit = if(!isFinish(task) && !isDoing(task)) {
    val jarTimeout = 120 * 1000
    val baseTimeout = 1 * 1000

    def download[T](url: String, t: => HttpResponse[T]) = {
      val f = Future(t)(taskExecutionContext)
      f.onComplete {
        case Success(x) => downloadLog.info(s"success [${x.code}] --${url}")
        case Failure(x) => downloadLog.warn(s"failure ${x.getMessage} --$url ")
      }(taskExecutionContextCallBack)
      f
    }

    task2request(task, repoxUrl).foreach { rt =>
      implicit val x = taskExecutionContext
      def future = for {
        pom <- download(rt.pom, Http(rt.pom).timeout(baseTimeout, baseTimeout).asString)

        depends = pom2Task(pom.body)
        _ = depends foreach (self ! _)

        pomShal <- download(rt.pomShal, Http(rt.pomShal).timeout(baseTimeout, baseTimeout).asString)
        jar <- download(rt.jar, Http(rt.jar).timeout(baseTimeout, baseTimeout).asBytes)
        jarShal <- download(rt.jarShal, Http(rt.jarShal).timeout(jarTimeout, jarTimeout).asBytes)

      } yield
        TaskResult(task, result = true)


      doingFuture flatMap { e =>
        val f = RecoverFuture.recoverFuture(future,3.second,reTryNum)
        val taskResult = f.recover { case e =>
          TaskResult(task, result = false)
          log.warn(s"failure $task  -- by ${e.getMessage}")
        }(taskExecutionContextCallBack)
        taskResult.onSuccess { case e => self ! e }(taskExecutionContextCallBack)
        taskResult
      }
    }
  }

  override def runResult(result: TaskResult): Unit = ()

  override def result2task(result: TaskResult): Task = result.task

  override def receiveRecover: Receive = taskRecover

  override def receiveCommand: Receive = taskCommand orElse {
    case x@CrawlerLib.Result(list) =>
      crawlerLibResult2doCacheTask(x, scalaVersion) foreach (self ! _)
  }

  override def persistenceId: String = "crawler-scala-index-do-cache"
}

object DoCache {

  case class Task(organization: String, name: String, version: String,
                  scalaVersion: Option[String] = None,
                  withSources: Boolean = false,
                  withJavadoc: Boolean = false)

  case class TaskResult(task: Task, result: Boolean)

  def crawlerLibResult2doCacheTask(libResult: CrawlerLib.Result, scalaVersion: Seq[String]): List[Task] = {
    val r1 = """"(.*)" ? %% ?"(.*)" ?% ?"(.*)"""".r
    val r2 = """"(.*)" ? % ?"(.*)" ?% ?"(.*)"""".r
    libResult.list.collect {
      case r1(organization, name, version) =>
        scalaVersion.map(sv => Task(organization, name, version, Some(sv)))
      case r2(organization, name, version) =>
        List(Task(organization, name, version))
    }.flatten
  }

  case class TaskRequest(pom: String, pomShal: String,
                         jar: String, jarShal: String) {
    override def toString: String = s"$pom , $jar"
  }

  def pom2Task(pom: String) = {
    Jsoup
      .parse(pom)
      .select("project > dependencies > dependency")
      .map(e => Task(
        organization = e.select("groupId").text(),
        name = e.select("artifactId").text(),
        version = e.select("version").text()
      ))
  }

  def task2request(task: Task, url: String): Seq[TaskRequest] = {
    def name2url(name: String) = name.replace('.', '/')

    task match {
      case Task(organization, name, version, None, withSources, withJavadoc)               =>
        val fileName = s"$name-$version"
        val baseUrl = s"$url/${name2url(organization)}/$name/$version"
        List(
          TaskRequest(
            s"$baseUrl/$fileName.pom",
            s"$baseUrl/$fileName.pom.sha1",
            s"$baseUrl/$fileName.jar",
            s"$baseUrl/$fileName.jar.sha1"
          )
        )
      case Task(organization, name, version, Some(scalaVersion), withSources, withJavadoc) =>
        val fileName = s"${name}_$scalaVersion-$version"
        val baseUrl = s"$url/${name2url(organization)}/${name}_$scalaVersion/$version"
        List(
          TaskRequest(
            s"$baseUrl/$fileName.pom",
            s"$baseUrl/$fileName.pom.sha1",
            s"$baseUrl/$fileName.jar",
            s"$baseUrl/$fileName.jar.sha1"
          )
        )
    }
  }
}

class Service()(implicit environment: ScalaIndexCrawlerEnvironment) {

  import environment._


  val crawlerPage: CrawlerPage = new CrawlerPage(wsClient)
  val crawlerLib : CrawlerLib  = new CrawlerLib(wsClient)

  val doSbtCache       : ActorRef = actorSystem.actorOf(Props(new DoCache(cacheConfig)))
  val scalaIndexCrawler: ActorRef = actorSystem.actorOf(Props(new ScalaIndexCrawler(
    rootTask, crawlerPage, crawlerLib, doSbtCache
  )))

  def run(): Unit = scalaIndexCrawler ! DoRun()
}

