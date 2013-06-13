package de.tu.dresden.quasy.run

import java.io.{FileInputStream, File}
import java.util.Properties
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation._
import de.tu.dresden.quasy.enhancer._
import clearnlp.{FullClearNlpPipeline}
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import de.tu.dresden.quasy.io.{LoadGoldStandards, AnnotatedTextSource}
import de.tu.dresden.quasy.enhancer.regex.RegexAcronymEnhancer
import scala.Array
import umls.UmlsEnhancer
import de.tu.dresden.quasy.answer.AnswerQuestion
import de.tu.dresden.quasy.model.db.{MetaMapCache, ScoreCache, LuceneIndex}
import de.tu.dresden.quasy.answer.postprocess.{CacheUpdater, AnswerPostProcesserSet, GoldStandardCsvWriter}
import de.tu.dresden.quasy.answer.score.factoid.WeightedContextScorer

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:52 PM
 */
object RunQA {

    def main(args:Array[String]) {
        //TODO investigate: When is umls semantic type info useful and when not! (eg. not for type "methyl donor" but for "cancer")
        //println(tycor.matchInstanceToType("sequence", "rna"))
        //println(tycor.matchInstanceToType("sequence", "AATAAA"))
        //val q = "What is the most prominent sequence consensus for the polyadenylation site?"
        //val q = "What is the methyl donor of DNA (cytosine-5)-methyltransferases?"           //TODO check: here umls tycor hurts
        //val q = "Which antiepileptic drug is most strongly associated with spina bifida?"
        //val q = "In which isochores are Alu elements enriched?"
        //val q = "Is Rheumatoid Arthritis more common in men or women?"
        //val q = "Which acetylcholinesterase inhibitors are used for treatment of myasthenia gravis?"  //TODO Bioasq service doesn't give me all necessary docs
        //val q = "Which forms of cancer is the Tpl2 gene associated with?"   //TODO check: FinalScorer policy for summing paragraph and umls tycor hurts, paragraph tycor hurts e.g.: cancer metastasis --> metastasis is a cancer
        //val q = "Where in the cell do we find the protein Cep135?"
        //val q = "Which medication should be administered when managing patients with suspected acute opioid overdose?"
        //val q = "Which species may be used for the biotechnological production of itaconic acid?"

        /*val text = new AnnotatedText(q)

        val goldAnswers = LoadGoldStandards.load(new File("corpus/questions.pretty.json"))
        val pmids = goldAnswers.find(_.body == q).get.answer.annotations.filter(_.`type` == "document").map(doc => doc.uri.substring(doc.uri.lastIndexOf("/")+1).toInt).toList

        val config = new File(args(0))

        val props = new Properties()
        props.load(new FileInputStream(config))
        val luceneIndex = LuceneIndex.fromConfiguration(props)

        val fullClearNlp = FullClearNlpPipeline.fromConfiguration(props)
        val chunker = OpenNlpChunkEnhancer.fromConfiguration(props)
        val fullPipeline = new EnhancementPipeline(List(fullClearNlp, chunker, UmlsEnhancer,RegexAcronymEnhancer, new OntologyEntitySelector(0.1))) //, new UniprotEnhancer, new DoidEnhancer, new GoEnhancer))//, new JochemEnhancer))

        fullPipeline.process(AnnotatedTextSource(text))
        //new OntologyEntitySelector(0.1).enhance(text)
        new QuestionEnhancer(luceneIndex).enhance(text)

        val answerer = new AnswerQuestion(props)
        println("#########QUESTION#############")
        text.getAnnotations[Question].foreach(question => {
            question.questionType = FactoidQ
            answerer.answer(question,pmids)
        })

        luceneIndex.close */

        val config = new File(args(0))
        val questionsFile = new File(args(1))

        val props = new Properties()
        props.load(new FileInputStream(config))

        val cacheDir = new File(props.getProperty("cache.dir","./cache"))

        ScoreCache.loadCache(new File(cacheDir,questionsFile.getName+".scores"))
        MetaMapCache.loadCache(new File(cacheDir,questionsFile.getName+".mm"))

        val luceneIndex = LuceneIndex.fromConfiguration(props)

        val fullClearNlp = FullClearNlpPipeline.fromConfiguration(props)
        val chunker = OpenNlpChunkEnhancer.fromConfiguration(props)
        val fullPipeline = new EnhancementPipeline(List(fullClearNlp, chunker, UmlsEnhancer,RegexAcronymEnhancer, new OntologyEntitySelector(0.1))) //, new UniprotEnhancer, new DoidEnhancer, new GoEnhancer))//, new JochemEnhancer))

        val goldAnswers = LoadGoldStandards.load(questionsFile)
        var qas = Map[Question,Set[String]]()
        val questionEnhancer = new QuestionEnhancer(luceneIndex)

        val questions = goldAnswers.filter(_.`type`.matches("factoid|list")).map(qa => {
            val text = new AnnotatedText(qa.body)
            fullPipeline.process(AnnotatedTextSource(text))
            //new OntologyEntitySelector(0.1).enhance(text)
            questionEnhancer.enhance(text)

            text.getAnnotations[Question].foreach(q => {
                q.questionType = QuestionType.fromString(qa.`type`)
                qas += (q -> qa.answer.exact.toSet)
            })
            val pmids = qa.answer.annotations.filter(_.`type` == "document").map(doc => doc.uri.substring(doc.uri.lastIndexOf("/")+1).toInt).toList

            (text,pmids)
        })

        val evaluation = new AnswerPostProcesserSet(Set(
            new GoldStandardCsvWriter(qas,new File("./QA_"+System.currentTimeMillis()+".csv")),
            CacheUpdater)
            )
        val answerer = new AnswerQuestion(props,evaluation)

        questions.foreach{
            case (text,pmids) => {
                println("#########QUESTION#############")
                text.getAnnotations[Question].foreach(question => {
                    question.questionType = FactoidQ
                    answerer.answer(question,pmids)
                })
                ScoreCache.storeCache
                MetaMapCache.storeCache
            }
        }

        luceneIndex.close
        System.exit(0)
    }
}
