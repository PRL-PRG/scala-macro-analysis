package cz.cvut.fit.prl.scalamacros.tools

import java.io.{File, FileWriter}

import com.typesafe.scalalogging.LazyLogging
import purecsv.safe._
import resource._

import scala.meta._
import scala.util.{Failure, Success, Try}


object MacroDefAnalysis extends App with LazyLogging {
  val NL = sys.props("line.separator")

  case class OutputRecord(relativePath: String, nDefMacros: Int, error: String)

  println(args.toList)
  if (args.length != 1) {
    sys.error(s"Usage: MacroDefAnalysis <path/to/project>")
  }

  val projectPath = args(0)
  val input = s"$projectPath/_analysis_/unique-files.txt"
  val output = s"$projectPath/_analysis_/def-macros-output.csv"

  if (!new File(input).canRead) {
    println(s"$input: does not exists or is not readable - skipping")
    sys.exit(0)
  }

  def analyze(path: String): Try[Int] = Try {
    val source = new File(s"$projectPath/$path").parse[Source].get
    val defs = source.collect { case x: Defn.Macro => x }
    val n = defs.size

    logger.info(s"Processed $path: $n")

    n
  }

  val csv =
    scala.io.Source
      .fromFile(input)
      .getLines()
      .map { x =>
        analyze(x) match {
          case Success(n) => OutputRecord(x, n, "")
          case Failure(e) => OutputRecord(x, -1, e.getMessage)
        }
      }
      .map(_.toCSV())
      .mkString("", NL, NL)

  managed(new FileWriter(output)).foreach(_.write(csv))
}
