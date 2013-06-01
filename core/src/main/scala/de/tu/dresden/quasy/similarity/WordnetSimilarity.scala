package de.tu.dresden.quasy.similarity

import edu.cmu.lti.ws4j.util.WS4JConfiguration
import edu.cmu.lti.ws4j.impl.WuPalmer
import edu.cmu.lti.lexical_db.NictWordNet
import de.tu.dresden.quasy.QuasyFactory
import de.tu.dresden.quasy.model.annotation.{Sentence, Token}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 2:35 PM
 */
object WordnetSimilarity extends SimilarityMeasure{
    private val db = new NictWordNet

    def tokenSimilarity(word1: String, word2: String) = {
        WS4JConfiguration.getInstance.setMFS(true)
        val measure = new WuPalmer(db)
        math.min(measure.calcRelatednessOfWords(word1, word2),1.0)
    }

    def phraseSimilarity(sentence1: Array[String], sentence2: Array[String]) = {
        val cleanedS1 = sentence1.filterNot(word => QuasyFactory.stopWords.contains(word))
        val cleanedS2 = sentence2.filterNot(word => QuasyFactory.stopWords.contains(word))

        WS4JConfiguration.getInstance.setMFS(true)
        val measure = new WuPalmer(db)
        val matrix = measure.getSimilarityMatrix(cleanedS1, cleanedS2)

        if (matrix.size > 0 && matrix(0).size > 0)
            matrix.map(row => math.min(row.max,1.0)).sum / matrix.size
        else
            0.0
    }

    def phraseSimilarity(sentence1: Array[Token], sentence2: Array[Token]) = phraseSimilarity(sentence1.map(_.lemma), sentence2.map(_.lemma))

    def sentenceSimilarity(sentence1: Sentence, sentence2: Sentence) = phraseSimilarity(sentence1.getTokens.toArray, sentence2.getTokens.toArray)

    def tokenSimilarity(token1: Token, token2: Token) = tokenSimilarity(token1.lemma,token2.lemma)
}
