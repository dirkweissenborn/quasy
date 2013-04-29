package de.tu.dresden.quasy.similarity

import edu.cmu.lti.lexical_db.NictWordNet
import edu.cmu.lti.ws4j.util.WS4JConfiguration
import edu.cmu.lti.ws4j.impl.WuPalmer

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 2:32 PM
 */
trait StringSimilarity {

    def wordSimilarity(word1: String, word2: String) : Double

    def sentenceSimilarity(sentence1: Array[String], sentence2: Array[String]) : Double
}
