package de.tu.dresden.quasy.run

import java.io.{FileInputStream, File}
import java.util.Properties
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Question, Sentence}
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline, NounalRelationEnhancer}
import de.tu.dresden.quasy.enhancer.clearnlp.FullClearNlpPipeline
import de.tu.dresden.quasy.answer.fetch.BioASQPmcFetcher
import de.tu.dresden.quasy.answer.filter.CandidateFilter
import de.tu.dresden.quasy.answer.score.{ConceptComparisonScorer, StringSimilarityScorer, SRLScorer}
import de.tu.dresden.quasy.similarity.{SimpleStringSimilarity, WordnetSimilarity}
import de.tu.dresden.quasy.QuasyFactory
import de.tu.dresden.quasy.enhancer.stanford.CorefStanfordEnhancer
import de.tu.dresden.quasy.answer.model.AnswerCandidate

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:52 PM
 */
object RunQA {

    def main(args:Array[String]) {
        if (args.size < 2) {
            throw new IllegalArgumentException("corpus-dir (1st argument) and the configuration-file (2nd) must be specified!")
        }
        val corpusDir = new File(args(0))
        val config = new File(args(1))

        val props = new Properties()
        props.load(new FileInputStream(config))
        val clearNlpPipeLine = FullClearNlpPipeline.fromConfiguration(props)

        val fetcher = new BioASQPmcFetcher(new EnhancementPipeline(clearNlpPipeLine,NounalRelationEnhancer))

        val simpleFilter = new CandidateFilter(new SRLScorer(SimpleStringSimilarity))
        val wordNetFilter = new CandidateFilter(new SRLScorer(WordnetSimilarity))

        val comparisonFilter = new CandidateFilter(ConceptComparisonScorer)

        corpusDir.listFiles.foreach(file => {
            val text= Xmlizer.fromFile[AnnotatedText](file)
            NounalRelationEnhancer.enhance(text)
            println("#########QUESTION#############")
            text.getAnnotations[Question].foreach(question => {
                println(text.id + "\t" + question.coveredText)
                println(question.printRoleLabels)
                println(question.getDependencyTree.prettyPrint)
                println()

                val candidateSections = fetcher.fetch(question,10)

                val candidateSentences = comparisonFilter.filter(candidateSections,10).flatMap(candidateSection => {
                    val sentences = candidateSection.answer.context.getAnnotationsBetween[Sentence](candidateSection.answer.begin,candidateSection.answer.end)
                    sentences.map(s => new AnswerCandidate(s,candidateSection.question))
                })

                println("#########ANSWERS#############")

                wordNetFilter.filter(simpleFilter.filter(comparisonFilter.filter(candidateSentences,0.67),20),10).foreach(candidate => {
                    println(candidate.answer.context.id + "\t"+candidate.answer.coveredText)
                    println(candidate.scores.map((entry) => entry._1.erasure.getSimpleName+"->"+entry._2).mkString("\n"))

                    candidate.answer.context.getAnnotationsBetween[Sentence](candidate.answer.begin,candidate.answer.end).foreach(sentence => {
                        println(sentence.printRoleLabels)
                        println(sentence.getDependencyTree.prettyPrint)
                        println()
                    })
                })
            })
        })

    }


}
