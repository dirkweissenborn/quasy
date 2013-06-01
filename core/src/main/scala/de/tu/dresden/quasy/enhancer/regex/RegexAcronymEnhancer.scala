package de.tu.dresden.quasy.enhancer.regex

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{OntologyConcept, OntologyEntityMention, Token}

/**
 * @author dirk
 * Date: 5/16/13
 * Time: 2:31 PM
 */
object RegexAcronymEnhancer extends TextEnhancer{
    private final val pattern = """([A-Z][A-Z0-9\-\&/]+)|([b-df-hj-np-tv-xzB-DF-HJ-NP-TV-XZ][b-df-hj-np-tv-xzB-DF-HJ-NP-TV-Z0-9\-\&/]{3,})"""

    def enhance(text: AnnotatedText) {
        text.getAnnotations[Token].filter(t => t.posTag.matches(PosTag.ANYNOUN_PATTERN) && t.coveredText.matches(pattern)).foreach(t => {
            new OntologyEntityMention(t.begin,t.end,t.context,
                List(new OntologyConcept(OntologyConcept.SOURCE_ACRONYM,t.coveredText.toUpperCase,t.coveredText.toUpperCase,Set[String](),0.5)))
        })
    }
}
