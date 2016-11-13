package crawler.scalaindex

/**
  * Created by yujieshui on 2016/11/13.
  */

trait ScalaIndexCrawlerEnvironment {
  val index_scala_host = "https://index.scala-lang.org"
  implicit val executionContext =
    scala.concurrent.ExecutionContext.fromExecutor(java.util.concurrent.Executors.newFixedThreadPool(1))

}
