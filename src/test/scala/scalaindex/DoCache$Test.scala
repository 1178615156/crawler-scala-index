package scalaindex

import org.scalatest.{FunSuite, WordSpecLike}

import scalaindex.crawler.CrawlerLib
import DoCache._
import scalaj.http.Http

/**
  * Created by yujieshui on 2016/12/17.
  */
class DoCache$Test extends WordSpecLike {


  "func" must {
    val task = crawlerLibResult2doCacheTask(CrawlerLib.Result(List(""""org.scalatra" %% "scalatra" % "2.5.0"""")),List("2.11"))
    "crawlerLibResult2doCacheTask" in {
      println(task)
      assert(task === List(Task("org.scalatra", "scalatra", "2.5.0",Some("2.11"))))
    }
    "task2request" in {
      val r = task2request(task.head, "https://repo1.maven.org/maven2")
      assert(r === List(TaskRequest(
        "https://repo1.maven.org/maven2/org/scalatra/scalatra_2.11/2.5.0/scalatra_2.11-2.5.0.pom",
        "https://repo1.maven.org/maven2/org/scalatra/scalatra_2.11/2.5.0/scalatra_2.11-2.5.0.pom.sha1",
        "https://repo1.maven.org/maven2/org/scalatra/scalatra_2.11/2.5.0/scalatra_2.11-2.5.0.jar",
        "https://repo1.maven.org/maven2/org/scalatra/scalatra_2.11/2.5.0/scalatra_2.11-2.5.0.jar.sha1"
      )))
      println(r.mkString("\n"))
    }
    "pom2task" in {
      val pom = "https://repo1.maven.org/maven2/org/scalatra/scalatra_2.11/2.5.0/scalatra_2.11-2.5.0.pom"
      val r = pom2Task(      Http(pom).timeout(10000, 10000).asString.body)
      println(r.mkString("\n"))
    }
  }
}
