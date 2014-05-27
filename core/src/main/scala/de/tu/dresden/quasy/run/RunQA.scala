package de.tu.dresden.quasy.run

import java.io.{PrintWriter, FileWriter, FileInputStream, File}
import java.util.Properties
import de.tu.dresden.quasy.model.annotation._
import de.tu.dresden.quasy.enhancer._
import clearnlp.FullClearNlpPipeline
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import de.tu.dresden.quasy.io.{LoadGoldStandards, AnnotatedTextSource}
import de.tu.dresden.quasy.enhancer.regex.RegexAcronymEnhancer
import scala.Array
import de.tu.dresden.quasy.enhancer.umls.{UmlsEnhancerOld, UmlsEnhancer}
import de.tu.dresden.quasy.answer.AnswerQuestion
import de.tu.dresden.quasy.model.db.{AnnotationCache, ScoreCache, LuceneIndex}
import de.tu.dresden.quasy.answer.postprocess.{CacheUpdater, AnswerPostProcesserSet, GoldStandardCsvWriter}
import org.apache.lucene.util.Version
import org.apache.commons.logging.LogFactory

/**
 * @author dirk
 *         Date: 4/25/13
 *         Time: 4:52 PM
 */
object RunQA {

  private val LOG = LogFactory.getLog(getClass())

  def main(args: Array[String]) {
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
    val resultFile = new File(args(1) + ".result")
    val exists = resultFile.exists()

    val outputWriter = new PrintWriter(new FileWriter(resultFile, true))

    val props = new Properties()
    props.load(new FileInputStream(config))

    val cacheDir = new File(props.getProperty("cache.dir", "./cache"))
    cacheDir.mkdirs()

    //ScoreCache.loadCache(new File(cacheDir, questionsFile.getName + ".scores"))
    //AnnotationCache.loadCache(new File(cacheDir, questionsFile.getName + ".annots"))

    val luceneIndex = LuceneIndex.fromConfiguration(props, Version.LUCENE_41)

    val fullClearNlp = FullClearNlpPipeline.fromConfiguration(props)
    val chunker = OpenNlpChunkEnhancer.fromConfiguration(props)
    val fullPipeline = new EnhancementPipeline(List(fullClearNlp, chunker, UmlsEnhancer, RegexAcronymEnhancer, new OntologyEntitySelector(0.1))) //, new UniprotEnhancer, new DoidEnhancer, new GoEnhancer))//, new JochemEnhancer))

    var goldAnswers = LoadGoldStandards.load(questionsFile)

    if (!exists) {
      outputWriter.print("{\"system\": \"BioASQ_Baseline_1bB\", \"username\": \"gbt\", \"password\": \"gr1979\", \n\"questions\": [")
      outputWriter.flush()
    } else {
      val answers = LoadGoldStandards.loadAnswers(resultFile)
      goldAnswers = goldAnswers.filterNot(g => answers.exists(_.id == g.id))
    }


    var qas = Map[Question, Set[String]]()
    val questionEnhancer = new QuestionEnhancer(luceneIndex)

    val texts = goldAnswers.filter(_.`type`.matches("factoid|list")).flatMap(qa => {
      val t = if (qa.body.startsWith("List "))
        qa.body.substring(0, qa.body.length - 1) + "?"
      else
        qa.body
      val text = AnnotationCache.getCachedAnnotatedText(t, qa.id)

      try {
        fullPipeline.process(AnnotatedTextSource(text))
        //new OntologyEntitySelector(0.1).enhance(text)
        questionEnhancer.enhance(text)

        text.getAnnotations[Question].foreach(question => question.questionType = QuestionType.fromString(qa.`type`))

        text.getAnnotations[Question].foreach(q => {
          q.questionType = QuestionType.fromString(qa.`type`)
          //if(qa.exact_answer != null)
          //qas += (q -> qa.exact_answer.toSet)
        })
        Some((qa, text))
      }
      catch {
        case t: Throwable => LOG.error(t.getMessage+": "+t); None
      }
    })

    val evaluation = new AnswerPostProcesserSet(
      if (qas.isEmpty)
        Set(CacheUpdater)
      else
        Set(new GoldStandardCsvWriter(qas, new File("./QA_" + System.currentTimeMillis() + ".csv")),
          CacheUpdater)
    )
    val answerer = new AnswerQuestion(props, evaluation)

    texts.foreach {
      case (qa, text) =>
        val pmids = if (qa.documents ne null)
          qa.documents.map(doc => doc.substring(doc.lastIndexOf("/") + 1).toInt).toList.take(10)
        else
          null

        //Should be one
        text.getAnnotations[Question].foreach(question => {
          println("#########QUESTION#############")
          outputWriter.println("{ \"id\": \"" + qa.id + "\",")

          answerer.answer(question, pmids) match {
            case Left(factoidAnswers) =>
              outputWriter.println("\"exact_answer\": [" +
                factoidAnswers.map(
                  a => "[" + {
                    if (a._1.answer != null)
                      (a._1.answer.synonyms.toSet ++ Set(a._1.answerText)).map(t => "\"" + t + "\"").mkString(",")
                    else
                      "\"" + a._1.answerText + "\""
                  } + "]").mkString(",") + "]}")
              if (!texts.last.equals(text))
                outputWriter.print(",")

            case Right(answers) =>
          }
          outputWriter.flush()
        })

        //ScoreCache.storeCache
        //AnnotationCache.storeCache
    }

    luceneIndex.close
    outputWriter.println("]}")
    outputWriter.close()
    System.exit(0)
  }
}

/*
  { id: 5118dd1305c10fae75000001,
  type: 'factoid',
  body: 'Is Rheumatoid Arthritis more common in men or women?',
  answer:
   { ideal: 'Disease patterns in RA vary between the sexes; the condition is more commonly seen in women, who exhibit a more aggressive disease and a poorer long-term outcome.',
     exact: 'Women'
*/