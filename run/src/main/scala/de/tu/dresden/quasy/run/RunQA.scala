package de.tu.dresden.quasy.run

import java.io.{StringReader, FileInputStream, File}
import java.util.Properties
import de.tu.dresden.quasy.util.{LuceneIndex}
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation._
import de.tu.dresden.quasy.enhancer._
import clearnlp.{FullClearNlpPipeline, ClearNlpPosTagger}
import de.tu.dresden.quasy.answer.fetch.{BioASQPubMedFetcher, LuceneFetcher}
import de.tu.dresden.quasy.answer.filter.ContextFilter
import de.tu.dresden.quasy.similarity.{SimpleStringSimilarity, WordnetSimilarity}
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.enhancer.gopubmed.GopubmedAnnotator
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import de.tu.dresden.quasy.io.{LoadGoldStandards, AnnotatedTextSource}
import de.tu.dresden.quasy.enhancer.regex.RegexAcronymEnhancer
import pitt.search.lucene.PorterAnalyzer
import pitt.search.semanticvectors._
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import scala.Array
import de.tu.dresden.quasy.answer.tycor.{MeshTycor, UmlsTycor, LuceneTycor}
import umls.UmlsEnhancer
import de.tu.dresden.quasy.answer.score.context.{StringSimilarityScorer, ConceptComparisonScorer, ChunkComparisonScorer}
import de.tu.dresden.quasy.answer.AnswerQuestion

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:52 PM
 */
object RunQA {

    def main(args:Array[String]) {

        //println(tycor.matchInstanceToType("sequence", "rna"))
        //println(tycor.matchInstanceToType("sequence", "AATAAA"))
        //val q = "What is the most prominent sequence consensus for the polyadenylation site?"
        //val q ="What is the methyl donor of DNA (cytosine-5)-methyltransferases?"
        val q = "Tumors of which three organs are classically associated with the multiple endocrine neoplasia type 1 syndrome?"
        val text = new AnnotatedText(q)

        val goldAnswers = LoadGoldStandards.load(new File("corpus/questions.pretty.json"))
        val pmids = goldAnswers.find(_.body == q).get.answer.annotations.filter(_.`type` == "document").map(doc => doc.uri.substring(doc.uri.lastIndexOf("/")+1).toInt).toList

        val config = new File(args(0))

        val props = new Properties()
        props.load(new FileInputStream(config))
        val luceneIndex = LuceneIndex.fromConfiguration(props)

        val fullClearNlp = FullClearNlpPipeline.fromConfiguration(props)
        val chunker = OpenNlpChunkEnhancer.fromConfiguration(props)
        val fullPipeline = new EnhancementPipeline(List(fullClearNlp, chunker, RegexAcronymEnhancer, new OntologyEntitySelector(0.1))) //, new UniprotEnhancer, new DoidEnhancer, new GoEnhancer))//, new JochemEnhancer))

        fullPipeline.process(AnnotatedTextSource(text))
        UmlsEnhancer.enhance(text)
        new OntologyEntitySelector(0.1).enhance(text)
        new QuestionEnhancer(luceneIndex).enhance(text)

        val answerer = new AnswerQuestion(props)
        println("#########QUESTION#############")
        text.getAnnotations[Question].foreach(question => {
            question.questionType = FactoidQ
            answerer.answer(question,pmids)
        })

        luceneIndex.close
        System.exit(0)
    }
}
