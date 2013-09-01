package superspec

import superspec.tt._
import mrsc.core._

trait CoreSubst extends CoreEval with CoreQuote {
  type Subst = Map[Name, Term]

  implicit class TermSubst(t: Term) {
    def /(subst: Subst) = {
      val env: NameEnv[Value] = subst.map {case (n, t) => (n, eval(t, emptyNEnv, Nil))}
      quote0(eval(t, env, Nil))
    }
  }
}

trait CoreDriver extends TTSc with CoreCheck {
  var v = 100
  def freshName(): Name = {
    v += 1
    Local(v)
  }

  // current ad-hoc solution for mapping variables and types of new free variables

  // logic
  override def driveTerm(c: Conf): DriveStep = eval0(c.term) match {
    case VNeutral(n) =>
      nv(n) match {
        case Some(n) =>
          elimVar(n, iType0(c.ctx, Free(n)))
        case None =>
          StopDStep
      }
    case _ => decompose(c)
  }

  // neutral variable of a value
  def nv(n: Neutral): Option[Name] =
    None

  def elimVar(n: Name, nt: Value): DriveStep = nt match {
    case _ => StopDStep
  }

  def decompose(c: Conf): DriveStep = StopDStep

  implicit def name2Term(n: Name) = Free(n)
}

trait CoreResiduator extends BaseResiduator with CoreDriver {
  import mrsc.core._

  override def fold(node: N, env: NameEnv[Value], bound: Env, recM: Map[TPath, Value]): Value =
    node.outs match {
      case _ =>
        super.fold(node, env, bound, recM)
    }
}

trait CoreProofResiduator extends ProofResiduator with CoreResiduator {

  override def proofFold(node: N,
                         env: NameEnv[Value], bound: Env, recM: Map[TPath, Value],
                         env2: NameEnv[Value], bound2: Env, recM2: Map[TPath, Value]): Value =
    node.outs match {
      case _ =>  super.proofFold(node, env, bound, recM, env2, bound2, recM2)
    }
}
