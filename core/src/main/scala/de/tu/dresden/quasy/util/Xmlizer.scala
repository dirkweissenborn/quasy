package de.tu.dresden.quasy.util

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation._
import java.io.{FileWriter, File}
import io.Source
import opennlp.tools.util.InvalidFormatException

/**
 * @author dirk
 * Date: 4/9/13
 * Time: 1:23 PM
 */
object Xmlizer {

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
        xstream.useAttributeFor(clazz,"coveredText")

        xstream.useAttributeFor(classOf[Span],"begin")
        xstream.useAttributeFor(classOf[Span],"end")

        xstream.useAttributeFor(classOf[DepTag],"dependsOn")
        xstream.useAttributeFor(classOf[DepTag],"tag")

        xstream.useAttributeFor(classOf[SemanticRoleLabel],"head")
        xstream.useAttributeFor(classOf[SemanticRoleLabel],"label")

        xstream.useAttributeFor(classOf[AnnotatedText],"id")

        xstream.omitField(clazz,"tokens")

        xstream.omitField(classOf[Sentence],"dependencyTree")

        xstream.omitField(classOf[DepTag],"headToken")
        xstream.omitField(classOf[Token],"sentence")
        xstream.omitField(classOf[Token],"depDepth")

        xstream.omitField(classOf[Any],"bitmap$0")
    }

    def toXml(obj:Any) = xstream.toXML(obj)

    def fromXml[T](xml:String):T =
            xstream.fromXML(xml).asInstanceOf[T]


    def fromFile[T](file:File) = {
        try {
            val xml = Source.fromFile(file).getLines().mkString("\n")
            fromXml[T](xml)
        }
        catch {
            case ex:Exception => throw new InvalidFormatException("Couldn't parse file: "+file.getAbsolutePath+"\n"+ex.printStackTrace())
        }
    }

    def toFile(obj:Any,file:File) = {
        try {
            xstream.toXML(obj,new FileWriter(file))
        }
        catch {
            case ex:Exception => ex.printStackTrace()
        }
    }

}
