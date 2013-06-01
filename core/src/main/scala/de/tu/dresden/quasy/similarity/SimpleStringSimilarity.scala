package de.tu.dresden.quasy.similarity

import de.tu.dresden.quasy.QuasyFactory
import de.tu.dresden.quasy.model.annotation.{Sentence, Token}

/**
 * @author dirk
 * Date: 4/30/13
 * Time: 5:21 PM
 */
object SimpleStringSimilarity extends SimilarityMeasure{
    def tokenSimilarity(word1: String, word2: String) = if (word1.equals(word2)) 1.0 else 0.0

    def phraseSimilarity(sentence1: Array[String], sentence2: Array[String]) = {
        val cleanedS1 = sentence1.filterNot(word => QuasyFactory.stopWords.contains(word))
        val cleanedS2 = sentence2.filterNot(word => QuasyFactory.stopWords.contains(word))

        val normConst = cleanedS1.size
        if (normConst == 0)
            0.0
        else
            cleanedS1.intersect(cleanedS2).length.toDouble / normConst
    }

    def phraseSimilarity(sentence1: Array[Token], sentence2: Array[Token]) = phraseSimilarity(sentence1.map(_.lemma), sentence2.map(_.lemma))

    def sentenceSimilarity(sentence1: Sentence, sentence2: Sentence) = phraseSimilarity(sentence1.getTokens.toArray, sentence2.getTokens.toArray)

    def tokenSimilarity(token1: Token, token2: Token) = tokenSimilarity(token1.lemma,token2.lemma)
}

