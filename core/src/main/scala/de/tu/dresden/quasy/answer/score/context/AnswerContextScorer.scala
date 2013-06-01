package de.tu.dresden.quasy.answer.score.context

import de.tu.dresden.quasy.answer.model.AnswerContext

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:52 PM
 */
trait AnswerContextScorer {

    def score(candidate:AnswerContext) : Double

}
