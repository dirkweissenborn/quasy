package de.tu.dresden.quasy.answer.model

import de.tu.dresden.quasy.model.annotation.{Question, OntologyConcept}
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 11:33 AM
 */
case class FactoidAnswer(answerText:String, answer:OntologyConcept, question:Question) {
    private var scores = Map[Manifest[_ <: FactoidScorer],Double]()

    def addScore[T <: FactoidScorer](s:Double)(implicit m:Manifest[T]) {
        scores += (m -> s)
    }

    def score[T <: FactoidScorer]()(implicit m:Manifest[T]):Double = {
        scores.getOrElse(m,0.0)
    }

    def getScores = scores
}

object FactoidAnswer {
    def apply(answer:OntologyConcept, question:Question) = new FactoidAnswer(answer.preferredLabel,answer,question)
    def apply(answerText:String, question:Question) = new FactoidAnswer(answerText,null,question)
}
