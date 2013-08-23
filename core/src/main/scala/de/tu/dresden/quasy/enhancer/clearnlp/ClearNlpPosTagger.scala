package de.tu.dresden.quasy.enhancer.clearnlp

import java.io.{StringReader, BufferedReader, FileInputStream, File}
import com.googlecode.clearnlp.reader.AbstractReader
import de.tu.dresden.quasy.enhancer.TextEnhancer
import com.googlecode.clearnlp.tokenization.AbstractTokenizer
import com.googlecode.clearnlp.engine.EngineGetter
import com.googlecode.clearnlp.component.AbstractComponent
import com.googlecode.clearnlp.nlp.{NLPDecode, NLPLib}
import com.googlecode.clearnlp.segmentation.AbstractSegmenter
import scala.Array
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation._
import org.apache.commons.logging.LogFactory
import java.util.Properties
import scala.collection.JavaConversions._


/**
 * @author dirk
 * Date: 5/15/13
 * Time: 3:44 PM
 */
class ClearNlpPosTagger(val dictionaryFile:File,
                        val posModelFile:File,
                        language: String = AbstractReader.LANG_EN) extends TextEnhancer {

    private val tokenizer: AbstractTokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile))
    private val analyzer: AbstractComponent = EngineGetter.getComponent(new FileInputStream(dictionaryFile), language, NLPLib.MODE_MORPH)
    private val segmenter: AbstractSegmenter = EngineGetter.getSegmenter(language, tokenizer)
    private val tagger: AbstractComponent = EngineGetter.getComponent(new FileInputStream(posModelFile), language, NLPLib.MODE_POS)

    private val components: Array[AbstractComponent] = Array(tagger,analyzer)

    protected def pEnhance(text: AnnotatedText) {
        val textString = text.text
        var sectionOffset = 0

        textString.split("[\n\r]").foreach(sectionString => {
            if (sectionString != "") {
                sectionOffset =  textString.indexOf(sectionString, sectionOffset)
                new Section(sectionOffset,sectionOffset+sectionString.length,text)

                var sentenceOffset = 0

                segmenter.getSentences(new BufferedReader(new StringReader(sectionString))).map(tokenList => {
                    sentenceOffset =  sectionString.indexOf(tokenList.head, sentenceOffset)

                    val tree = NLPDecode.toDEPTree(tokenList)
                    components.foreach(component => component.process(tree) )

                    var tokenOffset = sentenceOffset
                    var position = 1
                    tokenList.zip(tree.tail).foreach{
                        case (tokenString,depNode) => {
                            tokenOffset = sectionString.indexOf(tokenString, tokenOffset)
                            val tokenStart: Int = sectionOffset + tokenOffset
                            val token = new Token( tokenStart, tokenStart+tokenString.length, text, position)

                            token.lemma = depNode.lemma
                            token.posTag = depNode.pos

                            tokenOffset += tokenString.length
                            position += 1
                        }}

                    val sentenceString = sectionString.substring(sentenceOffset, tokenOffset)
                    if (tokenList.last.equals("?"))
                        new Question(sectionOffset+sentenceOffset, sectionOffset+sentenceOffset+sentenceString.length,text)
                    else
                        new Sentence(sectionOffset+sentenceOffset, sectionOffset+sentenceOffset+sentenceString.length,text)

                    sentenceOffset += sentenceString.length
                } )
            }
            sectionOffset += sectionString.length
        })
    }
}

object ClearNlpPosTagger {
    var enhancer: ClearNlpPosTagger = null
    val LOG =  LogFactory.getLog(getClass)

    final val DICTIONARY_PROPERTY_NAME = "enhancer.clearnlp.dictionary"
    final val POS_MODEL_PROPERTY_NAME = "enhancer.clearnlp.pos.model"

    def fromConfiguration(properties:Properties):ClearNlpPosTagger = {
        if (enhancer == null) {
            val dictionaryPath = properties.getProperty(DICTIONARY_PROPERTY_NAME)
            val posModelPath = properties.getProperty(POS_MODEL_PROPERTY_NAME)

            if (dictionaryPath != null && posModelPath != null ) {

                enhancer = new ClearNlpPosTagger(new File(dictionaryPath),new File(posModelPath))
            }
            else {
                LOG.error("Couldn't find clear-nlp pos model! Please check your configuration for parameter "+DICTIONARY_PROPERTY_NAME+"!")
                null
            }
        }
        enhancer
    }
}

