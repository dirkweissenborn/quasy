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
                    writer.startNode(m.erasure.getSimpleName)
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
        while(reader.hasMoreChildren) {
            reader.moveDown()
            val className = reader.getAttribute("class")
            val clazz = Class.forName(className)
            val m = Manifest.classType(clazz)

            var list = List[Annotation]()
            while(reader.hasMoreChildren) {
                reader.moveDown()
                val current = context.convertAnother(context.currentObject(),clazz)
                list ++= List(current.asInstanceOf[Annotation])
                reader.moveUp()
            }
            map += (m -> list)

            reader.moveUp()
        }

        map
    }
}
