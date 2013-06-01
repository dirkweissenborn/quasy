package de.tu.dresden.quasy.answer.score.context

import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.similarity.SimilarityMeasure
import de.tu.dresden.quasy.model.annotation.{Token, Sentence}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:50 PM
 */
class StringSimilarityScorer(similarityMeasure: SimilarityMeasure) extends AnswerContextScorer {

    def score(candidate: AnswerContext) = {
        similarityMeasure.phraseSimilarity(candidate.question.coveredText.split(" "), candidate.answerContext.coveredText.split(" "))
    }

}
