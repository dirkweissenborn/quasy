package de.tu.dresden.quasy.enhancer.clearnlp

import java.io.{StringReader, BufferedReader, FileInputStream, File}
import com.googlecode.clearnlp.reader.AbstractReader
import com.googlecode.clearnlp.tokenization.AbstractTokenizer
import com.googlecode.clearnlp.engine.EngineGetter
import com.googlecode.clearnlp.component.AbstractComponent
import com.googlecode.clearnlp.nlp.{NLPDecode, NLPLib}
import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation._
import com.googlecode.clearnlp.segmentation.AbstractSegmenter
import scala.collection.JavaConversions._
import java.util.Properties
import org.apache.commons.logging.LogFactory

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 5:12 PM
 */
class FullClearNlpPipeline(val dictionaryFile:File,
                           val posModelFile:File,
                           val depModelFile:File,
                           val predModelFile:File,
                           val srlModelFile:File,
                           val roleModelFile:File,
                           val vnModelFile:File,
                           language: String = AbstractReader.LANG_EN) extends TextEnhancer {

    private val tokenizer: AbstractTokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile))
    private val tagger: AbstractComponent = EngineGetter.getComponent(new FileInputStream(posModelFile), language, NLPLib.MODE_POS)
    private val analyzer: AbstractComponent = EngineGetter.getComponent(new FileInputStream(dictionaryFile), language, NLPLib.MODE_MORPH)
    private val parser: AbstractComponent = EngineGetter.getComponent(new FileInputStream(depModelFile), language, NLPLib.MODE_DEP)
    private val identifier: AbstractComponent = EngineGetter.getComponent(new FileInputStream(predModelFile), language, NLPLib.MODE_PRED)
    private val classifier: AbstractComponent = EngineGetter.getComponent(new FileInputStream(roleModelFile), language, NLPLib.MODE_ROLE)
    private val verbnet: AbstractComponent = EngineGetter.getComponent(new FileInputStream(vnModelFile), language, NLPLib.MODE_SENSE + "_vn")
    private val labeler: AbstractComponent = EngineGetter.getComponent(new FileInputStream(srlModelFile), language, NLPLib.MODE_SRL)
    private val segmenter: AbstractSegmenter = EngineGetter.getSegmenter(language, tokenizer)

    private val components: Array[AbstractComponent] = Array(tagger, analyzer, parser, identifier, classifier, verbnet, labeler)

    def enhance(text: AnnotatedText) {
        val textString = text.text
        var sectionOffset = 0

        textString.split("[\n\r]").foreach(sectionString => {
            if (sectionString != "") {
                sectionOffset =  textString.indexOf(sectionString, sectionOffset)
                new Section(sectionOffset,sectionOffset+sectionString.length,text)

                var sentenceOffset = 0

                segmenter.getSentences(new BufferedReader(new StringReader(sectionString))).map(tokenList => {
                    sentenceOffset =  sectionString.indexOf(tokenList.head, sentenceOffset)

                    val nlp = new NLPDecode()
                    val tree = nlp.toDEPTree(tokenList)
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
                            token.depTag = new DepTag(depNode.getLabel,depNode.getHead.id)
                            if (labeler != null) {
                                val srls = depNode.getSHeads.foldLeft(List[SemanticRoleLabel]())((list,shead) => {
                                    list ++ List(new SemanticRoleLabel(shead.getNode.id, shead.getLabel))
                                })
                                token.srls = srls
                            }

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

object FullClearNlpPipeline{
    var enhancer: FullClearNlpPipeline = null
    val LOG =  LogFactory.getLog(getClass)

    final val POS_MODEL_PROPERTY_NAME = "enhancer.clearnlp.pos.model"
    final val DICTIONARY_PROPERTY_NAME = "enhancer.clearnlp.dictionary"
    final val DEP_MODEL_PROPERTY_NAME = "enhancer.clearnlp.dependency.model"
    final val PRED_MODEL_PROPERTY_NAME = "enhancer.clearnlp.predicates.model"
    final val SRL_MODEL_PROPERTY_NAME = "enhancer.clearnlp.srl.model"
    final val ROLE_MODEL_PROPERTY_NAME = "enhancer.clearnlp.role.model"
    final val SENSE_MODEL_PROPERTY_NAME = "enhancer.clearnlp.sense.model"


    def fromConfiguration(properties:Properties):FullClearNlpPipeline = {
        if (enhancer == null) {
            val posModelPath = properties.getProperty(POS_MODEL_PROPERTY_NAME)
            val dictionaryPath = properties.getProperty(DICTIONARY_PROPERTY_NAME)
            val depModelPath = properties.getProperty(DEP_MODEL_PROPERTY_NAME)
            val srlModelPath = properties.getProperty(SRL_MODEL_PROPERTY_NAME)
            val predModelPath = properties.getProperty(PRED_MODEL_PROPERTY_NAME)
            val roleModelPath = properties.getProperty(ROLE_MODEL_PROPERTY_NAME)
            val senseModelPath = properties.getProperty(SENSE_MODEL_PROPERTY_NAME)

            if (  posModelPath != null &&
                    dictionaryPath != null &&
                    depModelPath != null &&
                    srlModelPath != null &&
                    roleModelPath != null &&
                    senseModelPath != null &&
                    predModelPath != null) {

                enhancer = new FullClearNlpPipeline(
                    new File(dictionaryPath),
                    new File(posModelPath),
                    new File(depModelPath),
                    new File(predModelPath),
                    new File(srlModelPath),
                    new File(roleModelPath),
                    new File(senseModelPath))
            }
            else {
                LOG.error("Couldn't find clear-nlp dependency model! Please check your configuration for parameter "+DEP_MODEL_PROPERTY_NAME+"!")
                null
            }
        }
        enhancer
    }
}
