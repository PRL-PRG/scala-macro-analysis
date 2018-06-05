package cz.cvut.fit.prl.scalamacros.tools

import java.io.{File, FileWriter}

import com.typesafe.scalalogging.LazyLogging
import purecsv.safe._
import resource._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.meta._
import scala.util.{Success, Try}


object MacroDefAnalysis extends App with LazyLogging {

  val NL = sys.props("line.separator")
  val ProjectPath = "/var/lib/scala/projects"
  val Input = "/var/lib/scala/analysis/files.csv"
  val Output = "/var/lib/scala/analysis/def-macros.csv"

  case class InputRecord(fileId: Int, projectId: Int, relativeUrl: String, fileHash: String)
  case class OutputRecord(projectId: Int, relativePath: String, nDefMacros: Int)

  def analyze(rec: InputRecord): Try[OutputRecord] = Try {
    val path = s"$ProjectPath/${rec.projectId}/${rec.relativeUrl}"
    val source = new File(path).parse[Source].get
    val defs = source.collect { case x : Defn.Macro => x}
    val n = defs.size

    logger.info(s"Processed $path: $n")

    OutputRecord(rec.projectId, rec.relativeUrl, n)
  }

  val uniqueFiles =
    scala.io.Source
      .fromFile(Input)
      .getLines()
      .drop(1)
      .map(x => CSVReader[InputRecord].readCSVFromString(x).head)

  val process = Future.traverse(uniqueFiles)(x => Future(x.flatMap(analyze)))
  val result = Await.result(process, Duration.Inf)

  for {
    out <- managed(new FileWriter(Output))
    res <- result.collect { case Success(x) => x } if res.nDefMacros > 0
  } {
    out.write(res.toCSV() + NL)
  }
}
