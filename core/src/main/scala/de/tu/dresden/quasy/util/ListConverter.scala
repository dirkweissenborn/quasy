package de.tu.dresden.quasy.util

import com.thoughtworks.xstream.converters._
import com.thoughtworks.xstream.converters.collections._
import com.thoughtworks.xstream.mapper._
import com.thoughtworks.xstream.io._

class ListConverter( _mapper : Mapper )  extends AbstractCollectionConverter(_mapper) {

    def canConvert( clazz: Class[_]) = {
        clazz == classOf[::[_]]
    }

    def marshal( value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val list = value.asInstanceOf[List[_]]
        for ( item <- list ) {
            writeItem(item, context, writer)
        }
    }

    def unmarshal( reader: HierarchicalStreamReader, context: UnmarshallingContext ) = {
        var list : List[_] = Nil
        while (reader.hasMoreChildren()) {
            reader.moveDown()
            val item = readItem(reader, context, list)
            list = list ::: List(item) // be sure to build the list in the same order
            reader.moveUp()
        }
        list
    }
}