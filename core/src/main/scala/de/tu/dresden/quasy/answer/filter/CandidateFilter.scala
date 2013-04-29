package de.tu.dresden.quasy.answer.filter

import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.answer.score.Scorer

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:46 PM
 */
class CandidateFilter(scorer:Scorer) {

    def filter(candidates:List[AnswerCandidate], maxNr:Int) : List[AnswerCandidate] = {
        candidates.sortBy(candidate => {
            val score = scorer.score(candidate)
            candidate.scores += (Manifest.classType(scorer.getClass) -> score)
            -score
        }).take(maxNr)
    }

}
