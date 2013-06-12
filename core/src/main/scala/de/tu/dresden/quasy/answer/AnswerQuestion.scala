package de.tu.dresden.quasy.answer

import de.tu.dresden.quasy.model.annotation._
import eval.EvalWriter
import fetch.{NlmPubmedFetcher, BioASQPubMedFetcher}
import model.{FactoidAnswer}

import java.util.Properties
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.io.AnnotatedTextSource
import de.tu.dresden.quasy.enhancer.umls.UmlsEnhancer
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

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 11:47 AM
 */
class AnswerQuestion(config:Properties,evalWriter:EvalWriter = null) {
    private val luceneIndex = LuceneIndex.fromConfiguration(config)
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
        println(question.getDependencyTree.prettyPrint)
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
        List(answerList(question,pmids).head)
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
            UmlsTycor.score(fa)
        })
        val prominenceSortedCandidates = answerCandidates.toSeq.sortBy(a => -a.score[WeightedContextScorer[Sentence]]-a.score[UmlsTycor.type]).take(50)

        prominenceSortedCandidates.foreach(fa => {
            idfScorer.score(fa)
            prominenceScorer.score(fa)
            paragraphTycor.score(fa)
            supportingEvidenceTycor.score(fa)
            //FinalConceptScorer.score(fa)
            println("scored: "+fa.answer.preferredLabel)
            fa.getScores.foreach(s => println("\t"+s._1.erasure.getSimpleName+"="+s._2))
        })

        val (maxScorer,averageScorer) =
            prominenceSortedCandidates.head.getScores.foldLeft((List[FactoidScorer](),List[FactoidScorer]())){
                case(acc,(manifest,_))=> {
                    (new MaxValueScorer(prominenceSortedCandidates)(manifest) :: acc._1,
                    new AverageValueScorer(prominenceSortedCandidates)(manifest) :: acc._2)
            } }
        prominenceSortedCandidates.foreach(fa => {
            maxScorer.foreach(_.score(fa))
            averageScorer.foreach(_.score(fa))
        })
        println("done")

        prominenceSortedCandidates.foreach(fa => {
            if (evalWriter ne null)
                evalWriter.writeFactoidEval(fa)
        })

        val sortedCandidates = prominenceSortedCandidates.sortBy(-_.score[FinalConceptScorer.type])

        /*
        println()
        sortedCandidates.foreach(fa => {
            if (evalWriter ne null)
                evalWriter.writeFactoidEval(fa)
            println(fa.answer.conceptId+"-"+fa.answer.preferredLabel+"="+fa.score[FinalConceptScorer.type])
        })
        */

        sortedCandidates.map(cand => (cand,cand.score[FinalConceptScorer.type])).toList
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
                val synSet = synonyms.map(_._2).toSet
                synonyms.foreach{
                    case (oc,_) => oc.synonyms = synSet
                }
            }
        }
    }

}
