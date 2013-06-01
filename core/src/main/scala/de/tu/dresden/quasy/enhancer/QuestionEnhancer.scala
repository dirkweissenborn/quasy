package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation._
import scala.Some
import de.tu.dresden.quasy.util.LuceneIndex
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version

/**
 * @author dirk
 * Date: 5/17/13
 * Time: 10:50 AM
 */
class QuestionEnhancer(val luceneIndex:LuceneIndex) extends TextEnhancer{
    def enhance(text: AnnotatedText) {
        text.getAnnotations[Question].foreach(question => {
            //What be ...? - questions
            val chunks = text.getAnnotationsBetween[Chunk](question.begin, question.end)

            if(WHAT_BE_NP(chunks) || NP_PP_WHAT_NP_VP(chunks) || PP_WHAT_NP_VP(chunks)) {
                extractTargetTypes(chunks(2), text, question)
            } else if (WHAT_NP_VP(chunks)) {
                extractTargetTypes(chunks(0), text, question)
            }
        })
    }


    protected def extractTargetTypes(chunk: Chunk, text: AnnotatedText, question: Question) {
        val targetTokens = chunk.getTokens.filter(_.posTag.matches(targetTypePosPattern))

        var targetTokenTxt = targetTokens.map(_.coveredText)
        while ( {
            val queryStr: String = targetTokenTxt.permutations.map(perm => "\"" + perm.mkString(" ")+"\"").mkString(" OR ")
            val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(queryStr)

            luceneIndex.searcher.search(q,null,100).scoreDocs.size < 100 && !targetTokenTxt.isEmpty
        })
            targetTokenTxt = targetTokenTxt.drop(1)

        val targetTxt = targetTokenTxt.mkString(" ")

        text.getAnnotationsBetween[OntologyEntityMention](targetTokens.head.begin, targetTokens.last.end).
            find(oem => oem.coveredText.equals(targetTxt) && oem.ontologyConcepts.exists(_.isInstanceOf[UmlsConcept])) match {
            case Some(oem) => {
                question.targetType = TargetType(targetTxt, oem.ontologyConcepts)
            }
            case None => {
                if(!targetTokenTxt.isEmpty)
                    question.targetType = TargetType(targetTxt)
            }
        }
    }

    def WHAT_BE_NP(chunks:List[Chunk]) =
        chunks.head.coveredText.matches(whatWhichPattern)  &&
        chunks(1).getTokens.minBy(_.depDepth).lemma.equals("be") &&
        chunks(2).chunkType.equals("NP")

    def WHAT_NP_VP(chunks:List[Chunk]) =
        chunks.head.chunkType.equals("NP") &&
        chunks.head.coveredText.matches(whatWhichPattern+".+")

    def NP_PP_WHAT_NP_VP(chunks:List[Chunk]) =
        chunks.head.chunkType.equals("NP") &&
        chunks(1).chunkType.equals("PP") && {
            val target = chunks(2)
            target.coveredText.matches(whatWhichPattern+".+")
        }

    def PP_WHAT_NP_VP(chunks:List[Chunk]) =
        chunks.head.chunkType.equals("PP") && {
            val target = chunks(1)
            target.coveredText.matches(whatWhichPattern+".+")
        }

    final val whatWhichPattern = "(What|what|Which|which)"

    final val targetTypePosPattern = PosTag.ANYNOUN_PATTERN+"|"+PosTag.Adjective
}
