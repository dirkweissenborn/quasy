package de.tu.dresden.quasy.answer.score

import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.model.annotation.{OntologyConcept, OntologyEntityMention}
import de.tu.dresden.quasy.model.db.MeshTree

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 5:46 PM
 */
/**
 * This class scores answer candidates based on the existence of concepts present in the questions
 */
//TODO weighting of concepts? E.g. based on their mesh tree letter (A,B,...)
object ConceptComparisonScorer extends Scorer {
    def score(candidate: AnswerCandidate) = {
        val answerConcepts = candidate.answer.context.getAnnotationsBetween[OntologyEntityMention](candidate.answer.begin, candidate.answer.end).flatMap(_.ontologyConcepts)
        var sum = 0.0

        val mentions: List[OntologyEntityMention] = candidate.question.context.getAnnotationsBetween[OntologyEntityMention](candidate.question.begin, candidate.question.end)
        mentions.foreach(qOem => {
            qOem.ontologyConcepts.flatMap(getAllowedAnswerConcepts).find(answerConcepts.contains) match {
                case Some(oc) => sum += 1
                case None => //...
            }
        })

        sum / mentions.size
    }

    def getAllowedAnswerConcepts(concept:OntologyConcept) = {
        concept.source match {
            case OntologyConcept.SOURCE_MESH => {
                //Allow Mesh parents
                //Why? Example:
                // Q: Does change in blood pressure predict heart disease?
                // A: Blood pressure change was positively related to risk of cardiovascular disease.
                // cardiovascular disease is parent of heart disease
                // -> if we can say something about the parent, this should hold for the children in the taxonomy as well
                val nrs = MeshTree.getTreeNrs(concept.conceptId)
                concept :: nrs.map(nr => {
                    val parentNr = nr.substring(0, nr.lastIndexOf('.'))
                    new OntologyConcept(MeshTree.getHeading(parentNr), concept.source)
                }).toList
            }
            case _ => List(concept)
        }
    }
}
