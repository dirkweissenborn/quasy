package de.tu.dresden.quasy.answer.score

import de.tu.dresden.quasy.answer.model.AnswerCandidate

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:52 PM
 */
trait Scorer {

    def score(candidate:AnswerCandidate) : Double

}
