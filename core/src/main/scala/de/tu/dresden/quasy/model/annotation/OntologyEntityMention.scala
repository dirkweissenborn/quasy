package de.tu.dresden.quasy.model.annotation

import de.tu.dresden.quasy.model.{PosTag, Span, AnnotatedText}
import java.net.URL

/**
 * @author dirk
 * Date: 4/8/13
 * Time: 12:20 PM
 */
//Basically taken from ctakes
class OntologyEntityMention( spans:Array[Span],
                             context:AnnotatedText,
                          var ontologyConcepts:List[OntologyConcept])
    extends Annotation(spans,context) {

    def this(begin:Int, end:Int, context:AnnotatedText, ontologyConcepts:List[OntologyConcept]) = this(Array(new Span(begin,end)), context, ontologyConcepts)

    override def addToContext {
        if (!getTokens.forall(_.posTag.matches(PosTag.ANYVERB_PATTERN)))
            context.getAnnotations[OntologyEntityMention].find(_.spans.toList.equals(spans.toList)) match {
                case Some(em) => em.ontologyConcepts ++= ontologyConcepts
                case _ => super.addToContext
            }
    }

    override def toString = {
        coveredText +
       "["+ontologyConcepts.map(c => {
            c.source+"-"+c.preferredLabel
        }).mkString(",")+"]"
    }
}

class OntologyConcept(val source:String,
                      val conceptId:String,
                      val preferredLabel:String,
                      var synonyms:Set[String] = Set[String](),
                      val score:Double = 1.0,
                      val uri:URL = null) {
    synonyms += preferredLabel

    override def equals(obj:Any) = obj match {
        case that:OntologyConcept => this.source.equals(that.source) && this.conceptId.equals(that.conceptId)
        case _ => false
    }

    override def hashCode = (conceptId+source).hashCode

    override def toString =  conceptId +"-"+preferredLabel+"["+source+"]"
}

object OntologyConcept {
    val SOURCE_UMLS = "UMLS"
    val SOURCE_MESH = "MESH"
    val SOURCE_JOCHEM= "JOCHEM"
    val SOURCE_UNIPROT = "UNIPROT"
    val SOURCE_DOID = "DOID"
    val SOURCE_GO = "GO"
    val SOURCE_ACRONYM = "ACRONYM"
}

class UmlsConcept(conceptId:String,
                  preferredLabel:String,
                  synonyms:Set[String],
                  val semanticTypes:Set[String],
                  val meshTreeNrs:Set[String] = Set[String](),
                  score:Double = 1.0,
                  uri:URL = null) extends OntologyConcept(OntologyConcept.SOURCE_UMLS,conceptId,preferredLabel,synonyms,score,uri)



