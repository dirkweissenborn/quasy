package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.{FactoidAnswer, AnswerContext}
import de.tu.dresden.quasy.model.annotation.OntologyEntityMention

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 12:43 PM
 */
class ConceptProminenceScorer(answerContexts:List[AnswerContext]) extends FactoidScorer{
    private val oems = answerContexts.flatMap(ctxt => ctxt.answerContext.context.getAnnotationsBetween[OntologyEntityMention](ctxt.answerContext.begin,ctxt.answerContext.end))

    protected[factoid] def scoreInternal(factoid: FactoidAnswer) = {
        oems.count(_.ontologyConcepts.exists(_.equals(factoid.answer))).toDouble / oems.size
    }
}
