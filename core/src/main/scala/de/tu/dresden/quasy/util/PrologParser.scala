package de.tu.dresden.quasy.util

import scala.util.parsing.combinator._
import scala.collection.mutable._
import prolog.io.IO
import scala.io.Source

//import scala.Console
import prolog.terms._
import prolog.builtins._

class PrologParser(val vars: LinkedHashMap[String, Var])
    extends RegexParsers {
    def this() = this(new LinkedHashMap[String, Var])

    def mkVar(x0: String) = {
        val x = if ("_" == x0) x0 + x0 + vars.size else x0
        vars.getOrElseUpdate(x, new Var())
    }

    def trimQuotes(q: String) = q.substring(1, q.length() - 1)

    protected override val whiteSpace = """(\s|%.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

    val varToken: Parser[String] = """[A-Z_]\w*""".r
    val realToken: Parser[String] = """-?(\d+)\.\d*""".r
    val numToken: Parser[String] = """-?(\d+)""".r
    val symToken: Parser[String] = """[^,()\[\]]+|[()\[\]]""".r
    val bracketToken:Parser[String] = """\[[^\[\]]*\]""".r

    //val specToken: Parser[String] = """[!\+\-\*/>=<]|\\\+|\[\]""".r
    val specToken: Parser[String] = """[!]|\\\+|\[\]""".r
    val quotedToken: Parser[String] = """\'[^']*\'""".r
    val stringToken: Parser[String] = """[^"]*""".r
    val eocToken: Parser[String] = """\.[\n\r\f\t]*""".r

    val dcgTopToken: Parser[String] = """-->""".r
    val topToken = """:-""".r
    val clauseTok = topToken | dcgTopToken
    val disjTok = """;""".r
    val implTok = """->""".r
    val conjTok = """,""".r
    val isTok = """is|==|\\==|=\\=|@=<|@>=|@<|@>|=>|<=|=\.\.|>=|=<|=:=|=|>|<""".r
    val plusTok = """\+|\-""".r
    val timesTok = """\*|/\\|\\/|//|/|mod|div|:|<<|>>|\^""".r

    def idToken: Parser[String] =
        ( (symToken ~ bracketToken) ^^ {
            case a~b =>
                a+b
        } ) |
            (quotedToken ^^ { trimQuotes } | specToken | symToken
        ) ^^ { x => x.replace("\\n", "\n") }

    def prog: Parser[List[List[Term]]] = (cmd | clause)*

    def cmd: Parser[List[Term]] = (topToken ~> conj <~ eocToken) ^^ mk_cmd

    def mk_cmd(bs: List[Term]) = {
        /*
        val f = new Fun(":-")
        f.args = new Array[Term](1)
        val c = TermParser.postProcessBody(Const.nil, bs)
        f.args(0) = Conj.fromList(c.tail)
        List(f)
        */
        PrologParser.postProcessBody(Const.cmd, bs)
    }

    def clause: Parser[List[Term]] = (head ~ opt(body) <~ eocToken) ^^
        {
            case h ~ None => List(h)
            case h ~ Some(bs) => PrologParser.postProcessBody(h, bs)
        }

    def head: Parser[Term] = term
    def body: Parser[List[Term]] = topToken ~> conj |
        ((dcgTopToken ~> conj) ^^ { xs => PrologParser.DCG_MARKER :: xs })

    def mkTerm(t: Term ~ Option[String ~ Term]): Term = t match {
        case x ~ None => x
        case x ~ Some(op ~ y) => {
            val t = new Fun(op)
            val xy = Array[Term](x, y)
            PrologParser.toFunBuiltin(new Fun(op, xy))
        }
    }

    def conj: Parser[List[Term]] = repsep(term, conjTok)

    def term: Parser[Term] = isTerm

    def clauseTerm: Parser[Term] =
        disjTerm ~ opt(clauseTok ~ disjTerm) ^^ mkTerm

    def disjTerm: Parser[Term] =
        repsep(implTerm, disjTok) ^^ (Disj.fromList)

    def implTerm: Parser[Term] =
        conjTerm ~ opt(implTok ~ conjTerm) ^^ mkTerm

    def conjTerm: Parser[Term] =
        repsep(isTerm, conjTok) ^^ (Conj.fromList)

    def isTerm: Parser[Term] =
        plusTerm ~ opt(isTok ~ plusTerm) ^^ mkTerm

    def plusTerm: Parser[Term] =
        timesTerm ~ opt(plusTok ~ timesTerm) ^^ mkTerm

    def timesTerm: Parser[Term] =
        parTerm ~ opt(timesTok ~ parTerm) ^^ mkTerm

    def parTerm: Parser[Term] = "(" ~> clauseTerm <~ ")" | dcg_escape | plainTerm

    def dcg_escape: Parser[Term] = "{" ~> disjTerm <~ "}" ^^
        { x =>
            val f = new Fun("{}")
            val xs:Array[Term] = Array(x)
            f.args = xs
            f
        }

    def plainTerm: Parser[Term] = listTerm | funTerm | stringTerm // this order is important for "[]"

    def funTerm: Parser[Term] =
        varToken ^^ mkVar |||
            (idToken ~ opt(args)) ^^
                {
                    case x ~ None => {
                        val b = PrologParser.string2ConstBuiltin(x)
                        if (!b.eq(null)) b
                        else new Const(x)
                    }
                    case x ~ Some(xs) => {
                        if(x.endsWith(" ")) {
                            val res: String = x + "(" + xs.map(_.toString).mkString(",") + ")"
                            val b = PrologParser.string2ConstBuiltin(res)
                            if (!b.eq(null)) b
                            else new Const(res)
                        }
                        else {
                            val l = xs.length
                            val array = new Array[Term](l)
                            xs.copyToArray(array)
                            if (l == 2 && x == ".")
                                new Cons(xs(0), xs(1))
                            else
                                PrologParser.toFunBuiltin(new Fun(x, array))
                        }
                    }
                }  |||
            realToken ^^
                { x => new Real(x) } |||
            numToken ^^
                { x => new SmallInt(x) }

    def stringTerm: Parser[Term] = "\"" ~> stringToken <~ "\"" ^^ { x =>
    //println("here=" + x)
        Cons.fromList(x.toList.map { i => SmallInt(i) })
    }

    def listTerm: Parser[Term] =
        listArgs ^^
            {
                case (xs ~ None) =>
                    PrologParser.list2cons(xs.asInstanceOf[List[Term]], Const.nil)
                case (xs ~ Some(x)) =>
                    PrologParser.list2cons(
                        xs.asInstanceOf[List[Term]],
                        x.asInstanceOf[Term])
            }

    def listArgs: Parser[List[Term] ~ Option[Term]] =
        "[" ~> (repsep(term, ",") ~ opt("|" ~> term)) <~ "]"

    def args: Parser[List[Term]] = "(" ~> repsep(term,",") <~ ")"


    def parse(s: String) = {
        val text = if (s.trim.endsWith(".")) s else s + ". "
        val termList = parseAll(clause, text) match {
            case Success(xss, _) => xss
            case other => {
                IO.warnmes("syntax error: " + other)
                Nil
            }
        }
        termList
    }

    def parseProg(text: String): List[List[Term]] = {
        val clauseList = parseAll(prog, text) match {
            case Success(xss, _) => xss
            case other => {
                IO.warnmes("syntax error: " + other)
                Nil
            }
        }
        clauseList
    }

    def file2clauses(fname: String): List[List[Term]] = {
        if ("stdio" == fname) {
            List(parse("true :- " + IO.readLine("> ")))
        } else parseProg(scala.io.Source.fromFile(fname, "utf-8").mkString)
    }

    def readGoal() = {
        val s = IO.readLine("?- ")
        if (s.eq(null)) null
        else {
            vars.clear()
            val t = parse("true :- " + s)
            (t, vars)
        }
    }
}

