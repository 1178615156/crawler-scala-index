package crawler

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by yujieshui on 2016/11/13.
  */



trait Crawler {
  type Url = String

  type Query
  type SourcesResult
  type ParseResult

  def sources(query: Query): Future[SourcesResult]

  def parse(sources: SourcesResult): Future[ParseResult]

  def crawler(query: Query): Future[ParseResult] = sources(query).flatMap(parse)
}

trait CrawlerAnalysis{
  self :Crawler =>
  type AnalysisResult
  def analysis(parseResult: ParseResult): Future[AnalysisResult]

}
