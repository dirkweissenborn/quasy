package de.tu.dresden.quasy.answer.score.factoid.tycor

import de.tu.dresden.quasy.model.annotation.{ConceptualAnswerType, UmlsConcept, OntologyConcept, AnswerType}
import de.tu.dresden.quasy.model.db.MeshTree
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 5/24/13
 * Time: 2:49 PM
 */
object MeshTycor extends TycorScorer {

    def scoreInternal(factoid: FactoidAnswer) = {
        val instance = factoid.answer
        if (factoid.question.answerType.isInstanceOf[ConceptualAnswerType]) {
            val aType = factoid.question.answerType.asInstanceOf[ConceptualAnswerType]

            val questionTreeNrs =
                (aType.concepts.filter(_.source == OntologyConcept.SOURCE_MESH).flatMap(c => MeshTree.getTreeNrs(c.conceptId))  ++
                    aType.concepts.filter(_.isInstanceOf[UmlsConcept]).flatMap(_.asInstanceOf[UmlsConcept].meshTreeNrs)).toSet

            if (questionTreeNrs.isEmpty)
                1.0
            else
                instance match {
                    case umlsConcept:UmlsConcept => {
                        if (questionTreeNrs.exists(treeNr => umlsConcept.meshTreeNrs.exists(ac => ac.startsWith(treeNr)) ))
                            1.0 else 0.0
                    }
                    case meshConcept:OntologyConcept if instance.source.equals(OntologyConcept.SOURCE_MESH) => {
                        val answerTreeNrs = MeshTree.getTreeNrs(meshConcept.conceptId)
                        if (questionTreeNrs.exists(treeNr => answerTreeNrs.exists(ac => ac.startsWith(treeNr))))
                            1.0 else 0.0
                    }
                    case _ => 0.0
                }
        }
        else
            0.0
    }

}
