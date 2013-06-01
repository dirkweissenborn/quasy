package de.tu.dresden.quasy.similarity

import de.tu.dresden.quasy.QuasyFactory
import org.hua.sr.{SRWS, SRWSService}
import de.tu.dresden.quasy.model.annotation.{Sentence, Token}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 2:35 PM
 */
object OmiotisSimilarity extends SimilarityMeasure {
    private var service:SRWSService = null
    private var port: SRWS = null

    init

    def init {
        service = new SRWSService()
        port = service.getSRWSPort
    }

    def phraseSimilarity(sentence1: Array[String], sentence2: Array[String]):Double = {
        val cleanedS1 = sentence1.filterNot(word => QuasyFactory.stopWords.contains(word))
        val cleanedS2 = sentence2.filterNot(word => QuasyFactory.stopWords.contains(word))

        try {
            return port.phraseRelatedness(cleanedS1.mkString(" "),cleanedS2.mkString(" "))
        }
        catch {
            case e => init; return port.phraseRelatedness(cleanedS1.mkString(" "),cleanedS2.mkString(" "))
        }

        0.0
    }

    def tokenSimilarity(word1: String, word2: String):Double = {
        try {
            return port.termRelatedness(word1,word2)
        }
        catch {
            case e => init; return port.termRelatedness(word1,word2)
        }

        0.0
    }

    def phraseSimilarity(sentence1: Array[Token], sentence2: Array[Token]) = phraseSimilarity(sentence1.map(_.lemma), sentence2.map(_.lemma))

    def sentenceSimilarity(sentence1: Sentence, sentence2: Sentence) = phraseSimilarity(sentence1.getTokens.toArray, sentence2.getTokens.toArray)

    def tokenSimilarity(token1: Token, token2: Token) = tokenSimilarity(token1.lemma,token2.lemma)
}
