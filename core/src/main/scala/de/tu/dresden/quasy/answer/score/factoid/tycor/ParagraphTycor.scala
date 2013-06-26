package de.tu.dresden.quasy.answer.score.factoid.tycor

import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.{FactoidAnswer, AnswerContext}
import de.tu.dresden.quasy.model.annotation._
import collection.Set
import de.tu.dresden.quasy.dependency.DepNode
import de.tu.dresden.quasy.model.PosTag
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.model.annotation.ConceptualAnswerType
import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 5/30/13
 * Time: 10:56 AM
 */
class ParagraphTycor(paragraphs:List[AnswerContext]) extends TycorScorer {

    private val paraToEnts =  paragraphs.map(p => (p,p.answerContext.context.getAnnotationsBetween[OntologyEntityMention](p.answerContext.begin,p.answerContext.end) )).toMap
    private val lemmaParagraphs = collection.mutable.Map[AnswerContext, String]()

    def scoreInternal(factoid: FactoidAnswer):Double = {
        var score = 0.0
        var evidence = ""
        val aType = factoid.question.answerType.asInstanceOf[SimpleAnswerTypeLike]
        if (!aType.coveredTokens.isEmpty) {
            val targetConcepts =
                if (aType.isInstanceOf[ConceptualAnswerType])
                    aType.asInstanceOf[ConceptualAnswerType].concepts
                else
                    List[OntologyConcept]()

            paragraphs.foreach(p => {
                // root tokens of target
                val targetTokens =
                    if (targetConcepts.isEmpty) {
                        //String matching
                        val regex = aType.coveredTokens.map(_.lemma).permutations.map("("+_.mkString(" ")+")").mkString("|")
                        val lemmaParagraphString: String = lemmaParagraphs.getOrElseUpdate(p, p.answerContext.getTokens.map(_.lemma).mkString(" "))
                        val matches = regex.r.findAllIn(lemmaParagraphString)

                        matches.matchData.map(rMatch => {
                            val startPosition = lemmaParagraphString.substring(0,rMatch.start).count(_.equals(' '))
                            val endPosition = startPosition + rMatch.toString.count(_.equals(' '))
                            val filteredTokens = p.answerContext.getTokens.drop(startPosition).take(endPosition+1-startPosition)
                            if(filteredTokens.size > 0)
                                filteredTokens.minBy(_.depDepth)
                            else
                                null
                        }).filter(_ ne null).toList
                    }
                    else {
                        //Concept matching
                        val targetMentions = paraToEnts(p).filter(_.ontologyConcepts.exists(c => targetConcepts.contains(c)))
                        targetMentions.map(_.getTokens.minBy(_.depDepth)).toList
                    }

                val answerMentions  = paraToEnts(p).filter(_.ontologyConcepts.contains(factoid.answer))
                //root tokens of answers
                val answerTokens = answerMentions.map(am =>
                    if (am.getTokens.isEmpty)
                        null
                    else
                        am.getTokens.minBy(_.depDepth))

                targetTokens.foreach(targetToken => {
                    answerTokens.foreach(answerToken => {
                        //match if occurs "TARGET ANSWER" or "TARGET BE ANSWER" or ""
                        if(targetToken.sentence == answerToken.sentence) {
                            /*val t = targetToken.sentence.getDependencyTree
                            val targetNode = t.getDepNode(targetToken).get
                            val answerNode = t.getDepNode(answerToken).get*/

                            if( targetToken == answerToken ||

                                answerToken.depTag.tag.matches("appos|npadvmod") &&
                                targetToken.position == answerToken.depTag.dependsOn ||

                               targetToken.depTag.tag == "nn" &&
                                answerToken.position == targetToken.depTag.dependsOn ||

                               answerToken.depTag.tag == "nn" &&
                                targetToken.position == answerToken.depTag.dependsOn  )
                            {
                                //this is a low scored type score, because these patterns don't necessarily imply that the answer is of the type
                                score = 0.5
                                evidence = answerToken.sentence.coveredText
                            }
                            if ({val tSrl = targetToken.srls.find(_.label == "A2")
                                val aSrl = answerToken.srls.find(_.label == "A1")
                                tSrl.isDefined && aSrl.isDefined && tSrl.get.head == aSrl.get.head &&
                                    targetToken.sentence.getTokens.find(_.position == aSrl.get.head).get.lemma == "be"
                            }||
                                targetToken.depTag.tag.matches("appos|npadvmod") &&
                                    answerToken.position == targetToken.depTag.dependsOn)
                            {
                                println("tycor evidence: "+answerToken.sentence.coveredText)
                                return 1.0
                            }

                        }
                    })
                })
            })
        }

        if (!evidence.isEmpty)
            println("tycor evidence: "+evidence)

        score
    }

}
