package superspec.tt

trait Command
case class TypeOf(in: String) extends Command
case class Compile(cf: CompileForm) extends Command
case class Reload(f: String) extends Command
case object Browse extends Command
case object Quit extends Command
case object Help extends Command
case object Noop extends Command

trait CompileForm
case class CompileInteractive(s: String) extends CompileForm
case class CompileFile(f: String) extends CompileForm
case class Cmd(cs: List[String], argDesc: String, f: String => Command, info: String)

trait MREPL {

  // TO OVERRIDE STARTS
  type T // term
  type V // value
  type Result[A] = Either[String, A]

  private var batch: Boolean = false
  val prompt: String
  def itype(ne: NameEnv[V], ctx: NameEnv[V], i: T): Result[V]
  def iquote(v: V): T
  def ieval(ne: NameEnv[V], i: T): V
  def icprint(c: T): String
  def itprint(t: V): String
  def assume(s: State, x: (String, T)): State
  def fromM(m: MTerm): T
  val parser: MetaParser = MetaParser
  // TO OVERRIDE ENDS

  case class State(interactive: Boolean, ne: NameEnv[V], ctx: NameEnv[V], modules: Set[String])

  def handleError(msg: String): Unit =
    if (batch) throw new Exception(msg)

  def output(x: Any): Unit =
    if (!batch) Console.println(x)

  def iinfer(ne: NameEnv[V], ctx: NameEnv[V], i: T): Option[V] =
    itype(ne, ctx, i) match {
      case Right(t) =>
        Some(t)
      case Left(msg) =>
        Console.println(msg)
        None
    }

  def handleStmt(state: State, stmt: Stmt[MTerm]): State =
    stmt match {
      case Assume(ass) =>
        val ass1 = ass.map{case (n, m) => (n, fromM(m))}
        ass1.foldLeft(state)(assume)
      case Let(x, e) =>
        val e1 = fromM(e)
        handleLet(state, x, e1)
      case Eval(e) =>
        val e1 = fromM(e)
        handleLet(state, "it", e1)
      case Import(f) =>
        loadModule(f, state, reload = false)
    }

  def handleLet(state: State, s: String, it: T): State =
    iinfer(state.ne, state.ctx, it) match {
      case None =>
        handleError(s"Not Inferred type for $it")
        state
      case Some(tp) =>
        val v = ieval(state.ne, it)
        if (s == "it"){
          output(icprint(iquote(v)) + "\n::\n" + itprint(tp) + ";")
        } else {
          output(s"$s \n::\n ${itprint(tp)};")
        }
        State(state.interactive, state.ne + (Global(s) -> v),  state.ctx + (Global(s) -> tp), state.modules)
    }

  private def loadModule(f: String, state: State, reload: Boolean): State =
    if (state.modules(f) && !reload)
      return state
    else
      try {
        val input = scala.io.Source.fromFile(f).mkString
        val parsed = parser.parseIO(parser.stmt+, input)
        parsed match {
          case Some(stmts) =>
            val s1 = stmts.foldLeft(state){(s, stm) => handleStmt(s, stm)}
            s1.copy(modules = s1.modules + f)
          case None =>
            handleError("cannot parse")
            state
        }
      } catch {
        case io: java.io.IOException =>
          handleError("cannot open file")
          Console.println(s"Error: ${io.getMessage}")
          state
      }

  def main(args: Array[String]) {
    var state = State(true, Map(), Map(), Set())
    args match {
      case Array("-i", f) =>
        state = loadModule(f, state, reload = false)
      case _ =>
        batch = true
        args.foreach { f =>
          state = loadModule(f, state, reload = false)
        }
    }
  }

}
