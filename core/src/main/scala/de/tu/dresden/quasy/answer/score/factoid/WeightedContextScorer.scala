package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.{FactoidAnswer, AnswerContext}
import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention,Annotation}
import de.tu.dresden.quasy.answer.score.context.{AnswerContextScorer, ConceptComparisonScorer}

/**
 * @author dirk
 * Date: 6/5/13
 * Time: 4:56 PM
 */
case class WeightedContextScorer[T <: Annotation](answerContexts:List[AnswerContext], similarityScorer:AnswerContextScorer)(implicit m:Manifest[T]) extends FactoidScorer{
    //sentences as (bag of its concepts, similarity score to question)
    private val sentences = answerContexts.map(ctxt => ctxt.answerContext.context.getAnnotationsBetween[T](ctxt.answerContext.begin,ctxt.answerContext.end).map(annotation => {
       (annotation.getAnnotationsWithin[OntologyEntityMention].flatMap(_.ontologyConcepts),
           similarityScorer.score(AnswerContext(annotation,ctxt.question)))
    })).flatten

    private val sum = sentences.map(_._2).sum

    def scoreInternal(factoid: FactoidAnswer) = {
        sentences.map {
            case (concepts, score) => {
                concepts
                if(concepts.exists(_.equals(factoid.answer)))
                    score
                else
                    0.0
            }
        }.sum / sum
    }

    override val man = manifest[WeightedContextScorer[T]]
}
