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
object CrawlerPage{
  case class Query(q: Option[String], page: Option[Int], sort: Option[String])
  case class Result(list:List[String])
}

class CrawlerPage(wSClient: WSClient)(implicit environment: ScalaIndexCrawlerEnvironment) extends Crawler {

  import environment._

  override type Query = CrawlerPage.Query
  override type Sources = String
  override type Parse =CrawlerPage.Result

  override def sources(query: Query): Future[Sources] = {
    val queryString = List(
      query.q.map(_.toString).map("q" -> _),
      query.page.map(_.toString).map("page" -> _),
      query.sort.map(_.toString).map("sort" -> _)
    ).collect { case Some(x) => x }

    wSClient.url(s"$index_scala_host/search")
      .withQueryString(queryString: _*)
      .get()
      .map(_.body)
  }


  def parse(sources: Sources): Future[Parse] = {
    val list = Jsoup.parse(sources).select(
      "#container-search > div > div:nth-child(2) > div > div"
    ).first().children().toList

    val result =
      list
        .map(e => e.select("div > div > div.col-md-8 > a > h4").text())
    Future.successful(
      CrawlerPage.Result(result)
    )
  }
}
