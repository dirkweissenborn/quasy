package de.tu.dresden.quasy.enhancer.stanford

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.AnnotatedText

import java.util.Properties
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation
import scala.collection.JavaConversions._
import de.tu.dresden.quasy.model.annotation.{Sentence, CoreferencedEntity}
import edu.stanford.nlp.dcoref.CorefChain

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 4:33 PM
 */
object CorefStanfordEnhancer extends TextEnhancer{
    private val props: Properties = new Properties
    props.put("annotators", "tokenize, ssplit, pos, lemma, parse, regexner, ner, dcoref")

    private val pipeline = new StanfordCoreNLP(props)

    protected def pEnhance(text: AnnotatedText) {
        val document = new Annotation(text.text)
        try {
            // run all Annotators on this text
            pipeline.annotate(document)

            val graph = document.get(classOf[CorefChainAnnotation])

            graph.foreach(chain => {
                val mentions=chain._2.getMentionsInTextualOrder
                if (mentions.size()>1) {
                    val representative = createCorefEntity(text,chain._2.getRepresentativeMention, null)
                    mentions.foreach(mention =>
                        if (!mention.equals(chain._2.getRepresentativeMention))
                            createCorefEntity(text,mention, representative)
                    )
                }
            })
        }
        catch {
            case e:Exception => //SHUT UP
        }
    }

    private def createCorefEntity(text:AnnotatedText,mention: CorefChain.CorefMention,reference: CoreferencedEntity)= {
        val sentence = text.getAnnotations[Sentence].get(mention.sentNum - 1)
        new CoreferencedEntity(
            sentence.getTokens.get(mention.startIndex - 1).begin,
            sentence.getTokens.get(mention.endIndex - 2).end,
            text,
            mention.mentionType.toString,
            reference)
    }
}
