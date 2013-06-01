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