object PrologParser {

    type VMAP = LinkedHashMap[Var, String]

    val builtinMap = new HashMap[String, Const]()
    builtinMap.put("true", true_())
    builtinMap.put("fail", fail_())
    builtinMap.put("=", new eq())

    private def fun2special(t: Fun): Fun = {
        if (2 == t.args.length) t.name match {
            case "," => new Conj(t.getArg(0), t.getArg(1))
            case "." => new Cons(t.getArg(0), t.getArg(1))
            case ":-" => new Clause(t.getArg(0), t.getArg(1))
            case ";" => new Disj(t.getArg(0), t.getArg(1))

            case _: String => t
        }
        else if (1 == t.args.length) t.name match {
            case "return" => new Answer(t.getArg(0))
            case _: String => t
        }
        else
            t
    }

    private def string2builtin(s: String): Const = {

        val res = builtinMap.get(s) match {
            case None => {

                try {
                    val bclass = Class.forName("prolog.builtins." + s)

                    try {

                        val b = bclass.newInstance

                        if (b.isInstanceOf[FunBuiltin] || b.isInstanceOf[ConstBuiltin]) {
                            val c = b.asInstanceOf[Const]
                            builtinMap.put(s, c)
                            c
                        } else {
                            println("unexpected prolog.builtins class =>" + s)
                            null
                        }
                    } catch {
                        case e => {
                            println("unexpected builtin creation failure =>" + s + "=>" + e)
                            null
                        }
                    }
                } catch {
                    case _ => {
                        //println("expected builtin creation failure =>" + s)
                        null
                    }
                }
            }
            case Some(b) => b
        }
        res
    }

