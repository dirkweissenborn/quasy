package de.tu.dresden.quasy.dependency

import scalax.collection.GraphEdge._
import scala.Some
import scalax.collection.GraphPredef.EdgeIn
import scalax.collection.edge.LDiEdge

/**
 * @author dirk
 * Date: 4/26/13
 * Time: 10:51 AM
 */
class DepEdge[N](nodes:Product, override val label:String)
    extends LDiEdge[N](nodes)
    with EdgeCopy[DepEdge]
    with EdgeIn[N,DepEdge]
{
    override type L1 = String

    override def copy[NN](newNodes: Product) =
        new DepEdge[NN](newNodes,label)

    override def equals(obj:Any) = obj match {
        case edge: DepTemplateEdge[N] =>
            edge.equals(this)
        case edge: DepEdge[N] =>
            super.equals(edge.asInstanceOf[LDiEdge[N]])
        case _ => false
    }
}

object DepEdge {
    def apply(from: DepNode, to: DepNode, label:String) =
        new DepEdge[DepNode](NodeProduct(from, to), label)

    def unapply[T](e: DepEdge[T]): Option[(T,T,String)] =
        if (e eq null) None else Some(e.from, e.to, e.label)

    final class DepEdgeAssoc[A <: DepNode](val e: DiEdge[A]) {
        @inline def ##(label: String) =
            new DepEdge[A](e.nodes, label)
    }
    implicit def edge2DepEdgeAssoc[A <: DepNode](e: DiEdge[A]) =
        new DepEdgeAssoc(e)

}


class DepTemplateEdge[N](nodes:Product, label:String) extends DepEdge[N](nodes, label) {
    private val labelRegex = label.r

    override def copy[NN](newNodes: Product) =
        new DepTemplateEdge[NN](newNodes,label)

    override def equals(obj:Any) = obj match {
        case edge: DepEdge[N] =>
            labelRegex.findFirstIn(edge.label).isDefined && this.from.equals(edge.from) && this.to.equals(edge.to)
        case _ => false
    }
}
object DepTemplateEdge {
    def apply(from: DepNode, to: DepNode, label:String) =
        new DepTemplateEdge[DepNode](NodeProduct(from, to), label)

    def unapply[T](e: DepTemplateEdge[T]): Option[(T,T,String)] =
        if (e eq null) None else Some(e.from, e.to, e.label)

    final class DepTemplateEdgeAssoc[A <: TemplateDepNode](val e: DiEdge[A]) {
        @inline def #?#(label: String) =
            new DepTemplateEdge[A](e.nodes, label)
    }
    implicit def edge2DepEdgeAssoc[A <: TemplateDepNode](e: DiEdge[A]) =
        new DepTemplateEdgeAssoc(e)

}

