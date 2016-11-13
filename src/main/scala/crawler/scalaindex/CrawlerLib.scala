package crawler.scalaindex

import crawler.Crawler
import org.jsoup.Jsoup
import play.api.libs.ws.WSClient

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Created by yujieshui on 2016/11/13.
  */
object CrawlerLib {

  case class Query(string: String)

  case class Result(list: List[String])

}

class CrawlerLib(wSClient: WSClient)(implicit environment: ScalaIndexCrawlerEnvironment) extends Crawler {

  import environment._

  type Query = CrawlerLib.Query
  type SourcesResult = String
  type ParseResult = CrawlerLib.Result

  override def sources(query: Query): Future[SourcesResult] = {
    wSClient.url(s"$index_scala_host/${query.string}")
      .get()
      .map(_.body)
  }

  override def parse(sources: SourcesResult): Future[ParseResult] = {
    val lib = Jsoup
      .parse(sources)
      .select("#sbt > pre")
      .first().text()
    Future.successful(CrawlerLib.Result(List(lib)))
  }


}