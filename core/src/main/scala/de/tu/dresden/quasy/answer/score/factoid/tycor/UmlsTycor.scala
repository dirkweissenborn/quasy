package de.tu.dresden.quasy.answer.score.factoid.tycor

import de.tu.dresden.quasy.model.annotation.{SimpleAnswerTypeLike, SemanticAnswerType, UmlsConcept, OntologyConcept}
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.model.db.UmlsSemanticNetwork

/**
 * @author dirk
 * Date: 5/22/13
 * Time: 1:39 PM
 */
object UmlsTycor  extends TycorScorer {

    override protected def scorable(factoidAnswer: FactoidAnswer) =
        factoidAnswer.question.answerType != null &&
            factoidAnswer.question.answerType.isInstanceOf[SemanticAnswerType]

    def scoreInternal(factoid: FactoidAnswer) = {
        val instance = factoid.answer
        val aType = factoid.question.answerType.asInstanceOf[SemanticAnswerType]
        instance match {
            case umlsConcept:UmlsConcept => {
                val sTypes = aType.semanticTypes
                if (sTypes.intersect(umlsConcept.semanticTypes).size > 0 ||
                    sTypes.exists(st => umlsConcept.semanticTypes.exists(cSt => !UmlsSemanticNetwork.?(cSt,"isa",st).isEmpty)))
                  //semantic network information is very shallow --> just 0.5 as tycor score
                   0.5 // Naloxone for example is not under D26-- Pharmaceutical preparations --, so Mesh as taxonomy cannot be applied for e.g. "What is the best medication for opioid overdose?" - MeshTycor.scoreInternal(factoid)
                else
                   0.0
            }
            case _ => 0.0
        }
    }
}