package crawler

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yujieshui on 2016/11/13.
  */


trait Crawler {
  type Url = String

  type Query
  type Sources
  type Parse
  type Result = Parse

  def sources(query: Query): Future[Sources]

  def parse(sources: Sources): Future[Parse]

  def crawler(query: Query): Future[Result] = sources(query).flatMap(parse)
}

trait CrawlerAnalysis {
  self: Crawler =>
  type AnalysisResult

  def analysis(parseResult: Parse): Future[AnalysisResult]

}
