package scalaindex

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._
import scalaindex.crawler.ScalaIndexCrawler.RootTask

/**
  * Created by yujieshui on 2016/11/13.
  */

trait ScalaIndexCrawlerEnvironment {
  val index_scala_host = "https://index.scala-lang.org"
  implicit      val executionContext   =
    scala.concurrent.ExecutionContext.fromExecutor(java.util.concurrent.Executors.newFixedThreadPool(1))
  implicit lazy val actorSystem        = ActorSystem("scalaIndexCrawler")
  implicit lazy val materializer       = ActorMaterializer()
  implicit lazy val wsClient: WSClient = AhcWSClient()
  implicit      val timeout            = Timeout(10.second)

  def cacheConfig: CacheConfig
  def rootTask : RootTask
}
