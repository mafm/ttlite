package superspec.tt

object CoreREPLMain extends CoreREPL {
  override def initialState =
    State(true, emptyNEnv, emptyNEnv, Set())
}

object NatREPLMain extends NatREPL {
  override def initialState =
    State(interactive = true, natVE, natTE, Set())
}

object ProductREPLMain extends ProductREPL {
  override def initialState =
    State(interactive = true, productVE, productTE, Set())
}

object SumREPLMain extends SumREPL {
  override def initialState =
    State(interactive = true, sumVE, sumTE, Set())
}

/*
object REPLMain extends CoreREPL with NatREPL with VectorREPL with EqREPL with FinREPL with ListREPL with ProductREPL with SumREPL {
  val te = natTE ++ eqTE ++ vectorTE ++ finTE ++ listTE ++ productTE ++ sumTE
  val ve = natVE ++ eqVE ++ vectorVE ++ finVE ++ listVE ++ productVE ++ sumVE
  override def initialState = State(interactive = true, ve, te, Set())
}
*/
