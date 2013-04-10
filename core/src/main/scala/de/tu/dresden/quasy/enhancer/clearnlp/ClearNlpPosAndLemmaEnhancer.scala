package de.tu.dresden.quasy.enhancer.clearnlp

import de.tu.dresden.quasy.model.{AnnotatedText}
import com.googlecode.clearnlp.engine.{EngineProcess, EngineGetter}
import java.io.{FileNotFoundException, FileInputStream, File}
import com.googlecode.clearnlp.reader.AbstractReader
import de.tu.dresden.quasy.enhancer.TextEnhancer
import org.apache.commons.logging.LogFactory
import java.util.Properties
import de.tu.dresden.quasy.model.annotation.Sentence
import scala.collection.JavaConversions._

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 12:48 PM
 */
class ClearNlpPosAndLemmaEnhancer(val posModelFile:File, dictionaryFile:File, language: String = AbstractReader.LANG_EN) extends TextEnhancer {
    if (!posModelFile.exists() || posModelFile.isDirectory)
        throw new FileNotFoundException("No file found at "+ posModelFile.getAbsolutePath)
    if (!dictionaryFile.exists() || dictionaryFile.isDirectory)
        throw new FileNotFoundException("No file found at "+ dictionaryFile.getAbsolutePath)

    private val analyzer = EngineGetter.getMPAnalyzer(language, new FileInputStream(dictionaryFile))
    private val taggers = EngineGetter.getPOSTaggers(new FileInputStream(posModelFile))

    if (taggers == null)
        throw new InstantiationException("File "+posModelFile+" is not a pos tagging model!")

    def enhance(text: AnnotatedText) {
        text.getAnnotations[Sentence].foreach(sentence => {
            val tokens = sentence.getTokens
            val nodes = EngineProcess.getPOSNodesWithLemmas(taggers, analyzer, seqAsJavaList(tokens.map(_.coveredText)))

            tokens.zip(nodes).foreach {
                case(token,node) => {
                    token.posTag = node.pos
                    token.lemma = node.lemma
                }
            }
        })
    }

}

object ClearNlpPosAndLemmaEnhancer {
    val LOG =  LogFactory.getLog(getClass)

    final val POS_MODEL_PROPERTY_NAME = "enhancer.clearnlp.dependency.model"
    final val DICTIONARY_PROPERTY_NAME = ClearNlpSegmentationEnhancer.DICTIONARY_PROPERTY_NAME


    def fromConfiguration(properties:Properties):ClearNlpPosAndLemmaEnhancer = {
        var enhancer: ClearNlpPosAndLemmaEnhancer = null
        val modelPath = properties.getProperty(POS_MODEL_PROPERTY_NAME)
        if (modelPath!=null) {
            val dictionaryPath = properties.getProperty(DICTIONARY_PROPERTY_NAME)

            if (dictionaryPath != null) {
                enhancer = new ClearNlpPosAndLemmaEnhancer(new File(modelPath), new File(dictionaryPath))
            } else {
                enhancer = null
            }

        }

        if(enhancer == null) {
            LOG.error("Couldn't find clear-nlp pos model or dictionary! Please check your configuration for parameter "+POS_MODEL_PROPERTY_NAME+" and "+DICTIONARY_PROPERTY_NAME+"!")
            null
        }
        else
            enhancer
    }
}
