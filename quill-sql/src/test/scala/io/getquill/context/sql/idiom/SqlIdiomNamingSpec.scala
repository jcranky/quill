package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.SqlMirrorContext
import io.getquill.Escape
import io.getquill.SnakeCase
import io.getquill.UpperCase
import io.getquill.MirrorSqlDialect
import io.getquill.SqlMirrorContext

trait CustomTableStrategy extends SnakeCase {
  override def table(s: String) = s"t_$s".toLowerCase
}
object CustomTableStrategy extends CustomTableStrategy

trait CustomColumnStrategy extends SnakeCase {
  override def column(s: String) = s"c_$s".toLowerCase
}
object CustomColumnStrategy extends CustomColumnStrategy

class SqlIdiomNamingSpec extends Spec {

  "uses the naming strategy" - {

    case class SomeEntity(someColumn: Int)

    "one transformation" in {
      val db = new SqlMirrorContext[MirrorSqlDialect, SnakeCase]
      import db._
      db.run(query[SomeEntity]).string mustEqual
        "SELECT x.some_column FROM some_entity x"
    }
    "mutiple transformations" in {
      val db = new SqlMirrorContext[MirrorSqlDialect, SnakeCase with UpperCase with Escape]
      import db._
      db.run(query[SomeEntity]).string mustEqual
        """SELECT "X"."SOME_COLUMN" FROM "SOME_ENTITY" "X""""
    }
    "specific table strategy" in {
      val db = new SqlMirrorContext[MirrorSqlDialect, CustomTableStrategy]
      import db._

      val q = quote {
        query[SomeEntity].map(t => t.someColumn)
      }

      db.run(q.dynamic).string mustEqual
        "SELECT t.some_column FROM t_someentity t"
    }
    "specific column strategy" in {
      val db = new SqlMirrorContext[MirrorSqlDialect, CustomColumnStrategy]
      import db._

      val q = quote {
        query[SomeEntity].map(t => t.someColumn)
      }

      db.run(q.dynamic).string mustEqual
        "SELECT t.c_somecolumn FROM some_entity t"
    }

    val db = new SqlMirrorContext[MirrorSqlDialect, SnakeCase]

    import db._

    "actions" - {
      "insert" in {
        db.run(query[SomeEntity].insert(lift(SomeEntity(1)))).string mustEqual
          "INSERT INTO some_entity (some_column) VALUES (?)"
      }
      "update" in {
        db.run(query[SomeEntity].update(lift(SomeEntity(1)))).string mustEqual
          "UPDATE some_entity SET some_column = ?"
      }
      "delete" in {
        db.run(query[SomeEntity].delete).string mustEqual
          "DELETE FROM some_entity"
      }
    }

    "queries" - {
      "property empty check" in {
        case class SomeEntity(optionValue: Option[Int])
        db.run(query[SomeEntity].filter(t => t.optionValue.isEmpty)).string mustEqual
          "SELECT t.option_value FROM some_entity t WHERE t.option_value IS NULL"
      }
    }
  }
}
