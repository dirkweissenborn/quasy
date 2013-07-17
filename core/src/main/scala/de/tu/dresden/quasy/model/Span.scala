package de.tu.dresden.quasy.model

import scala._
import com.thoughtworks.xstream.annotations.XStreamAsAttribute

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 11:39 AM
 */
/**
 * part between [start, end) of context
 * @param begin
 * @param end
 */
class Span(var begin:Int, var end:Int) {
    if (begin > end)
        throw new IllegalArgumentException("Begin must not be greater than end!")

    override def equals(obj:Any) = obj match {
        case that:Span => this.begin == that.begin && this.end == that.end
        case _ => false
    }

    override def toString = "["+begin + ","+end+")"

    override def hashCode = toString.hashCode()
}