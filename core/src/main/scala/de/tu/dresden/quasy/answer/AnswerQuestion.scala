package de.tu.dresden.quasy.answer

import de.tu.dresden.quasy.model.annotation._
import postprocess.AnswerPostProcessor
import fetch.{NlmPubmedFetcher}
import model.{FactoidAnswer}

import java.util.Properties
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.io.AnnotatedTextSource
import de.tu.dresden.quasy.enhancer.umls.{UmlsEnhancer}
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline}
import de.tu.dresden.quasy.enhancer.regex.RegexAcronymEnhancer
import de.tu.dresden.quasy.enhancer.clearnlp.{FullClearNlpPipeline}
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import score.context.{StringSimilarityScorer, ConceptComparisonScorer}
import score.factoid._
import de.tu.dresden.quasy.model.db.LuceneIndex
import scala.Left
import score.factoid.WeightedContextScorer
import de.tu.dresden.quasy.model.annotation.DecisionAnswerType
import scala.Right
import score.factoid.tycor.{UmlsTycor, SupportingEvidenceTycor, ParagraphTycor}
import de.tu.dresden.quasy.similarity.WordnetSimilarity
import org.apache.lucene.util.Version

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 11:47 AM
 */
class AnswerQuestion(config:Properties,evalWriter:AnswerPostProcessor = null) {
    private val luceneIndex = LuceneIndex.fromConfiguration(config, Version.LUCENE_36)
    private val fullClearNlp = FullClearNlpPipeline.fromConfiguration(config)
    private val chunker = OpenNlpChunkEnhancer.fromConfiguration(config)
    private val fullPipeline = new EnhancementPipeline(List(fullClearNlp, chunker, UmlsEnhancer, RegexAcronymEnhancer))
    private val fetcher = new NlmPubmedFetcher

    def answer(question:Question, pmids:List[Int] = null) : Either[List[(FactoidAnswer,Double)], List[(String,Double)]] = {
        val text = question.context
        println(text.id + "\t" + question.coveredText)
        println()
        println(text.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).map(_.toString).mkString("\t"))
        println(text.getAnnotationsBetween[Chunk](question.begin,question.end).map(_.toString).mkString("\t"))
        println()
        println(question.printRoleLabels)
        println(question.dependencyTree.prettyPrint)
        println()
        if (question.answerType!=null) {
            println("target type: "+ question.answerType.toString)
        }
        println()

