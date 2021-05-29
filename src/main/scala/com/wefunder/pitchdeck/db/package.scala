package com.wefunder.pitchdeck

import com.typesafe.scalalogging.LazyLogging
import doobie.util.log.{ ExecFailure, LogHandler, ProcessingFailure, Success }

import scala.concurrent.duration.FiniteDuration

package object db extends LazyLogging {
  val doobieLogHandler: LogHandler = LogHandler {
    case _: Success                         =>
    case ExecFailure(s, a, e1, t)           =>
      logger.error(s"""Failed Statement Execution:
                      |
                      |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                      |
                      | arguments = [${a.mkString(", ")}]
                      |   elapsed = ${e1.toMillis.toString} ms exec (failed)
                      |   failure = ${t.getMessage}
          """.stripMargin)

    case ProcessingFailure(s, a, e1, e2, t) =>
      logger.error(s"""Failed Resultset Processing:
                      |
                      |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                      |
                      | arguments = [${a.mkString(", ")}]
                      |   elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis.toString} ms processing (failed) (${(e1 + e2).toMillis.toString} ms total)
                      |   failure = ${t.getMessage}
          """.stripMargin)
  }
}
