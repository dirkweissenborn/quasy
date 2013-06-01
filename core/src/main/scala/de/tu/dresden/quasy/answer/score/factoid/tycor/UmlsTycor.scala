package de.tu.dresden.quasy.answer.tycor

import de.tu.dresden.quasy.model.annotation.{UmlsConcept, OntologyConcept, TargetType}
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 5/22/13
 * Time: 1:39 PM
 */
object UmlsTycor  extends FactoidScorer {

    def scoreInternal(factoid: FactoidAnswer) = {
        val instance = factoid.answer
        val aType = factoid.question.targetType

        instance match {
            case umlsConcept:UmlsConcept =>
                if (aType.concepts.filter(_.source == OntologyConcept.SOURCE_UMLS).flatMap(_.asInstanceOf[UmlsConcept].semanticTypes).intersect(umlsConcept.semanticTypes).size > 0)
                   1.0 // Naloxone for example is not under D26-- Pharmaceutical preparations --, so Mesh cannot be applied for for example "What is the best medication for opioid overdose?" - MeshTycor.scoreInternal(factoid)
                else
                    0.0
            case _ => 0.0
        }
    }
}