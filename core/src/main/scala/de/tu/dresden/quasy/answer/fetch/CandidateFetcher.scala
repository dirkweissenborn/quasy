package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.model.annotation.Question

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:59 PM
 */
trait CandidateFetcher {

    def fetch(question:Question, docCount:Int) : List[AnswerCandidate]

}
