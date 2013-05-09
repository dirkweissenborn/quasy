package de.tu.dresden.quasy.answer.score

import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.model.annotation.Token
import de.tu.dresden.quasy.similarity.SimilarityMeasure
import util.matching.Regex

/**
 * @author dirk
 * Date: 4/30/13
 * Time: 3:01 PM
 */
class SRLScorer(similarityMeasure: SimilarityMeasure) extends Scorer {

    //TODO CHECK
    final val weights = Map[Regex,Double]( ("A[1-2]".r -> 1.0), ("A[3-4A]".r -> 0.8), ("AM.*".r -> 0.5))

    def score(candidate: AnswerCandidate) = {
        val questionSrls = candidate.question.getTokens.flatMap(_.getRelationsAsString)
        val answerSrls = candidate.answer.getTokens.flatMap(_.getRelationsAsString)
        var normalizer = 0.0

        /*questionSrls.map{
            case (srole,(pred,predHead),(arg,argHead)) => {
                val max = answerSrls.foldLeft(0.0){
                    case (max, (answerSRole,(answerPred,_),(answerArg,_))) => {
                        //if (cleanSRole(srole).equals(cleanSRole(answerSRole))) {
                            var sim = similarityMeasure.phraseSimilarity(pred,answerPred)
                            if (!srole.startsWith("R-"))
                                sim *= similarityMeasure.phraseSimilarity(arg,answerArg)

                            math.max(sim, max)
                        //}
                        //else
                            //max
                    }
                }

                val depthWeight = 1.0 /(1+predHead.depDepth)
                val labelWeight = weights.find(_._1.findFirstIn(srole).isDefined).getOrElse((null,0.0))._2

                normalizer += depthWeight*labelWeight
                max * depthWeight*labelWeight
            }
        }.sum / math.max(normalizer,1)   */
        0.0
    }

    def cleanSRole(srole:String) = srole.replace("R-","").replace("C-","")
}

