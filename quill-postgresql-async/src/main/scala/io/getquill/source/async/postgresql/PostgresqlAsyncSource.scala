package io.getquill.source.async.postgresql

import scala.annotation.implicitNotFound
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.RowData
import com.typesafe.scalalogging.StrictLogging

import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.MySQLDialect
import io.getquill.source.sql.naming.NamingStrategy

class PostgresqlAsyncSource[N <: NamingStrategy]
    extends SqlSource[MySQLDialect.type, N, RowData, List[Any]]
    with PostgresqlAsyncDecoders
    with PostgresqlAsyncEncoders
    with StrictLogging {

  protected val pool = PostgresqlAsyncPool(config)

  private def withConnection[T](f: Connection => Future[T])(implicit ec: ExecutionContext) =
    ec match {
      case TransactionalExecutionContext(ec, conn) => f(conn)
      case other                                   => f(pool)
    }

  def probe(sql: String) =
    Try {
      val query =
        if (sql.startsWith("PREPARE"))
          pool.sendQuery(sql)
        else
          pool.sendQuery(s"PREPARE $sql")
      Await.result(query, Duration.Inf)
    }

  def transaction[T](f: ExecutionContext => Future[T])(implicit ec: ExecutionContext) =
    pool.inTransaction { c =>
      f(TransactionalExecutionContext(ec, c))
    }

  def execute(sql: String)(implicit ec: ExecutionContext) = {
    logger.info(sql)
    withConnection(_.sendQuery(sql))
  }

  def execute(sql: String, bindList: List[List[Any] => List[Any]])(implicit ec: ExecutionContext): Future[List[QueryResult]] =
    bindList match {
      case Nil =>
        Future.successful(List())
      case bind :: tail =>
        logger.info(sql)
        withConnection(_.sendPreparedStatement(sql, bind(List())))
          .flatMap(_ => execute(sql, tail))
    }

  def query[T](sql: String, bind: List[Any] => List[Any], extractor: RowData => T)(implicit ec: ExecutionContext) = {
    withConnection(_.sendPreparedStatement(sql, bind(List()))).map {
      _.rows match {
        case Some(rows) => rows.map(extractor)
        case None       => List()
      }
    }
  }
}