package de.tu.dresden.quasy.answer.postprocess

import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 6/10/13
 * Time: 1:56 PM
 */
trait AnswerPostProcessor {

    def processFactoid(factoidAnswer:FactoidAnswer)

}
