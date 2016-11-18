package scalaindex.crawler

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

/**
  * Created by yujieshui on 2016/11/14.
  */
object CrawlerConfig {

  def config = ConfigFactory load() getConfig "crawler.scalaindex.config"

  val scalaVersion = config.getStringList("scalaVersion".trim).toList
  val q            = config.getString("q".trim)
  val sort         = config.getString("sort".trim)
  val pageStart    = config.getInt("pageStart".trim)
  val pageEnd      = config.getInt("pageEnd".trim)
  val withSources  = config.getBoolean("withSources")
  val withJavadoc  = config.getBoolean("withJavadoc")
}
