package de.tu.dresden.quasy.dep

import scalax.collection.GraphEdge._
import scala.Some
import scalax.collection.GraphPredef.EdgeIn

/**
 * @author dirk
 * Date: 4/26/13
 * Time: 10:51 AM
 */
class DepEdge[N](nodes:Product, override val label:String)
    extends DiEdge[N](nodes)
    with EdgeCopy[DepEdge]
    with EdgeIn[N,DepEdge]
{

    override def copy[NN](newNodes: Product) =
        new DepEdge[NN](newNodes,label)

    override def equals(obj:Any) = obj match {
        case Some(edge: DepTemplateEdge[N]) => edge.equals(this)
        case Some(edge: DepEdge[N]) =>  super.equals(edge.asInstanceOf[DiEdge[N]]) && edge.label.equals(label)
        case _ => false
    }
}

object DepEdge {
    def apply(from: DependencyNode, to: DependencyNode, label:String) =
        new DepEdge[DependencyNode](NodeProduct(from, to), label)

    def unapply[T](e: DepEdge[T]): Option[(T,T,String)] =
        if (e eq null) None else Some(e.from, e.to, e.label)

    final class DepEdgeAssoc[A <: DependencyNode](val e: DiEdge[A]) {
        @inline def ##(label: String) =
            new DepEdge[A](e.nodes, label)
    }
    implicit def edge2DepEdgeAssoc[A <: DependencyNode](e: DiEdge[A]) =
        new DepEdgeAssoc(e)
}


class DepTemplateEdge[N](nodes:Product, label:String) extends DepEdge[N](nodes, label) {
    private val labelRegex = label.r

    override def copy[NN](newNodes: Product) =
        new DepTemplateEdge[NN](newNodes,label)

    override def equals(obj:Any) = obj match {
        case Some(edge: DepEdge[N]) =>  super.equals(edge.asInstanceOf[DiEdge[N]]) && labelRegex.findFirstIn(edge.label).isDefined
        case _ => false
    }
}
object DepTemplateEdge {
    def apply(from: DependencyNode, to: DependencyNode, label:String) =
        new DepTemplateEdge[DependencyNode](NodeProduct(from, to), label)

    def unapply[T](e: DepTemplateEdge[T]): Option[(T,T,String)] =
        if (e eq null) None else Some(e.from, e.to, e.label)

    final class DepTemplateEdgeAssoc[A <: TemplateDependencyNode](val e: DiEdge[A]) {
        @inline def #?#(label: String) =
            new DepTemplateEdge[A](e.nodes, label)
    }
    implicit def edge2DepEdgeAssoc[A <: TemplateDependencyNode](e: DiEdge[A]) =
        new DepTemplateEdgeAssoc(e)

}

