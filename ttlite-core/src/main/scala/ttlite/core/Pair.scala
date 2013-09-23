package ttlite.core

trait PairAST extends CoreAST {
  case class Product(A: Term, B: Term) extends Term
  case class Pair(et: Term, a: Term, b: Term) extends Term
  case class ProductElim(et: Term, m: Term, f: Term, pair: Term) extends Term

  case class VProduct(A: Value, B: Value) extends Value
  case class VPair(et: Value, a: Value, b: Value) extends Value
  case class NProductElim(et: Value, m: Value, f: Value, pair: Neutral) extends Neutral
}

trait PairMetaSyntax extends CoreMetaSyntax with PairAST {
  override def fromM(m: MTerm): Term = m match {
    case MVar(Global("Product")) @@ a @@ b =>
      Product(fromM(a), fromM(b))
    case MVar(Global("Pair")) @@ et @@ x @@ y =>
      Pair(fromM(et), fromM(x), fromM(y))
    case MVar(Global("elim")) @@ (MVar(Global("Product")) @@ a @@ b) @@ m @@ f @@ p =>
      ProductElim(Product(fromM(a), fromM(b)), fromM(m), fromM(f), fromM(p))
    case _ => super.fromM(m)
  }
}

trait PairPrinter extends FunPrinter with PairAST {
  override def print(p: Int, ii: Int, t: Term): Doc = t match {
    case Product(a, b) =>
      print(p, ii, 'Product @@ a @@ b)
    case Pair(et, x, y) =>
      print(p, ii, 'Pair @@ et @@ x @@ y)
    case ProductElim(et, m, f, pair) =>
      print(p, ii, 'elim @@ et @@ m @@ f @@ pair)
    case _ =>
      super.print(p, ii, t)
  }
}

trait PairEval extends FunEval with PairAST {
  override def eval(t: Term, ctx: Context[Value], bound: Env): Value = t match {
    case Product(a, b) =>
      VProduct(eval(a, ctx, bound), eval(b, ctx, bound))
    case Pair(et, x, y) =>
      VPair(eval(et, ctx, bound), eval(x, ctx, bound), eval(y, ctx, bound))
    case ProductElim(et, m, f, pair) =>
      val etVal = eval(et, ctx, bound)
      val mVal = eval(m, ctx, bound)
      val fVal = eval(f, ctx, bound)
      val pVal = eval(pair, ctx, bound)
      productElim(etVal, mVal, fVal, pVal)
    case _ =>
      super.eval(t, ctx, bound)
  }

  def productElim(etVal: Value, mVal: Value, fVal: Value, pVal: Value) =
    pVal match {
      case VPair(_, x, y) =>
        fVal @@ x @@ y
      case VNeutral(n) =>
        VNeutral(NProductElim(etVal, mVal, fVal, n))
    }
}

trait PairCheck extends FunCheck with PairAST {
  override def iType(i: Int, ctx: Context[Value], t: Term): Value = t match {
    case Product(a, b) =>
      val aType = iType(i, ctx, a)
      val j = checkUniverse(i, aType)

      val bType = iType(i, ctx, b)
      val k = checkUniverse(i, bType)

      VUniverse(math.max(j, k))
    case Pair(et, x, y) =>
      val eType = iType(i, ctx, et)
      checkUniverse(i, eType)

      val VProduct(aVal, bVal) = eval(et, ctx, List())

      val xType = iType(i, ctx, x)
      checkEqual(i, xType, aVal)

      val yType = iType(i, ctx, y)
      checkEqual(i, yType, bVal)

      VProduct(aVal, bVal)
    case ProductElim(et, m, f, p) =>
      val eType = iType(i, ctx, et)
      checkUniverse(i, eType)

      val VProduct(aVal, bVal) = eval(et, ctx, List())

      val mVal = eval(m, ctx, List())
      val pVal = eval(p, ctx, List())

      val pType = iType(i, ctx, p)
      checkEqual(i, pType, VProduct(aVal, bVal))

      val mType = iType(i, ctx, m)
      checkEqual(i, mType, VPi(VProduct(aVal, bVal), {_ => VUniverse(-1)}))

      val fType = iType(i, ctx, f)
      checkEqual(i, fType, VPi(aVal, a => VPi(bVal, b => mVal @@ VPair(VProduct(aVal, bVal), a, b))))

      mVal @@ pVal
    case _ =>
      super.iType(i, ctx, t)
  }

  override def iSubst(i: Int, r: Term, it: Term): Term = it match {
    case Product(a, b) =>
      Product(iSubst(i, r, a), iSubst(i, r, b))
    case Pair(et, x, y) =>
      Pair(iSubst(i, r, et), iSubst(i, r, x), iSubst(i, r, y))
    case ProductElim(et, m, f, p) =>
      ProductElim(iSubst(i, r, et), iSubst(i, r, m), iSubst(i, r, f), iSubst(i, r, p))
    case _ => super.iSubst(i, r, it)
  }
}

trait PairQuote extends CoreQuote with PairAST {
  override def quote(ii: Int, v: Value): Term = v match {
    case VProduct(a, b) =>
      Product(quote(ii, a), quote(ii, b))
    case VPair(et, x, y) =>
      Pair(quote(ii, et), quote(ii, x), quote(ii, y))
    case _ => super.quote(ii, v)
  }

  override def neutralQuote(ii: Int, n: Neutral): Term = n match {
    case NProductElim(et, m, f, p) =>
      ProductElim(quote(ii, et), quote(ii, m), quote(ii, f), neutralQuote(ii, p))
    case _ => super.neutralQuote(ii, n)
  }
}

trait PairREPL extends CoreREPL with PairAST with PairMetaSyntax with PairPrinter with PairCheck with PairEval with PairQuote