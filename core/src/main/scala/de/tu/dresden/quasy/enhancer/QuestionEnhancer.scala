package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation._
import scala.Some
import org.apache.lucene.util.Version
import de.tu.dresden.quasy.model.db.{UmlsSemanticNetwork, LuceneIndex}
import de.tu.dresden.quasy.dependency.DepNode
import de.tu.dresden.quasy.answer.model.FactoidAnswer
import org.apache.lucene.queryparser.classic.QueryParser

/**
 * @author dirk
 *         Date: 5/17/13
 *         Time: 10:50 AM
 */
class QuestionEnhancer(val luceneIndex: LuceneIndex) extends TextEnhancer {
    //Everything under anatomical structure and spatial concept
    private final val whereAnswerTypes = Set("anst", "emst", "anab", "cgab", "acab", "ffas", "bpoc", "tisu", "cell", "celc", "gngm", "spco", "bsoj", "blor", "mosq", "nusq", "amas", "crbs", "geoa")

    protected def pEnhance(text: AnnotatedText) {
        text.getAnnotations[Question].foreach(question => {
            //What be ...? - questions
            val chunks = text.getAnnotationsBetween[Chunk](question.begin, question.end)


            //############ WHERE #####################
            if (WHERE_IN_NP(chunks)) {
                val target = extractTargetTypes(chunks(2))
                target match {
                    case ConceptualAnswerType(_, concepts) => {
                        val semanticTypes = concepts.filter(_.isInstanceOf[UmlsConcept]).map(_.asInstanceOf[UmlsConcept]).flatMap(_.semanticTypes).toSet
                        var answerTypes = semanticTypes.flatMap(st => UmlsSemanticNetwork.?("?", "part_of", st).map(_._1)).toSet
                        if (answerTypes.isEmpty)
                            answerTypes = whereAnswerTypes
                        question.answerType = SemanticAnswerType(answerTypes)
                    }
                    case _ => question.answerType = SemanticAnswerType(whereAnswerTypes)
                }
            }
            else if (question.coveredText.toLowerCase.startsWith("where"))
                question.answerType = SemanticAnswerType(whereAnswerTypes)

            else if(question.coveredText.toLowerCase.startsWith("list")){
                question.answerType = extractOntologyEntityMention(chunks(0).getTokens.drop(1)) match {
                    case (Some(oem),targets) => {
                        ConceptualAnswerType(targets, oem.ontologyConcepts)
                    }
                    case (None,targets) => SimpleAnswerType(targets)
                }
            }
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
            else if (WHAT_DO_STH_DO(question)){
                question.getTokens.find(t => t.srls.exists(srl => question.getTokens(srl.head-1).depDepth == 0 && srl.label.matches("A[03]"))) match {
                    case Some(token) => {
                        val subjTokens =
                            question.dependencyTree.getSubtree(token).nodes.toList.
                            map(_.value.asInstanceOf[DepNode]).flatMap(_.tokens).toList.sortBy(_.position).filterNot(_.posTag.equals(PosTag.Preposition_or_subordinating_conjunction))

                        extractOntologyEntityMention(subjTokens) match {
                            case (Some(oem),_) => {
                                val semanticTypes = oem.ontologyConcepts.filter(_.isInstanceOf[UmlsConcept]).map(_.asInstanceOf[UmlsConcept]).flatMap(_.semanticTypes).toSet
                                val answerTypes = semanticTypes.flatMap(st =>
                                    UmlsSemanticNetwork.?(st, ".*"+question.getTokens.find(_.depDepth == 0).get.lemma+".*", "?").map(_._3)).toSet
                                question.answerType = SemanticAnswerType(answerTypes)
                            }
                            case (None,_) =>
                        }
                    }
                    case None =>
                }
            }
            else if (WHAT_DO_STH(question)){
                question.getTokens.find(t => t.srls.exists(srl => question.getTokens(srl.head-1).depDepth == 0 && srl.label.matches("A[12]"))) match {
                    case Some(token) => {
                        val objTokens =
                            question.dependencyTree.getSubtree(token).nodes.toList.
                                map(_.value.asInstanceOf[DepNode]).flatMap(_.tokens).toList.sortBy(_.position)

                        extractOntologyEntityMention(objTokens) match {
                            case (Some(oem),_) => {
                                val semanticTypes = oem.ontologyConcepts.filter(_.isInstanceOf[UmlsConcept]).map(_.asInstanceOf[UmlsConcept]).flatMap(_.semanticTypes).toSet
                                val answerTypes = semanticTypes.flatMap(st =>
                                    UmlsSemanticNetwork.?("?", ".*"+question.getTokens.find(_.depDepth == 0).get.lemma+".*", st).map(_._1)).toSet
                                question.answerType = SemanticAnswerType(answerTypes)
                            }
                            case (None,_) =>
                        }
                    }
                    case None =>
                }
            }
            //######### Is/are ... or ...?############
            //TODO cleaning
            else if (question.getTokens.head.lemma == "be" && question.getTokens.exists(_.lemma == "or")) {
                var criterion = ""
                //Is Rheumatoid Arthritis !the result! of ...?  -> attr
                //Is Rheumatoid Arthritis !more common! in ...? -> acomp
                question.getTokens.find(t => t.depDepth == 1 && t.depTag.tag.matches("acomp|attr")) match {
                    case Some(t) => {
                        val depTree = question.dependencyTree
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
                                case (Some(oem),targets) => {
                                    oem.ontologyConcepts.map(oc => FactoidAnswer(targets.map(_.coveredText).mkString(" "), oc,question))
                                }
                                case (None,targets) => List(FactoidAnswer(targets.map(_.coveredText).mkString(" "),question))
                            } )
                        }
                    }
                    case None =>
                }

                question.answerType = DecisionAnswerType(answers.toArray,criterion)
            }
        })
    }

    protected def extractTargetTypes(chunk: Chunk): AnswerType = {
        val targetTokens = chunk.getTokens.filter(_.posTag.matches(targetTypePosPattern))

        extractOntologyEntityMention(targetTokens) match {
            case (Some(oem),targets) => {
                ConceptualAnswerType(targets, oem.ontologyConcepts)
            }
            case (None,targets) => SimpleAnswerType(targets)
        }
    }

    protected def extractOntologyEntityMention(targetTokens: List[Token]): (Option[OntologyEntityMention],List[Token]) = {
        val text = targetTokens.head.context
        var targets = targetTokens

        //an answer type can only be an answer type if it has at least 100 appearances in medline
        while ( {
            val queryStr: String = targets.map(_.coveredText).permutations.map(perm => "\"" + perm.mkString(" ") + "\"").mkString(" OR ")
            val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(queryStr)

            val result = luceneIndex.searcher.search(q, null, 100)
            result.totalHits < 100 && !targets.isEmpty
        })
            targets = targets.drop(1)
        if(targets.isEmpty)
            targets = targetTokens

        val begin = targets.minBy(_.begin).begin
        val end = targets.maxBy(_.end).end

        (text.getAnnotationsBetween[OntologyEntityMention](targetTokens.head.begin, targetTokens.last.end).
            find(oem => oem.begin == begin && oem.end == end && oem.ontologyConcepts.exists(_.isInstanceOf[UmlsConcept])),targets)
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
            chunks.head.coveredText.matches(".*"+whatWhichPattern + ".+")

    def NP_PP_WHAT_NP_VP(chunks: List[Chunk]) =
        chunks.head.chunkType.equals("NP") &&
            chunks(1).chunkType.equals("PP") && {
            val target = chunks(2)
            target.coveredText.matches(whatWhichPattern + ".+")
        }

    //Which forms of cancer are ...?
    def WHAT_NP_PP_NP_VP(chunks: List[Chunk]) =
        chunks.size > 3 &&
        chunks.head.chunkType.equals("NP") &&
            chunks.head.coveredText.matches(whatWhichPattern + ".+") &&
            chunks(1).chunkType.equals("PP") &&
            chunks(2).chunkType.equals("NP") &&
            chunks(3).chunkType.equals("VP")

    def WHAT_DO_STH_DO(question:Question) =
        question.getTokens.head.lemma.matches(whatWhichPattern) &&
        question.getTokens(1).lemma == "do"

    def WHAT_DO_STH(question:Question) =
        question.getTokens.head.lemma.matches(whatWhichPattern) &&
            question.getTokens(1).depDepth == 0


    def PP_WHAT_NP_VP(chunks: List[Chunk]) =
        chunks.head.chunkType.equals("PP") && {
            val target = chunks(1)
            target.coveredText.matches(whatWhichPattern + ".+")
        }

    final val whatWhichPattern = "(What|what|Which|which)"
    final val wherePattern = "(Where|where)"

    final val targetTypePosPattern = PosTag.ANYNOUN_PATTERN + "|" + PosTag.Adjective

    /*
    TODO
    Which is the molecular mechanism underlying K-ras alterations in carcinomas?

K[UMLS-Potassium Ion]	ras[UMLS-ras Oncogene]	carcinomas[UMLS-Carcinoma]
Chunk_NP[Which]	Chunk_VP[is]	Chunk_NP[the molecular mechanism underlying K-ras alterations]	Chunk_PP[in]	Chunk_NP[carcinomas]

R-A1(be,Which)	A2(be,the molecular mechanism underlying K - ras alterations in carcinomas)	A1(underlie,K - ras alterations)	AM-LOC(underlie,in carcinomas)
		2:nsubj:which:1
0:root:be:2
				5:det:the:3
				5:amod:molecular:4
		2:attr:mechanism:5
				5:partmod:underlie:6
										9:hmod:k:7
										9:hyph:-:8
								10:nn:ra:9
						6:dobj:alteration:10
						6:prep:in:11
								11:pobj:carcinoma:12
		2:punct:?:13
		target type: SimpleAnswerType(List(Token[ras], Token[alterations]))
     */
}
