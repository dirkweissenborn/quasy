package de.tu.dresden.quasy.answer.filter

import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.answer.score.context.AnswerContextScorer

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:46 PM
 */
class ContextFilter(scorer:AnswerContextScorer) {

    def filter(candidates:List[AnswerContext], maxNr:Int) : List[AnswerContext] = {
        val manifest = Manifest.classType(scorer.getClass)
        val sortedCandidates = candidates.sortBy(candidate => {
            val score = scorer.score(candidate)
            candidate.scores += (manifest -> score)
            -score
        })
        if (maxNr < sortedCandidates.size) {
            val threshold = sortedCandidates(maxNr).scores(manifest)
            sortedCandidates.takeWhile(_.scores(manifest) >= threshold)
        }
        else
            sortedCandidates
    }

    def filter(candidates:List[AnswerContext], threshold:Double) : List[AnswerContext] = {
        candidates.filter(candidate => {
            val score = scorer.score(candidate)
            candidate.scores += (Manifest.classType(scorer.getClass) -> score)
            score >= threshold
        })
    }

}
