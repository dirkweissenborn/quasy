package de.tu.dresden.quasy.util

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{DepTag, UmlsConcept, SemanticRoleLabel, OntologyConcept}

/**
 * @author dirk
 * Date: 4/9/13
 * Time: 1:23 PM
 */
object AnnotatedTextXmlizer {

    val xstream = new XStream(new DomDriver())
    init

    def init {
        xstream.autodetectAnnotations(true)
        xstream.registerConverter(new ListConverter(xstream.getMapper))

        xstream.alias("span", classOf[Span])
        xstream.alias("ontologyConcept", classOf[OntologyConcept])
        xstream.alias("srl", classOf[SemanticRoleLabel])
        xstream.alias("umlsConcept", classOf[UmlsConcept])

        val clazz = classOf[de.tu.dresden.quasy.model.annotation.Annotation]
        xstream.useAttributeFor(clazz,"begin")
        xstream.useAttributeFor(clazz,"end")
        xstream.useAttributeFor(clazz,"coveredText")

        xstream.useAttributeFor(classOf[Span],"begin")
        xstream.useAttributeFor(classOf[Span],"end")

        xstream.useAttributeFor(classOf[DepTag],"dependsOn")
        xstream.useAttributeFor(classOf[DepTag],"tag")

        xstream.useAttributeFor(classOf[SemanticRoleLabel],"head")
        xstream.useAttributeFor(classOf[SemanticRoleLabel],"label")

    }

    def toXml(text:AnnotatedText) = xstream.toXML(text)

    def fromXml(xml:String) = xstream.fromXML(xml).asInstanceOf[AnnotatedText]

}
