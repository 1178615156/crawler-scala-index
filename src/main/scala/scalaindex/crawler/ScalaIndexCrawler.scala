package scalaindex.crawler

import akka.actor.{Actor, ActorRef}
import akka.pattern._
import org.slf4j.LoggerFactory


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
  val log = LoggerFactory getLogger "scala-index-crawler"

  import context.dispatcher

  override def receive: Receive = {
    case x: DoRun              => task2pageQuery(rootTask) foreach (self ! _)
    case x: CrawlerPage.Query  => page.crawler(x) foreach (self ! _)
    case x: CrawlerPage.Result =>
      x.list.map(CrawlerLib.Query(_)).map(lib.crawler).foreach(_ pipeTo pipeActor)
  }
}


