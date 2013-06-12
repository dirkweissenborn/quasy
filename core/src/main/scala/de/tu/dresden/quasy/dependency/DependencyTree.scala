package de.tu.dresden.quasy.dependency

import de.tu.dresden.quasy.model.annotation.{Token, Sentence}
import com.thoughtworks.xstream.InitializationException
import scalax.collection.GraphTraversal.VisitorReturn
import scalax.collection.Graph
import scalax.collection.GraphPredef._
import DepEdge._
import scalax.collection.edge.LDiEdge
import collection.mutable.ListBuffer

/**
 * @author dirk
 * Date: 4/17/13
 * Time: 11:47 AM
 */
class DependencyTree(val graph:Graph[DepNode,DepEdge])  {
    if (graph.isCyclic)
        throw new InitializationException("Graph of dependency tree must not be cyclic")

    var root = graph.nodes.filter((node:graph.NodeT ) => node.incoming.size == 0).last
    var firstNode:DepNode = graph.nodes.find( (node:graph.NodeT) => node.value.asInstanceOf[DepNode].nodeHead.position.equals(1)).get.value.asInstanceOf[DepNode]

    def getSubtree(node:DepNode): Graph[DepNode,DepEdge] = {
        var edges = List[DepEdge[DepNode]]()
        graph.get(node).traverse()(_ => VisitorReturn.Continue, edge => edges ::= edge.toEdgeIn)
        Graph.from(List[DepNode](node), edges)
    }

    def getSubtree(node:graph.NodeT): Graph[DepNode,DepEdge] = {
        var edges = List[DepEdge[DepNode]]()
        node.traverse()(_ => VisitorReturn.Continue, edge => edges ::= edge.toEdgeIn)
        Graph.from(List[DepNode](node), edges)
    }

    def getSubtree(token:Token): Graph[DepNode,DepEdge] = {
       getSubtree(getDepNode(token).get)
    }

    def getDepNode(token:Token) = {
        graph.nodes.find(_.value.asInstanceOf[DepNode].nodeHead.equals(token))
    }

    def matchTreeToTemplate(template: Graph[(String,Int),LDiEdge]) = {
        var results = List(ListBuffer[DepEdge[graph.NodeT]]())

        def matchSubTree(it:Iterator[template.EdgeT], availableEdges:Set[graph.EdgeT]) {
            val templEdge = it.next()
            val edgeRegex = templEdge.edge.label.toString
            val toRegex = templEdge.to.value.asInstanceOf[(String,Int)]._1
            availableEdges.filter(avEdge => avEdge.edge.label.matches(edgeRegex) &&
              avEdge.edge.to.value.asInstanceOf[DepNode].getLabel.matches(toRegex)).foreach(matchEdge => {
                results.head.prepend(matchEdge.edge)
                if (results.head.size.equals(template.edges.size))
                    results ::= results.head.clone()

                if (templEdge.to.outgoing.size>0)
                    matchSubTree(templEdge.to.outgoing.iterator,matchEdge.to.outgoing.toSet)
                if(it.hasNext)
                    matchSubTree(it, availableEdges - matchEdge)

                results.head -= results.head.head
              })
        }

        val roots = template.nodes.filter(_.incoming.size == 0)

        roots.foreach(root => {
            graph.nodes.filter(_.value.asInstanceOf[DepNode].getLabel.matches(root.value.asInstanceOf[(String,Int)]._1)).foreach(rootMatch => {
                matchSubTree(root.outgoing.iterator,rootMatch.outgoing.toSet)
            })
        })

        results.drop(1)
    }

    def prettyPrint =
        graph.nodes.toList.sortBy(_.value.asInstanceOf[DepNode].nodeHead.position)
        .map(node => {
            val depNode= node.value.asInstanceOf[DepNode]
            "\t"*2*depNode.nodeHead.depDepth+depNode.nodeHead.depTag.dependsOn+":"+depNode.nodeHead.depTag.tag+":"+depNode.getLabel+":"+depNode.nodeHead.position
        }).mkString("\n")

    def find(condition:DepNode => Boolean) = graph.nodes.find(node => condition(node.value.asInstanceOf[DepNode])) match {
        case Some(node) => Some(node.value.asInstanceOf[DepNode])
        case None => None
    }
}

object DependencyTree {

    def apply(elems: GraphParamIn[DepNode,DepEdge]*)  =
        new DependencyTree(Graph.from(elems.filter(_.isNode).map(_.asInstanceOf[DepNode]),elems.filter(_.isEdge).map(_.asInstanceOf[DepEdge[DepNode]])))

    def fromSentence(sentence:Sentence) = {
        val edges: Seq[DepEdge[DepNode]] = sentence.getTokens.map[DepEdge[DepNode], Seq[DepEdge[DepNode]]](t => {
            if (t.depTag.dependsOn > 0)
                new DepNode(sentence.getTokens.find(_.position.equals(t.depTag.dependsOn)).get) ~> new DepNode(t) ## (t.depTag.tag)
            else
                null
        })
        val nodes =  sentence.getTokens.map[DepNode, Seq[DepNode]](t => {
             new DepNode(t)
        })

        new DependencyTree(Graph.from(nodes, edges.filterNot(_ == null)))
    }

}