        question.questionType match {
            case FactoidQ => {
                if (question.answerType.isInstanceOf[DecisionAnswerType])
                    Left(answerFactoidDecision(question,pmids))
                else
                    Left(answerFactoid(question,pmids))
            }
            case YesNoQ => Right(answerYesNo(question,pmids))
            case ListQ => {
                if (question.answerType.isInstanceOf[DecisionAnswerType])
                    Left(answerListDecision(question,pmids))
                else
                    Left(answerList(question,pmids))
            }
            case _ => Right(answerSummary(question,pmids))
        }
    }

    def answerYesNo(question:Question, pmids:List[Int]):List[(String,Double)] = {
        List(("yes",1.0))
    }

    def answerFactoid(question:Question, pmids:List[Int]):List[(FactoidAnswer,Double)] = {
        answerList(question,pmids)
    }

    def answerList(question:Question, pmids:List[Int]):List[(FactoidAnswer,Double)] = {
        /*val simpleFilter = new ContextFilter(new StringSimilarityScorer(SimpleStringSimilarity))
        val chunkWordNetSimFilter = new ContextFilter(new ChunkComparisonScorer(WordnetSimilarity))
        val conceptFilter = new ContextFilter(ConceptComparisonScorer)  */

        /*val concept = new UmlsConcept("C0012854","DNA",Set("dna"),Array("bpoc"))
        val factoid = new FactoidAnswer(concept,question)
        println(SupportingEvidenceTycor(luceneIndex,3,fullPipeline).scoreInternal(factoid)) */
        // Preprocess answer contexts
        print("... fetching answers ...")

        val candidateSections =
            if (pmids != null)
                fetcher.fetchByPmid(question,pmids)
            else
                fetcher.fetch(question,5)

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

            println(section.answerContext.context.getAnnotationsBetween[OntologyEntityMention](section.answerContext.begin,section.answerContext.end).map(oem => oem.toString).toSet.mkString("\t"))
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
        ).toSet.filterNot(c => qConcepts.contains(c)).map(c => FactoidAnswer(c,question))


        // Score answers
        val prominenceScorer = new ConceptProminenceScorer(candidateSections)
        val paragraphTycor = new ParagraphTycor(candidateSections)
        val supportingEvidenceTycor = new SupportingEvidenceTycor(luceneIndex,5,fullPipeline)
        val idfScorer = new IdfScorer(luceneIndex)
        val weightedProminenceScorer =
            if (question.getAnnotationsWithin[OntologyEntityMention].size >1)
                WeightedContextScorer[Sentence](candidateSections,ConceptComparisonScorer)
            else
                WeightedContextScorer[Sentence](candidateSections,new StringSimilarityScorer(WordnetSimilarity))

        println("scoring answers...")
        answerCandidates.foreach(fa => {
            weightedProminenceScorer.score(fa)
            prominenceScorer.score(fa)
            UmlsTycor.score(fa)
        })
        var sortedCandidates = answerCandidates.toSeq.sortBy(a =>
            -FinalConceptScorer.scoreInternal(a))
        sortedCandidates = sortedCandidates.take(if(question.questionType == ListQ)  30 else 15)

        sortedCandidates.foreach(fa =>{
            idfScorer.score(fa)
            paragraphTycor.score(fa)
            supportingEvidenceTycor.score(fa)
            FinalConceptScorer.score(fa)
            println("scored: "+fa.answer.preferredLabel)
            fa.getScores.foreach(s => println("\t"+s._1.erasure.getSimpleName+"="+s._2))
        })

       /* val (maxScorer,averageScorer) =
            sortedCandidates.head.getScores.foldLeft((List[FactoidScorer](),List[FactoidScorer]())){
                case(acc,(manifest,_))=> {
                    (new MaxValueScorer(prominenceSortedCandidates)(manifest) :: acc._1,
                    new AverageValueScorer(prominenceSortedCandidates)(manifest) :: acc._2)
            } }
        sortedCandidates.foreach(fa => {
            maxScorer.foreach(_.score(fa))
            averageScorer.foreach(_.score(fa))
        }) */
        println("done")

        sortedCandidates.foreach(fa => {
            if (evalWriter ne null)
                evalWriter.processFactoid(fa)
        })

        sortedCandidates = sortedCandidates.sortBy(-_.score[FinalConceptScorer.type])

        val mappedCandidates = sortedCandidates.map(cand => (cand,cand.score[FinalConceptScorer.type])).toList
        if(question.questionType == FactoidQ)
            mappedCandidates.take(5)
        else
            mappedCandidates.take(3) ++ mappedCandidates.drop(3).takeWhile(_._2 >= -2.5)
    }

    def answerFactoidDecision(question:Question, pmids:List[Int]):List[(FactoidAnswer,Double)] = {
        List(answerListDecision(question,pmids).head)
    }

    def answerListDecision(question:Question, pmids:List[Int]):List[(FactoidAnswer,Double)] = {
        println("#########ANSWERS#############")

        // Extract candidate answers, exclude question concepts
        val answerCandidates = question.answerType.asInstanceOf[DecisionAnswerType].answerOptions
        //question -> answer skeletons -> fill with answers -> try to find patterns in documents

        val scorer = new DecisionAnswerPatternScorer(luceneIndex)

        answerCandidates.foreach(fa => scorer.score(fa))

        answerCandidates.sortBy(-_.score[DecisionAnswerPatternScorer]).map(fa => {
            val score = fa.score[DecisionAnswerPatternScorer]
            println(fa.answerText+"="+score)
            (fa,score)
        }).toList
    }

    def answerSummary(question:Question, pmids:List[Int]):List[(String,Double)] = {
        null
    }

    //########### Utilities ##############

    //should become an enhancer
    def enhanceConceptsWithSynonyms(answerTexts:Seq[AnnotatedText]) {
        answerTexts.flatMap(_.getAnnotations[OntologyEntityMention].
          flatMap(oem => oem.ontologyConcepts.map( (_,oem.coveredText) ))).
          groupBy(_._1).foreach{
            case (_,synonyms) => {
                val synSet = synonyms.map(s => {
                    s._2.split(" ").zipWithIndex.groupBy(_._1).toSeq.map(e => (e._1,e._2.minBy(_._2)._2)).sortBy(_._2).map(_._1).mkString(" ")
                }).toSet
                synonyms.foreach{
                    case (oc,_) => oc.synonyms = synSet
                }
            }
        }
    }

}
