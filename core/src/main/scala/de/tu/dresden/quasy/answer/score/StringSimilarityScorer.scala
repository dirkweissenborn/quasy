package de.tu.dresden.quasy.answer.score

import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.similarity.SimilarityMeasure
import de.tu.dresden.quasy.model.annotation.{Token, Sentence}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:50 PM
 */
class StringSimilarityScorer(similarityMeasure: SimilarityMeasure) extends Scorer {

    def score(candidate: AnswerCandidate) = {
        similarityMeasure.phraseSimilarity(candidate.answer.getTokens.toArray, candidate.question.getTokens.toArray)
    }

}
