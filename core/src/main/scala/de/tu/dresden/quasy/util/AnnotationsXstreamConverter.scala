package de.tu.dresden.quasy.util

import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext, Converter}
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import de.tu.dresden.quasy.model.annotation.Annotation
import collection.mutable._

/**
 * @author dirk
 * Date: 4/8/13
 * Time: 5:03 PM
 */
class AnnotationsXstreamConverter extends Converter {
    def marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val map = source.asInstanceOf[Map[Manifest[_],List[Annotation]]]

        map.foreach {
            case(m, annotations) => {
                writer.startNode(m.erasure.getSimpleName+"s")
                writer.addAttribute("class",m.erasure.getName)
                annotations.foreach(annotation => {
                    writer.startNode(annotation.getClass.getSimpleName)
                    writer.addAttribute("class",annotation.getClass.getName)
                    context.convertAnother(annotation)
                    writer.endNode()
                })
                writer.endNode()
            }
        }
        writer.flush()
    }

    def canConvert(p1: Class[_]) =
        true

    def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = {
        val map = Map[Manifest[_],List[Annotation]]()
        val annotationM = Manifest.classType(classOf[Annotation])

        while(reader.hasMoreChildren) {
            reader.moveDown()
            var className = reader.getAttribute("class")
            var m = Manifest.classType(Class.forName(className))

            var list = List[Annotation]()
            while(reader.hasMoreChildren) {
                reader.moveDown()
                className = reader.getAttribute("class")
                val clazz = Class.forName(className)
                val current = context.convertAnother(context.currentObject(),clazz)
                list ++= List(current.asInstanceOf[Annotation])
                reader.moveUp()
            }
            while(m.<:<(annotationM) && !m.equals(annotationM)) {
                map += (m -> list)
                m = Manifest.classType(m.erasure.getSuperclass)
            }

            reader.moveUp()
        }

        map
    }
}
