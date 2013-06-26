package de.tu.dresden.quasy.enhancer.stanford

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.AnnotatedText
import java.util.Properties
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation
import scala.collection.JavaConversions._
import de.tu.dresden.quasy.model.annotation.{DepTag, Token, Sentence}
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 4:33 PM
 */
class FullStanfordNlpEnhancer extends TextEnhancer{
    private val props: Properties = new Properties
    props.put("annotators", "tokenize, ssplit, pos, lemma, parse, ner, dcoref")

    private val pipeline = new StanfordCoreNLP(props)

    protected def pEnhance(text: AnnotatedText) {
        val document = new Annotation(text.text)

        // run all Annotators on this text
        pipeline.annotate(document)


        val sentences = document.get(classOf[SentencesAnnotation])

        sentences.foreach(sentence => {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            var sentenceBegin = -1
            var sentenceEnd = -1
            // this is the Stanford dependency graph of the current sentence
            val dependencies = sentence.get(classOf[BasicDependenciesAnnotation])

            sentence.get(classOf[TokensAnnotation]).foreach( token => {
                if (sentenceBegin < 0)
                    sentenceBegin = token.beginPosition()

                sentenceEnd = token.endPosition()
                // this is the text of the token
                val quasyToken = new Token(token.beginPosition(),token.endPosition(),text,token.index())
                // this is the POS tag of the token
                quasyToken.posTag = token.tag()

                quasyToken.lemma = token.lemma().toLowerCase()
                val vertex = dependencies.getNodeByIndexSafe(token.index())
                if (dependencies.containsVertex(vertex)) {
                    val parents = dependencies.getIncomingEdgesSorted(vertex)
                    if (!parents.isEmpty)
                        quasyToken.depTag = new DepTag(parents.head.getRelation.getShortName,parents.head.getSource.index())
                    else
                        quasyToken.depTag = new DepTag("root",0)
                }
                quasyToken
            } )
            new Sentence(sentenceBegin,sentenceEnd,text)

        })

        var graph = document.get(classOf[CorefChainAnnotation])
        graph
    }
}
