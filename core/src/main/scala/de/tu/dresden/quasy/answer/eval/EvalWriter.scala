package de.tu.dresden.quasy.answer.eval

import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 6/10/13
 * Time: 1:56 PM
 */
trait EvalWriter {

    def writeFactoidEval(factoidAnswer:FactoidAnswer)

}
