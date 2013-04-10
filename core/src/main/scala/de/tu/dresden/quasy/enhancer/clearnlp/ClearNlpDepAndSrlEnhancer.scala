package de.tu.dresden.quasy.enhancer.clearnlp

import java.io.{FileInputStream, FileNotFoundException, File}
import com.googlecode.clearnlp.engine.{EngineProcess, EngineGetter}
import de.tu.dresden.quasy.model.{AnnotatedText}
import de.tu.dresden.quasy.util.ClearNlpHelper
import scala.collection.JavaConversions._
import de.tu.dresden.quasy.enhancer.TextEnhancer
import com.googlecode.clearnlp.predicate.AbstractPredIdentifier
import com.googlecode.clearnlp.dependency.srl.AbstractSRLabeler
import org.apache.commons.logging.LogFactory
import java.util.Properties
import de.tu.dresden.quasy.model.annotation.{SemanticRoleLabel, Sentence, DepTag}

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 2:45 PM
 */
class ClearNlpDepAndSrlEnhancer(val parserModelFile:File,val predModelFile:File = null, val srlModelFile:File = null) extends TextEnhancer {
    if (!parserModelFile.exists() || parserModelFile.isDirectory)
        throw new FileNotFoundException("No file found at "+ parserModelFile.getAbsolutePath)

    private val parser = EngineGetter.getDEPParser(new FileInputStream(parserModelFile))

    private var identifier:AbstractPredIdentifier =  null
    private var labeler:AbstractSRLabeler = null
    if(srlModelFile !=  null && srlModelFile.exists() && srlModelFile.isFile) {
        identifier = EngineGetter.getPredIdentifier(new FileInputStream(predModelFile))
        labeler =   EngineGetter.getSRLabeler(new FileInputStream(srlModelFile))
    }


    if (parser == null)
        throw new InstantiationException("File "+parserModelFile+" is not a pos tagging model!")

    def enhance(text: AnnotatedText) {
        text.getAnnotations[Sentence].foreach(sentence => {
            val tokens = sentence.getTokens
            val posNodes = ClearNlpHelper.toPOSNodes(tokens)
            val tree = EngineProcess.toDEPTree(posNodes)
            parser.parse(tree)

            if (labeler!=null) {
                identifier.identify(tree)
                tree.initSHeads
                labeler.label(tree)
            }

            tokens.zip(tree.tail).foreach {
                case(token,depNode) => {
                    token.depTag = new DepTag(depNode.getLabel,depNode.getHead.id)
                    if (labeler != null) {
                         val srls = depNode.getSHeads.foldLeft(List[SemanticRoleLabel]())((list,shead) => {
                             list ++ List(new SemanticRoleLabel(shead.getNode.id, shead.getLabel))
                         })
                        token.srls = srls
                    }
                }
            }
        })
    }

}

object ClearNlpDepAndSrlEnhancer {
    val LOG =  LogFactory.getLog(getClass)

    final val DEP_MODEL_PROPERTY_NAME = "enhancer.clearnlp.dependency.model"
    final val PRED_MODEL_PROPERTY_NAME = "enhancer.clearnlp.predicates.model"
    final val SRL_MODEL_PROPERTY_NAME = "enhancer.clearnlp.srl.model"


    def fromConfiguration(properties:Properties):ClearNlpDepAndSrlEnhancer = {
        var enhancer: ClearNlpDepAndSrlEnhancer = null
        val modelPath = properties.getProperty(DEP_MODEL_PROPERTY_NAME)
        if (modelPath!=null) {
            val predModelPath = properties.getProperty(PRED_MODEL_PROPERTY_NAME)

            if (predModelPath != null) {
                val srlModelPath = properties.getProperty(SRL_MODEL_PROPERTY_NAME)
                if (predModelPath != null)
                    enhancer = new ClearNlpDepAndSrlEnhancer(new File(modelPath), new File(predModelPath), new File(srlModelPath))
                else {
                    enhancer = new ClearNlpDepAndSrlEnhancer(new File(modelPath))
                    LOG.warn("Couldn't find clear-nlp SRL model! Please check your configuration for parameter "+SRL_MODEL_PROPERTY_NAME+"!")
                }
            } else {
                enhancer = new ClearNlpDepAndSrlEnhancer(new File(modelPath))
                LOG.warn("Couldn't find clear-nlp SRL model! Please check your configuration for parameter "+PRED_MODEL_PROPERTY_NAME+"!")
            }

            enhancer
        }
        else {
            LOG.error("Couldn't find clear-nlp dependency model! Please check your configuration for parameter "+DEP_MODEL_PROPERTY_NAME+"!")
            null
        }
    }
}
