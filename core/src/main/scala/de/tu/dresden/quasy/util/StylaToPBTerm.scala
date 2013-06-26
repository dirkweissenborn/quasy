package de.tu.dresden.quasy.util

import prolog.terms._
import se.sics.prologbeans.PBTerm

/**
 * @author dirk
 *          Date: 6/18/13
 *          Time: 3:49 PM
 */
object StylaToPBTerm {

    def transform(stylaTerm:Term):PBTerm = stylaTerm match {
        case list:Cons => {
           if(list.toString().equals(Const.nil.sym))
               PBTerm.NIL
           else
              PBTerm.makeTerm(transform(list.getHead), transform(list.getBody))
        }
        case fun:Fun => {
            PBTerm.makeTerm(fun.sym,fun.args.map(transform))
        }
        case constant: Const => {
            PBTerm.makeAtom(constant.sym)
        }
        case real:Real => {
           PBTerm.makeTerm(real.nval.toDouble)
        }
        case num:SmallInt => {
            PBTerm.makeTerm(num.getValue.toInt)
        }
    }

}