    def toConstBuiltin(c: Const): Const = {
        val t = string2ConstBuiltin(c.name)
        if (t.eq(null)) c else t
    }

    def string2ConstBuiltin(s: String): ConstBuiltin =
        string2builtin(s: String) match {
            case null => null
            case b: ConstBuiltin => b
            case _ => null
        }

    def string2FunBuiltin(s: String): FunBuiltin =
        string2builtin(s: String) match {
            case null => null
            case b: FunBuiltin => b
            case _ => null
        }

    def toFunBuiltin(f: Fun): Fun = {
        val proto: FunBuiltin = string2FunBuiltin(f.sym)

        val res =
            if (proto.eq(null)) fun2special(f)
            else if (f.args.length != proto.arity) f
            else {
                val b = proto.funClone
                b.args = f.args
                b
            }
        res
    }

    def toQuoted(s: String) = {
        val c =
            if (s.eq(null) || s.length == 0) ' '
            else s.charAt(0)
        if (c >= 'a' && c <= 'z') s
        else "'" + s + "'"
    }
    def printclause(rvars: VMAP, xs: List[Term]) =
        IO.println(clause2string(rvars, xs))

    def term2string(t: Term): String =
        term2string(new VMAP(), List(t), "")

    def clause2string(rvars: VMAP,
                      xs: List[Term]): String =
        term2string(rvars, xs, ".")

    def term2string(rvars: VMAP,
                    xs: List[Term], end: String): String = {
        val buf = new StringBuilder()

        def pprint(t: Term): Unit = {

            t.ref match {
                case v: Var => {
                    val s_opt = rvars.get(v)
                    s_opt match {
                        case None => buf ++= v.toString
                        case Some(s) => buf ++= s
                    }
                }
                case _: cut => buf ++= "!"
                case _: neck => buf ++= "true"
                case q: eq => {
                    pprint(q.getArg(0))
                    buf ++= "="
                    pprint(q.getArg(1))
                }
                case l: Cons =>
                    buf ++= Cons.to_string({ x => term2string(rvars, List(x), "") }, l)
                case f: Fun => {
                    val s = toQuoted(f.name)
                    buf ++= s
                    buf ++= "("
                    var first = true
                    if (f.args.eq(null)) buf ++= "...null..."
                    else
                        for (x <- f.args) {
                            if (first) first = false else buf ++= ","
                            pprint(x)
                        }
                    buf ++= ")"
                }
                case c: Const => {
                    val s = toQuoted(c.name)
                    buf ++= s
                }
                case other: Term => buf ++= other.toString
            }

        }

        pprint(xs.head)
        val bs = xs.tail
        if (!bs.isEmpty) {
            buf ++= ":-\n  "
            val ys = if (!bs.isEmpty && bs.head.ref.isInstanceOf[neck]) bs.tail else bs
            pprint(ys.head)
            ys.tail.foreach { x => { buf ++= ",\n  "; pprint(x) } }
        }
        buf ++= end
        buf.toString
    }

    def list2cons(xs: List[Term], z: Term): Term = xs match {
        case Nil => z
        case y :: ys => new Cons(y, list2cons(ys, z))
    }

    /*
     * transforms the body - cut, for now etc.
     */

    def postProcessBody(h: Term, xs: List[Term]): List[Term] = {
        var hasCut = false
        val V = new Var()

        def fixSpecial(t: Term): Term = t match {
            case f: Fun => {
                val g = f.funClone
                val newargs: Array[Term] = f.args.map(fixSpecial)
                g.args = newargs
                g
            }
            case x: Const =>
                x.sym match {
                    case "!" => {
                        hasCut = true
                        cut(V)
                    }
                    case other => x
                }
            case other => other
        }

        val ys = xs.map(fixSpecial)
        val (e, es) = ys match {
            case DCG_MARKER :: bs =>
                dcgExpand(h, bs)
            case _ => (h, ys)
        }
        if (hasCut) e :: neck(V) :: es else e :: es
    }

    val DCG_MARKER = new Const("$dcg_body")

    //def dcgExpand(a: Term, b: List[Term]) = (a, b)

    def dcgExpand(a: Term, bs: List[Term]): (Term, List[Term]) = {
        val V1 = new Var()
        val V2 = new Var()
        val hd = dcg_goal(a, V1, V2)
        val cs = dcg_body(bs, V1, V2)
        (hd, cs)
    }

    def dcg_goal(g0: Term, S1: Var, S2: Var): Term = g0 match {
        case c: Cons => { // assuming just [f] not [f,..]
        val f = new Cons(c.getHead, S2)
            val e = new eq()
            val args = new Array[Term](2)
            args(0) = S1
            args(1) = f
            e.args = args
            e
        }
        //case b: cut => b
        //case n: neck => n
        case b: ConstBuiltin => {
            S1.set_to(S2)
            b
        }
        case f: FunBuiltin => {
            S1.set_to(S2)
            f
        }
        case g: Const => {
            if (g.sym == "{}" && g.isInstanceOf[Fun]) {
                val x = g.asInstanceOf[Fun].args(0) // validate
                S1.set_to(S2)
                x
            } else {
                val f = new Fun("vs")
                val args = new Array[Term](2)
                args(0) = S1
                args(1) = S2
                f.args = args
                termcat.action(g, f)
            }
        }
    }

