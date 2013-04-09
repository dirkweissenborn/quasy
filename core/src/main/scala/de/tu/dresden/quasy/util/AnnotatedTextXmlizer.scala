package de.tu.dresden.quasy.util

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import de.tu.dresden.quasy.model.{Span, Text}

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

        val clazz = classOf[Span]
        xstream.useAttributeFor(clazz,"start")
        xstream.useAttributeFor(clazz,"end")

        xstream.omitField(classOf[de.tu.dresden.quasy.model.annotation.Token],"bitmap_-0")
    }

    def toXml(text:Text) = xstream.toXML(text)

    def fromXml(xml:String) = xstream.fromXML(xml).asInstanceOf[Text]

}
