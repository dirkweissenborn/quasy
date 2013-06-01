package de.tu.dresden.quasy.answer.score.context

import de.tu.dresden.quasy.similarity.SimilarityMeasure
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.model.annotation.Chunk

/**
 * @author dirk
 * Date: 5/15/13
 * Time: 10:48 AM
 */
class ChunkComparisonScorer(similarityMeasure: SimilarityMeasure) extends AnswerContextScorer {
    def score(candidate: AnswerContext) = {
        var sum = 0.0
        val answerChunks = candidate.answerContext.context.getAnnotationsBetween[Chunk](candidate.answerContext.begin,candidate.answerContext.end)
        val questionChunks: List[Chunk] = candidate.question.context.getAnnotationsBetween[Chunk](candidate.question.begin, candidate.question.end).filterNot(_.chunkType.equals("PP"))
        if (answerChunks.size > 0)
            questionChunks.foreach(qChunk => {
                val mappedAnswerChunk = answerChunks.map(aChunk => {
                    (aChunk,similarityMeasure.phraseSimilarity(qChunk.getTokens.toArray,aChunk.getTokens.toArray))
                }).maxBy(_._2)

                sum += mappedAnswerChunk._2
            })

        sum / questionChunks.size
    }
}
