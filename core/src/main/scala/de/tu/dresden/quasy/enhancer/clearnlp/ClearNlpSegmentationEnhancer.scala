package de.tu.dresden.quasy.enhancer.clearnlp

import com.googlecode.clearnlp.reader.AbstractReader
import java.io._
import com.googlecode.clearnlp.engine.EngineGetter
import de.tu.dresden.quasy.model.{AnnotatedText}
import com.googlecode.clearnlp.segmentation.AbstractSegmenter
import scala.collection.JavaConversions._
import de.tu.dresden.quasy.enhancer.TextEnhancer
import org.apache.commons.logging.LogFactory
import java.util.Properties
import de.tu.dresden.quasy.model.annotation.{Token, Sentence, Section}

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 1:04 PM
 */
class ClearNlpSegmentationEnhancer(dictionaryFile:File, language: String = AbstractReader.LANG_EN) extends TextEnhancer{
    if (!dictionaryFile.exists() || dictionaryFile.isDirectory)
        throw new FileNotFoundException("No file found at "+ dictionaryFile.getAbsolutePath)

    private val tokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile))
    private val segmenter: AbstractSegmenter = EngineGetter.getSegmenter(language, tokenizer)

    if (tokenizer == null || segmenter == null)
        throw new InstantiationException("File "+dictionaryFile.getAbsolutePath+" is not a clearnlp lexicon file!")

    def enhance(text: AnnotatedText) {
        val textString = text.text
        var sectionOffset = 0

        textString.split("[\n\r]").foreach(sectionString => {
            if (sectionString != "") {
                val section = new Section(sectionOffset,sectionOffset+sectionString.length,text)

                var sentenceOffset = 0
                segmenter.getSentences(new BufferedReader(new StringReader(sectionString))).map(tokens =>{
                    var sentenceString = sectionString.substring(sentenceOffset, sectionString.indexOf(tokens.last, sentenceOffset)+1)

                    while(sentenceString.startsWith(" ")) {
                        sentenceString = sentenceString.substring(1)
                        sentenceOffset += 1
                    }

                    new Sentence(sectionOffset+sentenceOffset, sectionOffset+sentenceOffset+sentenceString.length,text)

                    var tokenOffset = 0
                    var position = 1
                    tokens.foreach( tokenString => {
                        tokenOffset = sentenceString.indexOf(tokenString, tokenOffset)
                        val tokenStart: Int = sectionOffset + sentenceOffset + tokenOffset
                        new Token( tokenStart, tokenStart+tokenString.length, text, position)

                        tokenOffset += tokenString.length
                        position += 1
                    })

                    sentenceOffset += sentenceString.length
                } )
            }
            sectionOffset += 1 + sectionString.length
        })
    }
}

object ClearNlpSegmentationEnhancer {
    val LOG =  LogFactory.getLog(getClass)

    final val DICTIONARY_PROPERTY_NAME = "enhancer.clearnlp.dictionary"

    def fromConfiguration(properties:Properties):ClearNlpSegmentationEnhancer = {
        val modelPath = properties.getProperty(DICTIONARY_PROPERTY_NAME)
        if (modelPath!=null)
            new ClearNlpSegmentationEnhancer(new File(modelPath))
        else {
            LOG.error("Couldn't find clear-nlp dictionary! Please check your configuration for parameter "+DICTIONARY_PROPERTY_NAME+"!")
            null
        }
    }
}