    def dcg_body(bs: List[Term], S1: Var, S2: Var) = {
        //println("here=" + bs)
        val r = dcg_conj(bs, S1, S2)
        //println("there=" + r)
        r
    }

    def dcg_conj(ts: List[Term], S1: Var, S2: Var): List[Term] = {
        ts match {
            case Nil => {
                S1.set_to(S2)
                Nil
            }
            case Const.nil :: Nil => {
                S1.set_to(S2)
                Nil
            }
            case x :: xs => {
                val V = new Var()
                val y = dcg_goal(x, S1, V)
                val ys = dcg_conj(xs, V, S2)
                y :: ys
            }

        }
    }

    def string2goal(s: String, parser: PrologParser): List[Term] =
        parser.parse("true :- " + s + ". ")

    def string2goal(s: String): List[Term] = string2goal(s, new PrologParser())

    def main(args:Array[String]) {
        val p = new PrologParser()
        val example = "candidates(13,3,0,10,[ev(-900,'C0730510','Inferior oblique muscle paresis','Inferior oblique underaction',[inferior,oblique,muscle,paresis],[dsyn],[[[3,4],[1,2],0],[[5,5],[3,3],0],[[1,1],[4,4],0]],yes,no,['SNOMEDCT'],[148/7,159/23],0),ev(-797,'C0030552','Muscle Paresis','Paresis',[muscle,paresis],[sosy],[[[5,5],[1,1],0],[[1,1],[2,2],0]],yes,no,['MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','COSTAR','CST','CHV','SNMI'],[148/7,176/6],0),ev(-760,'C0030552','Paresis','Paresis',[paresis],[sosy],[[[1,1],[1,1],0]],yes,no,['MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','COSTAR','CST','CHV','SNMI'],[148/7],0),ev(-695,'C0582817','Inferior oblique muscle','Inferior oblique muscle structure',[inferior,oblique,muscle],[bpoc],[[[3,4],[1,2],0],[[5,5],[3,3],0]],no,no,['MTH','SNOMEDCT','SNMI','SNM','CHV','NCI','FMA','UWDA'],[159/23],0),ev(-640,'C0224133','Inferior oblique','Entire inferior oblique',[inferior,oblique],[bpoc],[[[3,4],[1,2],0]],no,no,['MTH','SNOMEDCT'],[159/16],0),ev(-640,'C0582817','Inferior oblique','Inferior oblique muscle structure',[inferior,oblique],[bpoc],[[[3,4],[1,2],0]],no,no,['MTH','SNOMEDCT','SNMI','SNM','CHV','NCI','FMA','UWDA'],[159/16],0),ev(-593,'C0026845','Muscle','Muscle',[muscle],[tisu],[[[5,5],[1,1],0]],no,no,['FMA','LCH','LNC','MSH','MTH','NCI','OMIM','SNM','SNOMEDCT','UWDA','AOD','CHV','CSP','ICF-CY','ICF','SNMI'],[176/6],0),ev(-593,'C0205315','Oblique','Oblique',[oblique],[spco],[[[4,4],[1,1],0]],no,no,['LNC','NCI','SNMI','SNOMEDCT','CHV'],[168/7],0),ev(-593,'C0542339','Inferior','Inferior',[inferior],[spco],[[[3,3],[1,1],0]],no,no,['FMA','LNC','MTH','NCI','SNMI','SNOMEDCT','UWDA','CHV'],[159/8],0),ev(-593,'C0678975',inferior,inferiority,[inferior],[socb],[[[3,3],[1,1],0]],no,no,['AOD','CHV','MTH'],[159/8],0),ev(-560,'C1995013','Muscles','Set of muscles',[muscles],[bpoc],[[[5,5],[1,1],1]],no,no,['FMA','MTH','UWDA'],[176/6],1),ev(-521,'C0442025','Muscular','Muscular',[muscular],[spco],[[[5,5],[1,1],3]],no,no,['MTH','SNMI','SNOMEDCT','CHV'],[176/6],1),ev(-493,'C1053014','Musculus','Musculus <invertebrate>',[musculus],[euka],[[[5,5],[1,1],6]],no,no,['MTH','NCBI','CHV'],[176/6],1)])."

        //Source.fromFile("/home/dirk/workspace/public_mm_print/example_mmo.txt").mkString
        val u = p.parse(example)
        u
    }
}
