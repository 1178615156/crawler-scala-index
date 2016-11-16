package scalaindex.crawler

import com.typesafe.config.ConfigFactory

/**
  * Created by yujieshui on 2016/11/14.
  */
object CrawlerScalaIndexConfig {

  def config = ConfigFactory load() getConfig "crawler.scalaindex.config"

  val scalaVersion = Seq("2.11.8", "2.12.0")
  val q            = "targets:scala_2.11"
  val sort         = "starts"
  val pageStart    = 1
  val pageEnd      = 20
}
