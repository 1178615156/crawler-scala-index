package scalaindex

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * Created by yujieshui on 2016/11/14.
  */
case class CacheConfig(
                        repoxUrl: String,
                        withSources: Boolean,
                        withJavadoc: Boolean,
                        scalaVersion: List[String],
                        reTryNum :Int
                      )

object CrawlerConfig {

  def config: Config = ConfigFactory load() getConfig "crawler.scalaindex.config"

  lazy val q        : String = config.getString("q".trim)
  lazy val sort     : String = config.getString("sort".trim)
  lazy val pageStart: Int    = config.getInt("pageStart".trim)
  lazy val pageEnd  : Int    = config.getInt("pageEnd".trim)

  lazy val url         : Option[String]       = Try(config.getString("url")).toOption
  lazy val withSources : Option[Boolean]      = Try(config.getBoolean("withSources")).toOption
  lazy val withJavadoc : Option[Boolean]      = Try(config.getBoolean("withJavadoc")).toOption
  lazy val scalaVersion: Option[List[String]] = Try(config.getStringList("scalaVersion".trim).toList).toOption
  lazy val reTryNum = Try(config.getInt("reTryNum")).toOption
}
