package scalaindex

import scalaindex.crawler.ScalaIndexCrawler.RootTask

/**
  * Created by yujieshui on 2016/12/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val urlR = "--url=(.*)".r
    val url = args.collectFirst {
      case urlR(e) => e
    }
    implicit lazy val environment = new ScalaIndexCrawlerEnvironment {
      val cacheConfig: CacheConfig = CacheConfig(
        repoxUrl = url.getOrElse(CrawlerConfig.url.get),
        withSources = CrawlerConfig.withSources.get,
        withJavadoc = CrawlerConfig.withJavadoc.get,
        scalaVersion = CrawlerConfig.scalaVersion.get,
        CrawlerConfig.reTryNum.getOrElse(1)
      )

      val rootTask   : RootTask    = RootTask(
        q = Some(CrawlerConfig.q),
        sort = Some(CrawlerConfig.sort),
        pageStart = Some(CrawlerConfig.pageStart),
        pageEnd = Some(CrawlerConfig.pageEnd)
      )
    }
    val service = new Service()
    service.run()
  }
}