package de.tu.dresden.quasy.answer.score

import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.similarity.StringSimilarity
import de.tu.dresden.quasy.model.annotation.{Token, Sentence}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:50 PM
 */
class StringSimilarityScorer(similarityMeasure: StringSimilarity) extends Scorer {

    def score(candidate: AnswerCandidate) = {
        similarityMeasure.sentenceSimilarity(candidate.answerText.getAnnotations[Token].map(_.coveredText).toArray, candidate.question.getAnnotations[Token].map(_.coveredText).toArray)
    }

}
