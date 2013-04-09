package de.tu.dresden.quasy.clearnlp

import com.googlecode.clearnlp.engine.{EngineProcess, EngineGetter}
import com.googlecode.clearnlp.dependency.{DEPTree}
import com.googlecode.clearnlp.segmentation.AbstractSegmenter
import com.googlecode.clearnlp.reader.AbstractReader
import scala.collection.JavaConversions._
import java.io.{BufferedReader, StringReader}

/**
 * @author dirk
 * Date: 3/26/13
 * Time: 8:08 AM
 */
/**
 * @since 1.1.0
 * @author Jinho D. Choi ({ @code jdchoi77@gmail.com})
 */
object DemoDEPParser {
    def main(args: Array[String]) {
        val dictionaryFile: String = args(0)
        val posModelFile: String = args(1)
        val depModelFile: String = args(2)
        try {
            new DemoDEPParser(dictionaryFile, posModelFile, depModelFile)
        }
        catch {
            case e: Exception => {
                e.printStackTrace
            }
        }
    }
}

class DemoDEPParser(dictionaryFile: String, posModelFile: String, depModelFile: String) {

    private val language: String = AbstractReader.LANG_EN

    private val tokenizer = EngineGetter.getTokenizer(language, dictionaryFile)
    private val analyzer = EngineGetter.getMPAnalyzer(language, dictionaryFile)
    private val taggers = EngineGetter.getPOSTaggers(posModelFile)
    private val parser = EngineGetter.getDEPParser(depModelFile)

    def parse(text:String) :Seq[DEPTree] = {
        val segmenter: AbstractSegmenter = EngineGetter.getSegmenter(language, tokenizer)
        segmenter.getSentences(new BufferedReader(new StringReader(text))).map(tokens =>{
            EngineProcess.getDEPTree(taggers, analyzer, parser, tokens)
        } )
    }

}


