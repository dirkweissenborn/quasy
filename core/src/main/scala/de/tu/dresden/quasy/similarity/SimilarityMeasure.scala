package de.tu.dresden.quasy.similarity

import edu.cmu.lti.lexical_db.NictWordNet
import edu.cmu.lti.ws4j.util.WS4JConfiguration
import edu.cmu.lti.ws4j.impl.WuPalmer
import de.tu.dresden.quasy.model.annotation.{Token, Sentence}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 2:32 PM
 */
trait SimilarityMeasure {

    def tokenSimilarity(token1: Token, token2: Token) : Double

    def phraseSimilarity(sentence1: Array[Token], sentence2: Array[Token]) : Double

    def sentenceSimilarity(sentence1: Sentence, sentence2: Sentence):Double
}
