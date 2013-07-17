package de.tu.dresden.quasy.model

/**
 * @author dirk
 *         Date: 4/15/13
 *         Time: 2:47 PM
 */
object PosTag {

    val ANYVERB_PATTERN = """VB.*"""
    val PLURALNOUN_PATTERN = """NNP?S"""
    val ANYNOUN_PATTERN = """NN.*"""
    val ANYADJECTIVE_PATTERN = """JJ.*"""

    val THIRD_PERSON_SGL_POSTAG = "VBZ"

    //###################################################

    val Email = "ADD"
    val Possessive_ending = "POS"
    val Aﬃx = "AFX"
    val Personal_pronoun = "PRP"
    val Coordinating_conjunction = "CC"
    val Possessive_pronoun = "PRP$"
    val Cardinal_number = "CD"
    val Adverb = "RB"
    val Code_ID = "CODE"
    val Adverb_comparative = "RBR"
    val Determiner = "DT"
    val Adverb_superlative = "RBS"
    val Existential_there = "EX"
    val Particle = "RP"
    val Foreign_word = "FW"
    val To = "TO"
    val Go_with = "GW"
    val Interjection = "UH"
    val Preposition_or_subordinating_conjunction = "IN"
    val Verb_base_form = "VB"
    val Adjective = "JJ"
    val Verb_past_tense = "VBD"
    val Adjective_comparative = "JJR"
    val Verb_gerund_or_present_participle = "VBG"
    val Adjective_superlative = "JJS"
    val Verb_past_participle = "VBN"
    val List_item_marker = "LS"
    val Verb_non_3rd_person_singular_present = "VBP"
    val Modal = "MD"
    val Verb_3rd_person_singular_present = "VBZ"
    val Noun_singular_or_mass = "NN"
    val Wh_determiner = "WDT"
    val Noun_plural = "NNS"
    val Wh_pronoun = "WP"
    val Proper_noun_singular = "NNP"
    val Possessive_wh_pronoun = "WP$"
    val Proper_noun_plural = "NNPS"
    val Wh_adverb = "WRB"
    val Predeterminer = "PDT"
    val Unknown = "XX"
}
