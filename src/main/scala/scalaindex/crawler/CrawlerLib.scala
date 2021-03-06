package scalaindex.crawler

import org.jsoup.Jsoup
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scalaindex.ScalaIndexCrawlerEnvironment
import scalaj.http.Http

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
  type Sources = String
  type Parse = CrawlerLib.Result

  override def sources(query: Query): Future[Sources] = {
    wSClient.url(s"$index_scala_host/${query.string}")
      .get()
      .map(_.body)
//    Future.successful(
//      Http(s"$index_scala_host/${query.string}").asString.body
//    )
  }

  override def parse(sources: Sources): Future[Parse] = {
    val lib = Jsoup
      .parse(sources)
      .select("#sbt > pre")
      .first().text()
    Future.successful(CrawlerLib.Result(List(lib)))
  }


}