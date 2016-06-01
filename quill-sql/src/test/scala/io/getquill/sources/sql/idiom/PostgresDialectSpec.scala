package io.getquill.sources.sql.idiom

import io.getquill.sources.sql.SqlSpec

class PostgresDialectSpec extends SqlSpec {

  "supports the `prepare` statement" in {
    val sql = "test"
    PostgresDialect.prepare(sql) mustEqual
      s"PREPARE p${PostgresDialect.preparedStatementId} AS $sql"
  }
}
