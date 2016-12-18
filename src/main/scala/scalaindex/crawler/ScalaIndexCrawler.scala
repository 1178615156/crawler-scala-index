package scalaindex.crawler

import akka.actor.{Actor, ActorRef}
import akka.pattern._
import org.slf4j.LoggerFactory

import scalaindex.ScalaIndexCrawlerEnvironment


object ScalaIndexCrawler {

  case class RootTask(q: Option[String],
                      sort: Option[String],
                      pageStart: Option[Int],
                      pageEnd: Option[Int])

  def task2pageQuery(task: RootTask): Seq[CrawlerPage.Query] = {
    val pages = task.pageStart.getOrElse(0) to task.pageEnd.getOrElse(10)
    pages.map(i => CrawlerPage.Query(task.q, Some(i), task.sort))
  }

  case class DoRun()

}

import scalaindex.crawler.ScalaIndexCrawler._

final class
ScalaIndexCrawler(val rootTask: RootTask,
                  val page: CrawlerPage,
                  val lib: CrawlerLib,
                  val pipeActor: ActorRef
                 )(implicit val environment: ScalaIndexCrawlerEnvironment)
  extends Actor {
  val log = LoggerFactory getLogger "crawler"

  import context.dispatcher

  override def receive: Receive = {
    case x: DoRun =>
      log.info("run")
      task2pageQuery(rootTask) foreach (self ! _)

    case x: CrawlerPage.Query =>
      log.debug(s"page query: $x ")
      page.crawler(x) foreach (self ! _)

    case x: CrawlerPage.Result =>
      log.debug(s"page result: $x ")
      x.list map (CrawlerLib.Query(_)) foreach (self ! _)
    case x: CrawlerLib.Query   =>
      log.debug(s"lib query;$x")
      lib.crawler(x) foreach (self ! _)

    case x: CrawlerLib.Result =>
      log.debug(s"lib result;$x")
      pipeActor ! x
  }
}


