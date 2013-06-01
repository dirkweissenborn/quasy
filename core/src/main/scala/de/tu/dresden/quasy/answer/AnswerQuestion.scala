package de.tu.dresden.quasy.answer

import de.tu.dresden.quasy.model.annotation._
import fetch.LuceneFetcher
import filter.ContextFilter
import model.{FactoidAnswer}
import scala.Left
import scala.Right
import java.util.Properties
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.io.AnnotatedTextSource
import de.tu.dresden.quasy.enhancer.umls.UmlsEnhancer
import de.tu.dresden.quasy.answer.tycor.{UmlsTycor}
import de.tu.dresden.quasy.util.LuceneIndex
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline}
import de.tu.dresden.quasy.enhancer.regex.RegexAcronymEnhancer
import de.tu.dresden.quasy.enhancer.clearnlp.{FullClearNlpPipeline}
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import score.context.{ConceptComparisonScorer, ChunkComparisonScorer, StringSimilarityScorer}
import de.tu.dresden.quasy.similarity.{WordnetSimilarity, SimpleStringSimilarity}
import score.factoid.tycor.ParagraphTycor
import score.factoid.{FinalConceptScorer, ConceptProminenceScorer}

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 11:47 AM
 */
class AnswerQuestion(config:Properties) {
    private val luceneIndex = LuceneIndex.fromConfiguration(config)
    private val fullClearNlp = FullClearNlpPipeline.fromConfiguration(config)
    private val chunker = OpenNlpChunkEnhancer.fromConfiguration(config)
    private val fullPipeline = new EnhancementPipeline(List(fullClearNlp, chunker, UmlsEnhancer, RegexAcronymEnhancer))
    private val fetcher = new LuceneFetcher(luceneIndex)

    def answer(question:Question, pmids:List[Int] = null) : Either[Map[OntologyConcept,Double], Map[String,Double]] = {
        val text = question.context
        println(text.id + "\t" + question.coveredText)
        println()
        println(text.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).map(_.toString).mkString("\t"))
        println(text.getAnnotationsBetween[Chunk](question.begin,question.end).map(_.toString).mkString("\t"))
        /*println()
        println(question.printRoleLabels)
        println(question.getDependencyTree.prettyPrint)  */
        if (question.targetType!=null) {
            println("target type: "+ question.targetType.coveredText+"[" + question.targetType.concepts.map(tt => tt.source).mkString(",")+"]")
        }
        println()

        question.questionType match {
            case FactoidQ => Left(answerFactoid(question,pmids))
            case YesNoQ => Right(answerYesNo(question,pmids))
            case ListQ => Left(answerList(question,pmids))
            case _ => Right(answerSummary(question,pmids))
        }
    }

    def answerFactoid(question:Question, pmids:List[Int]):Map[OntologyConcept,Double] = {
        Map(answerList(question,pmids).head)
    }

    def answerYesNo(question:Question, pmids:List[Int]):Map[String,Double] = {
        null
    }

    def answerList(question:Question, pmids:List[Int]):Map[OntologyConcept,Double] = {
        val simpleFilter = new ContextFilter(new StringSimilarityScorer(SimpleStringSimilarity))
        val chunkWordNetSimFilter = new ContextFilter(new ChunkComparisonScorer(WordnetSimilarity))
        val conceptFilter = new ContextFilter(ConceptComparisonScorer)


        // Preprocess answer contexts
        print("... fetching answers ...")

        val candidateSections =
            if (pmids != null)
                fetcher.fetchByPmid(question,pmids)
            else
                fetcher.fetch(question,10)

        println("done")

        print("... annotating answers ...")
        val answerTexts: Set[AnnotatedText] = candidateSections.map(_.answerContext.context).toSet
        fullPipeline.process(AnnotatedTextSource(answerTexts.toSeq:_*))
        val seqTexts: Seq[AnnotatedText] = answerTexts.toSeq
        //UmlsEnhancer.enhanceBatch(seqTexts)
        enhanceConceptsWithSynonyms(seqTexts)
        println("done")

        // Print answer contexts
        println("#########ANSWERS#############")
        candidateSections.foreach(section => {
            println(section.answerContext.context.id + "\t"+section.answerContext.coveredText)
            println(section.scores.map((entry) => entry._1.erasure.getSimpleName+"->"+entry._2).mkString("\n"))

            println(section.answerContext.context.getAnnotationsBetween[OntologyEntityMention](section.answerContext.begin,section.answerContext.end).map(oem => oem.toString).mkString("\t"))
            println(section.answerContext.context.getAnnotationsBetween[Chunk](section.answerContext.begin,section.answerContext.end).map(_.toString).mkString("\t"))
            /*
            candidateSentence.answerContext.context.getAnnotationsBetween[Sentence](candidateSentence.answerContext.begin,candidateSentence.answerContext.end).foreach(sentence => {
                println()
                println(candidateSentence.answerContext.context.getAnnotationsBetween[OntologyEntityMention](sentence.begin,sentence.end).map(oem => oem.toString).mkString("\t"))
                println(candidateSentence.answerContext.context.getAnnotationsBetween[Chunk](sentence.begin,sentence.end).map(_.toString).mkString("\t"))
                println()
                println(sentence.printRoleLabels)
                println(sentence.getDependencyTree.prettyPrint)
                println()
            })*/
        })

        // Extract candidate answers, exclude question concepts
        val qConcepts = question.context.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).flatMap(_.ontologyConcepts)
        val answerCandidates = candidateSections.flatMap(candidateSentence =>
            candidateSentence.answerContext.context.getAnnotationsBetween[OntologyEntityMention](candidateSentence.answerContext.begin,candidateSentence.answerContext.end).
                flatMap(_.ontologyConcepts)
        ).toSet.filterNot(c => qConcepts.contains(c)).map(c => new FactoidAnswer(c,question))


        // Score answers
        val prominenceScorer = new ConceptProminenceScorer(candidateSections)
        val paragraphTycor = new ParagraphTycor(candidateSections)

        print("scoring answers...")
        var sortedCandidates =answerCandidates.toSeq.sortBy(fa => {
            -prominenceScorer.score(fa)
        }).take(30)

        sortedCandidates = sortedCandidates.sortBy(fa => {
            UmlsTycor.score(fa)
            paragraphTycor.score(fa)
            -FinalConceptScorer.score(fa)
        })
        println("done")

        sortedCandidates.foreach(fa => {
            println(fa.answer.conceptId+"-"+fa.answer.preferredLabel + "\t\t" + fa.scores.map(s => s._1.erasure.getSimpleName+"="+s._2).mkString("\t"))
        })

        sortedCandidates.map(cand => (cand.answer,cand.scores(Manifest.classType(FinalConceptScorer.getClass)))).toMap
    }

    def answerSummary(question:Question, pmids:List[Int]):Map[String,Double] = {
        null
    }

    //########### Utilities ##############

    //should become an enhancer
    def enhanceConceptsWithSynonyms(answerTexts:Seq[AnnotatedText]) {
        answerTexts.flatMap(_.getAnnotations[OntologyEntityMention].
          flatMap(oem => oem.ontologyConcepts.map( (_,oem.coveredText) ))).
          groupBy(_._1).foreach{
            case (_,synonyms) => {
                val synSet = synonyms.map(_._2).toSet
                synonyms.foreach{
                    case (oc,_) => oc.synonyms = synSet
                }
            }
        }
    }

}
