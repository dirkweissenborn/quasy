package de.tu.dresden.quasy.answer.postprocess

import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 6/12/13
 * Time: 3:00 PM
 */
class AnswerPostProcesserSet(set:Set[AnswerPostProcessor]) extends AnswerPostProcessor {
    def processFactoid(factoidAnswer: FactoidAnswer) {
        set.foreach(_.processFactoid(factoidAnswer))
    }
}
