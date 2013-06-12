package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation._
import scala.Some
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version
import de.tu.dresden.quasy.model.db.{UmlsSemanticNetwork, LuceneIndex}
import de.tu.dresden.quasy.dependency.DepNode
import de.tu.dresden.quasy.answer.model.FactoidAnswer
import org.apache.lucene.search.TopDocs

/**
 * @author dirk
 *         Date: 5/17/13
 *         Time: 10:50 AM
 */
class QuestionEnhancer(val luceneIndex: LuceneIndex) extends TextEnhancer {
    //Everything under anatomical structure and spatial concept
    private final val whereAnswerTypes = Array("anst", "emst", "anab", "cgab", "acab", "ffas", "bpoc", "tisu", "cell", "celc", "gngm", "spco", "bsoj", "blor", "mosq", "nusq", "amas", "crbs", "geoa")

    def enhance(text: AnnotatedText) {
        text.getAnnotations[Question].foreach(question => {
            //What be ...? - questions
            val chunks = text.getAnnotationsBetween[Chunk](question.begin, question.end)


            //############ WHERE #####################
            if (WHERE_IN_NP(chunks)) {
                val target = extractTargetTypes(chunks(2))
                target match {
                    case ConceptualAnswerType(coveredText, concepts) => {
                        val semanticTypes = concepts.filter(_.isInstanceOf[UmlsConcept]).map(_.asInstanceOf[UmlsConcept]).flatMap(_.semanticTypes).toSet
                        var answerTypes = semanticTypes.flatMap(st => UmlsSemanticNetwork.?("?", "part_of", st).map(_._1)).toSet.toArray
                        if (answerTypes.isEmpty)
                            answerTypes = whereAnswerTypes
                        question.answerType = SemanticAnswerType("", answerTypes)
                    }
                    case _ => question.answerType = SemanticAnswerType("", whereAnswerTypes)
                }
            }
            else if (question.coveredText.toLowerCase.startsWith("where"))
                question.answerType = SemanticAnswerType("", whereAnswerTypes)


            //########## WHAT ########################
            else if (WHAT_BE_NP(chunks) || NP_PP_WHAT_NP_VP(chunks)) {
                question.answerType = extractTargetTypes(chunks(2))
            } else if (WHAT_NP_VP(chunks)) {
                question.answerType = extractTargetTypes(chunks(0))
            } else if (PP_WHAT_NP_VP(chunks))
                question.answerType = extractTargetTypes(chunks(1))
            else if (WHAT_NP_PP_NP_VP(chunks))
                //TODO this is a poor heuristic, might work in many cases though
                question.answerType = extractTargetTypes(chunks(2))

            //######### Is/are ... or ...?############
            //TODO cleaning
            else if (question.getTokens.head.lemma == "be" && question.getTokens.exists(_.lemma == "or")) {
                var criterion = ""
                //Is Rheumatoid Arthritis !the result! of ...?  -> attr
                //Is Rheumatoid Arthritis !more common! in ...? -> acomp
                question.getTokens.find(t => t.depDepth == 1 && t.depTag.tag.matches("acomp|attr")) match {
                    case Some(t) => {
                        val depTree = question.getDependencyTree
                        val nodes = depTree.getSubtree(t).nodes.toList.map(_.value.asInstanceOf[DepNode])
                        criterion = nodes.filter(_.nodeHead.position <= t.position).flatMap(_.tokens).toSeq.sortBy(_.position).map(_.coveredText).mkString(" ")
                    }
                    case None =>
                }
                var answers = List[FactoidAnswer]()
                question.getTokens.find(t => t.depTag.tag.matches("prep")) match {
                    case Some(t) => {
                        criterion += " "+t.coveredText
                        val it = question.getTokens.dropRight(1).filter(_.position > t.position).iterator
                        while (it.hasNext) {
                            val targetTokens = it.takeWhile(!_.lemma.matches(",|or")).toList
                            answers ++= (extractOntologyEntityMention(targetTokens) match {
                                case (Some(oem),targetTxt) => {
                                    oem.ontologyConcepts.map(oc => FactoidAnswer(targetTxt, oc,question))
                                }
                                case (None,targetTxt) => List(FactoidAnswer(targetTxt,question))
                            } )
                        }
                    }
                    case None =>
                }

                question.answerType = DecisionAnswerType(answers.map(_.answerText).mkString(", "),answers.toArray,criterion)
            }
        })
    }

