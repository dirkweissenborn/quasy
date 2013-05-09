package de.tu.dresden.quasy.similarity

import de.tu.dresden.quasy.model.annotation.{Sentence, Token}

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 6:59 PM
 */
class SemanticVectorsSimilarity extends SimilarityMeasure{
    def tokenSimilarity(token1: Token, token2: Token) = 0.0

    def phraseSimilarity(sentence1: Array[Token], sentence2: Array[Token]) = 0.0

    def sentenceSimilarity(sentence1: Sentence, sentence2: Sentence) = 0.0
}
