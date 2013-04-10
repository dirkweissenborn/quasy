package de.tu.dresden.quasy.enhancer.opennlp

import java.io.{FileInputStream, File}
import opennlp.tools.chunker.{Chunker, ChunkerME, ChunkerModel}
import org.apache.commons.logging.LogFactory
import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{AnnotatedText}
import java.util.Properties
import de.tu.dresden.quasy.model.annotation.Sentence


/**
 * @author dirk
 * Date: 4/2/13
 * Time: 10:11 AM
 */
class OpenNlpChunkEnhancer(val modelFile:File) extends TextEnhancer{
    val LOG =  LogFactory.getLog(getClass)
    private var chunker:Chunker = null

    try {
        val fis = new FileInputStream(modelFile)
        val model = new ChunkerModel(fis)
        val chunkerModelAbsPath = modelFile.getAbsolutePath
        LOG.info("Chunker model file: " + chunkerModelAbsPath)
        chunker = new ChunkerME(model)
    }
    catch {
        case e: Exception => {
            LOG.error("Chunker model: " + modelFile.getAbsolutePath+" could not be initialized")
            throw new ExceptionInInitializerError(e)
        }
    }

    def enhance(text: AnnotatedText) {
        text.getAnnotations[Sentence].foreach(sentence => {
            val tokens = sentence.getTokens
            val tags = sentence.getTokens.map(_.posTag)

            val chunks = chunker.chunk(tokens.map(_.coveredText).toArray,tags.toArray)
            //TODO
        })
    }
}

object OpenNlpChunkEnhancer {
    val LOG =  LogFactory.getLog(getClass)

    final val MODEL_PROPERTY_NAME = "enhancer.opennlp.chunk.model"

    def fromConfiguration(properties:Properties):OpenNlpChunkEnhancer = {
        val modelPath = properties.getProperty(MODEL_PROPERTY_NAME)
       if (modelPath!=null)
           new OpenNlpChunkEnhancer(new File(modelPath))
       else {
           LOG.error("Couldn't find opennlp chunker model! Please check your configuration for parameter "+MODEL_PROPERTY_NAME+"!")
           null
       }
    }
}