    protected def extractTargetTypes(chunk: Chunk): AnswerType = {
        val targetTokens = chunk.getTokens.filter(_.posTag.matches(targetTypePosPattern))

        extractOntologyEntityMention(targetTokens) match {
            case (Some(oem),targetTxt) => {
                ConceptualAnswerType(targetTxt, oem.ontologyConcepts)
            }
            case (None,targetTxt) => AnswerType(targetTxt)
        }
    }

    protected def extractOntologyEntityMention(targetTokens: List[Token]): (Option[OntologyEntityMention],String) = {
        val text = targetTokens.head.context
        var targetTokenTxt = targetTokens.map(_.coveredText)
        while ( {
            val queryStr: String = targetTokenTxt.permutations.map(perm => "\"" + perm.mkString(" ") + "\"").mkString(" OR ")
            val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(queryStr)

            val result = luceneIndex.searcher.search(q, null, 100)
            result.totalHits < 100 && !targetTokenTxt.isEmpty
        })
            targetTokenTxt = targetTokenTxt.drop(1)

        val targetTxt = targetTokenTxt.mkString(" ")

        (text.getAnnotationsBetween[OntologyEntityMention](targetTokens.head.begin, targetTokens.last.end).
            find(oem => oem.coveredText.equals(targetTxt) && oem.ontologyConcepts.exists(_.isInstanceOf[UmlsConcept])),targetTxt)
    }

    //WHERE PATTERN
    def WHERE_IN_NP(chunks: List[Chunk]) =
        chunks.head.coveredText.matches(wherePattern) &&
            chunks(1).coveredText.toLowerCase.equals("in") &&
            chunks(2).chunkType.equals("NP")


    //WHAT PATTERN
    def WHAT_BE_NP(chunks: List[Chunk]) =
        chunks.head.coveredText.matches(whatWhichPattern) &&
            chunks(1).getTokens.minBy(_.depDepth).lemma.equals("be") &&
            chunks(2).chunkType.equals("NP")

    def WHAT_NP_VP(chunks: List[Chunk]) =
        chunks.head.chunkType.equals("NP") &&
        chunks(1).chunkType.equals("VP") &&
            chunks.head.coveredText.matches(whatWhichPattern + ".+")

    def NP_PP_WHAT_NP_VP(chunks: List[Chunk]) =
        chunks.head.chunkType.equals("NP") &&
            chunks(1).chunkType.equals("PP") && {
            val target = chunks(2)
            target.coveredText.matches(whatWhichPattern + ".+")
        }

    //Which forms of cancer are ...?
    def WHAT_NP_PP_NP_VP(chunks: List[Chunk]) =
        chunks.head.chunkType.equals("NP") &&
            chunks.head.coveredText.matches(whatWhichPattern + ".+") &&
            chunks(1).chunkType.equals("PP") &&
            chunks(2).chunkType.equals("NP")

    def PP_WHAT_NP_VP(chunks: List[Chunk]) =
        chunks.head.chunkType.equals("PP") && {
            val target = chunks(1)
            target.coveredText.matches(whatWhichPattern + ".+")
        }

    final val whatWhichPattern = "(What|what|Which|which)"
    final val wherePattern = "(Where|where)"

    final val targetTypePosPattern = PosTag.ANYNOUN_PATTERN + "|" + PosTag.Adjective
}
