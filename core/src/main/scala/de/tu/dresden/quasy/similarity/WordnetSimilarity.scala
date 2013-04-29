package de.tu.dresden.quasy.similarity

import edu.cmu.lti.ws4j.util.WS4JConfiguration
import edu.cmu.lti.ws4j.impl.WuPalmer
import edu.cmu.lti.lexical_db.NictWordNet

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 2:35 PM
 */
object WordnetSimilarity extends StringSimilarity{
    private val db = new NictWordNet

    def wordSimilarity(word1: String, word2: String) = {
        WS4JConfiguration.getInstance.setMFS(true)
        val measure = new WuPalmer(db)
        measure.calcRelatednessOfWords(word1, word2)
    }

    def sentenceSimilarity(sentence1: Array[String], sentence2: Array[String]) = {
        WS4JConfiguration.getInstance.setMFS(true)
        val measure = new WuPalmer(db)
        val matrix = measure.getSimilarityMatrix(sentence1, sentence2)

        matrix.map(row => row.max).sum / matrix.size
    }
}
