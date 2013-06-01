package de.tu.dresden.quasy.answer.score.context

import de.tu.dresden.quasy.answer.model.AnswerContext
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
object ConceptComparisonScorer extends AnswerContextScorer {
    def score(candidate: AnswerContext) = {
        val answerConcepts = candidate.answerContext.context.getAnnotationsBetween[OntologyEntityMention](candidate.answerContext.begin, candidate.answerContext.end).flatMap(_.ontologyConcepts)
        var sum = 0.0

        val mentions: List[OntologyEntityMention] = candidate.question.context.getAnnotationsBetween[OntologyEntityMention](candidate.question.begin, candidate.question.end)
        mentions.foreach(qOem => {
            getAllowedAnswerConcepts(qOem).find(answerConcepts.contains) match {
                case Some(oc) => sum += 1
                case None => //...
            }
        })

        sum / mentions.size
    }

    def getAllowedAnswerConcepts(mention:OntologyEntityMention):List[OntologyConcept] = {
        val acronym = mention.getTokens.map(_.coveredText(0).toUpper).mkString("")

        //TODO why 0.8? just something lower than 1.0
        if (mention.ontologyConcepts.exists(_.source.equals(OntologyConcept.SOURCE_ACRONYM)))
            mention.ontologyConcepts.flatMap(getAllowedAnswerConcepts)
        else
            new OntologyConcept(OntologyConcept.SOURCE_ACRONYM,acronym,acronym,Set[String](),0.8) :: mention.ontologyConcepts.flatMap(getAllowedAnswerConcepts)
    }

    def getAllowedAnswerConcepts(concept:OntologyConcept):List[OntologyConcept] = {
        concept.source match {
            case OntologyConcept.SOURCE_MESH => {
                //Allow Mesh parents
                //Why? Example:
                // Q: Does change in blood pressure predict heart disease?
                // A: Blood pressure change was positively related to risk of cardiovascular disease.
                // cardiovascular disease is parent of heart disease
                // -> if we can say something about the parent, this should hold for the children in the taxonomy as well
                val nrs = MeshTree.getTreeNrs(concept.preferredLabel)
                concept :: nrs.map(nr => {
                    if (nr.contains(".")) {
                        val parentNr = nr.substring(0, nr.lastIndexOf('.'))
                        val heading: String = MeshTree.getHeading(parentNr)
                        new OntologyConcept(heading, concept.source,heading,Set[String]())
                    }
                    else null
                }).toList
            }
            case _ => List(concept)
        }
    }
}
