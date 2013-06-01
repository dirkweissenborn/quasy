package de.tu.dresden.quasy.answer.score.factoid.tycor

import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.{FactoidAnswer, AnswerContext}
import de.tu.dresden.quasy.model.annotation.OntologyEntityMention
import collection.Set
import de.tu.dresden.quasy.dependency.DepNode

/**
 * @author dirk
 * Date: 5/30/13
 * Time: 10:56 AM
 */
class ParagraphTycor(paragraphs:List[AnswerContext]) extends FactoidScorer {

    private val paraToEnts =  paragraphs.map(p => (p,p.answerContext.context.getAnnotationsBetween[OntologyEntityMention](p.answerContext.begin,p.answerContext.end) )).toMap

    protected[factoid] def scoreInternal(factoid: FactoidAnswer) = {
        var score = 0.0
        val targetConcepts = factoid.question.targetType.concepts

        if (paragraphs.exists(p => {
            val targetTokens =
                if (targetConcepts.isEmpty) {
                    //String matching
                    val regex = factoid.question.targetType.coveredText.split(" ").permutations.map("("+_.mkString(" ")+")").mkString("|")
                    val matches = regex.r.findAllIn(p.answerContext.coveredText)

                    matches.matchData.map(rMatch =>
                        p.answerContext.getTokens.
                            filter(_.between(rMatch.start + p.answerContext.begin, rMatch.end + p.answerContext.begin)).minBy(_.depDepth)).toList
                }
                else {
                    //Concept matching
                    val targetMentions = paraToEnts(p).filter(_.ontologyConcepts.exists(c => targetConcepts.contains(c)))
                    targetMentions.map(_.getTokens.minBy(_.depDepth)).toList
                }

            val answerMentions  = paraToEnts(p).filter(_.ontologyConcepts.contains(factoid.answer))

            val answerTokens = answerMentions.map(am =>
                if (am.getTokens.isEmpty)
                    null
                else
                    am.getTokens.minBy(_.depDepth))

            targetTokens.exists(targetToken => {
                answerTokens.exists(answerToken => {
                    //match if occurs "TARGET ANSWER" or "TARGET BE ANSWER"
                    targetToken.sentence == answerToken.sentence && {
                        val t = targetToken.sentence.getDependencyTree
                        val targetNode = t.getDepNode(targetToken).get
                        val answerNode = t.getDepNode(answerToken).get

                        targetNode != answerNode &&
                        (targetNode.findIncomingFrom(answerNode).isDefined || {
                            val intersect = targetNode.diPredecessors.intersect(answerNode.diPredecessors)
                            !intersect.isEmpty && intersect.head.value.asInstanceOf[DepNode].nodeHead.lemma == "be"
                        } || {
                            val intersect = targetNode.diPredecessors.intersect(answerNode.diSuccessors)
                            !intersect.isEmpty && intersect.head.value.asInstanceOf[DepNode].nodeHead.lemma == "as"
                        })
                    }
                })
            })
        }) )
            score = 1.0


        score
    }

}
