package scalaindex.crawler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.scalatest.{BeforeAndAfterAll, FunSuite, Suite, WordSpecLike}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Created by yujieshui on 2016/11/13.
  */

trait TestResources extends BeforeAndAfterAll {
  this: Suite =>
  def config = ConfigFactory load "application.conf"

  lazy          val actorSystem        = ActorSystem("scalaIndexCrawler", config)
  implicit lazy val materializer       = ActorMaterializer()(actorSystem)
  implicit lazy val wsClient: WSClient = AhcWSClient()
  implicit lazy val environment        = new ScalaIndexCrawlerEnvironment {}

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    actorSystem
    materializer
    wsClient
    environment

  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    actorSystem.terminate()
    materializer.shutdown()
    wsClient.close()
  }

  implicit class aw[T](val future: Future[T]) {
    def await(duration: Duration = Duration.Inf): T = scala.concurrent.Await.result(future, duration)
  }

}

class CrawlerPageTest extends WordSpecLike with TestResources {
  lazy val crawlerPage = new CrawlerPage(wsClient)
  lazy val crawlerLib  = new CrawlerLib(wsClient)
  val query = CrawlerPage.Query(Some("targets:scala_2.11"), Some(1), Some("stars"))

  import environment._

  "page " must {

    "sources and parse" in {
      val sources = crawlerPage.sources(query).await()
      println(sources)
      val parse = crawlerPage.parse(sources).await()
      println(parse)
    }

  }

  "lib " must {
    val querys = crawlerPage.sources(query).flatMap(crawlerPage.parse).await().list
    "sources" in {
      querys.foreach(lib => {
        println(
          crawlerLib.sources(CrawlerLib.Query(lib)).flatMap(crawlerLib.parse).await()
        )
      })

    }


  }
}
