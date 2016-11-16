package scalaindex.crawler

import akka.actor.ActorRef
import akka.pattern._
import akka.persistence.{PersistentActor, RecoveryCompleted}
import org.slf4j.LoggerFactory


object ScalaIndexCrawler {

  case class RootTask(q: Option[String],
                      sort: Option[String],
                      pageStart: Option[Int],
                      pageEnd: Option[Int])

  case class GetResult()

  case class DoRun()

  def task2pageQuery(task: RootTask): Seq[CrawlerPage.Query] = {
    val pages = task.pageStart.getOrElse(0) to task.pageEnd.getOrElse(10)
    pages.map(i => CrawlerPage.Query(task.q, Some(i), task.sort))
  }

  case class PageResult(query: CrawlerPage.Query, result: Option[CrawlerPage.Result])

  case class LibResult(query: CrawlerLib.Query, result: Option[CrawlerLib.Result])

}

import ScalaIndexCrawler._

final class
ScalaIndexCrawler(val rootTask: RootTask,
                  val crawlerPage: CrawlerPage,
                  val crawlerLib: CrawlerLib,
                  val pipeActor: ActorRef
                 )(implicit val environment: ScalaIndexCrawlerEnvironment)
  extends PersistentActor {
  val log = LoggerFactory getLogger "scala-index-crawler"

  import context.dispatcher

  val persistenceId: String = s"scala-index-crawler-$rootTask"

  var pageStats = Map[CrawlerPage.Query, PageResult]()
  var libStatus = Map[CrawlerLib.Query, LibResult]()

  var asRun                   = false
  val receiveCommand: Receive = {
    case x@DoRun() if !asRun => persist(x) { x =>
      asRun = true
      val pageQuerys = task2pageQuery(rootTask)
      pageQuerys foreach (self ! _)
      sender() ! pageQuerys
    }
    case x@DoRun() if asRun  => sender() ! pageStats.keys.toList
    case x@GetResult()       => sender() ! libStatus.values.toList

    case query: CrawlerPage.Query if !finish(query) => persist(query) { query =>
      log.info(s"receive page query : $query")
      markPage(query)
    }
      crawlerPage.crawler(query) map (e => PageResult(query, Some(e))) pipeTo self
    case x@PageResult(query, Some(result))          => persist(x) { x =>
      log.info(s"receive page result:  $result")
      updatePageStatus(x)
    }
      result.list.map(e => CrawlerLib.Query(e)).foreach(self ! _)

    case query: CrawlerLib.Query if !finish(query) => persist(query) { query =>
      log.info(s"receive lib query : $query")
      markLib(query)
    }
      crawlerLib.crawler(query).map(e => LibResult(query, Some(e))).pipeTo(self)
    case x@LibResult(query, Some(result))          => persist(x) { x =>
      log.info(s"receive lib result:  $result")
      updateLibStatus(x)
      pipeActor ! x
    }


  }

  val receiveRecover: Receive = {
    case x: DoRun                 => asRun = true
    case query: CrawlerPage.Query => log.info(s"recover $query"); markPage(query)
    case query: CrawlerLib.Query  => log.info(s"recover $query"); markLib(query)
    case result: PageResult       => log.info(s"recover $result"); updatePageStatus(result)
    case result: LibResult        => log.info(s"recover $result"); updateLibStatus(result)
    case RecoveryCompleted        =>
      pageStats.values.filter(_.result.isEmpty) map (_.query) foreach (self ! _)
      libStatus.values.filter(_.result.isEmpty) map (_.query) foreach (self ! _)
  }


  def markPage(query: crawlerPage.Query) = pageStats += query -> PageResult(query, None)

  def markLib(query: crawlerLib.Query) = libStatus += query -> LibResult(query, None)

  def updatePageStatus(result: PageResult) = pageStats += result.query -> result

  def updateLibStatus(result: LibResult) = libStatus += result.query -> result

  def finish(query: crawlerPage.Query) = pageStats.get(query).exists(_.result.nonEmpty)

  def finish(query: crawlerLib.Query) = libStatus.get(query).exists(_.result.nonEmpty)
}


