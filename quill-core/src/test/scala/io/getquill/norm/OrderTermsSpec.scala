package io.getquill.norm

import io.getquill.SourceSpec
import io.getquill.sources.mirror.mirrorSource
import io.getquill.sources.mirror.mirrorSource._

class OrderTermsSpec extends SourceSpec(mirrorSource) {

  "doesn't reorder groupBy.map" in {
    val q = quote {
      qr1.map(b => b.s).sortBy(b => b)
    }
    OrderTerms.unapply(q.ast) mustEqual None
  }

  "sortBy" - {
    "a.sortBy(b => c).filter(d => e)" in {
      val q = quote {
        qr1.sortBy(b => b.s).filter(d => d.s == "s1")
      }
      val n = quote {
        qr1.filter(d => d.s == "s1").sortBy(b => b.s)
      }
      OrderTerms.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

  "take" - {
    "a.map(b => c).take(d)" in {
      val q = quote {
        qr1.map(b => b.s).take(10)
      }
      val n = quote {
        qr1.take(10).map(b => b.s)
      }
      OrderTerms.unapply(q.ast) mustEqual Some(n.ast)
    }
  }

}
