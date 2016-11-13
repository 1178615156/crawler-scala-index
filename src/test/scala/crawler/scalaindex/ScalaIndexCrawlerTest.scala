package crawler.scalaindex

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import crawler.scalaindex.ScalaIndexCrawler._
import org.scalatest.{FunSuite, WordSpecLike}
import akka.pattern._
import scala.concurrent.duration._

/**
  * Created by yujieshui on 2016/11/13.
  */
class ScalaIndexCrawlerTest extends WordSpecLike with TestResources {
  implicit val timeout = Timeout(10.second)
  val rootTask   : RootTask    = RootTask(Some("targets:scala_2.11"), Some("stars"), Some(1), Some(1))
  val crawlerPage: CrawlerPage = new CrawlerPage(wsClient)
  val crawlerLib : CrawlerLib  = new CrawlerLib(wsClient)
  Thread.sleep(1000)
  "persist message" must {
    lazy val testKit = new TestKit(ActorSystem("test")) with ImplicitSender
    import testKit._
    lazy val scalaIndexCrawler = system.actorOf(Props(new ScalaIndexCrawler(
      rootTask, crawlerPage, crawlerLib,testActor
    )))
    "send doRun" in {

      val pages = (scalaIndexCrawler ask DoRun() await()).asInstanceOf[Seq[_]]
      assert(pages.size === 1)
    }
    "sleep" in {
      Thread.sleep(10000)
    }
    "send getResult" in {
      val x = scalaIndexCrawler.ask(GetResult()).await().asInstanceOf[Seq[_]]
      println(x)
    }
    "stop all" in {
      system.terminate().await()
    }
  }
  "restart system" must {
    lazy val testKit = new TestKit(ActorSystem("test")) with ImplicitSender
    import testKit._
    lazy val scalaIndexCrawler = system.actorOf(Props(new ScalaIndexCrawler(
      rootTask, crawlerPage, crawlerLib,testActor
    )))

    "get result" in {
      println(
        scalaIndexCrawler.ask(GetResult()).await().asInstanceOf[Seq[_]].mkString("\n")
      )
    }
    "sleep" in {
      Thread.sleep(10000)
    }
    "get result 2 " in {
      println(
        scalaIndexCrawler.ask(GetResult()).await().asInstanceOf[Seq[_]].mkString("\n")
      )
    }
  }
}
