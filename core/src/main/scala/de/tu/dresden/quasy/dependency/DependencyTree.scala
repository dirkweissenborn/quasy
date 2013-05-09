package de.tu.dresden.quasy.dependency

import de.tu.dresden.quasy.model.annotation.{Sentence}
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
        graph.get(node).traverse()(node => VisitorReturn.Continue, edge => edges ::= edge.toEdgeIn)
        Graph.from(List[DepNode](node), edges)
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

    /*/TODO a little bit HACKY
    def buildTemplate(edges:LDiEdge[(String,Int)]{type L1 = String}*) = {
        val context = new AnnotatedText("")
        val nodes = Map(edges.flatMap(edge => Set(edge.from,edge.to)).map{
            case (lemmaRegex,position) => {
                context.text += " "+lemmaRegex
                ((lemmaRegex,position) -> new TemplateDepNode(new Token(context.text.length-lemmaRegex.length,context.text.length,context,position)))
            }}:_*)

        DependencyTree(edges.map(edge => {
            val from = nodes(edge.from)
            val to = nodes(edge.to)
            to.nodeHead.depTag = new DepTag(edge.label.toString, from.nodeHead.position)
            DepTemplateEdge(from,to,edge.label.toString)
        }):_*)
    }*/

    def greedySubtreeSimilarity(currentRoot1:DepNode, currentRoot2:DepNode):Double = {
       0.0
    }

    def greedySimilarityDistance(tree1:DependencyTree, tree2:DependencyTree) = 1 - greedySimilarity(tree1,tree2)

    def greedySimilarity(tree1:DependencyTree, tree2:DependencyTree):Double = {
       0.0
    }

    private def calculateSimilarities(nodes1:List[DepNode], nodes2:List[DepNode]) = {
         0.0
    }

    private def calculateWeightingFactor(node1:DepNode,node2:DepNode) =
        1.0/math.pow(2,(node1.nodeHead.depDepth+node2.nodeHead.depDepth)/2.0)

    def checkEquality(tree1:DependencyTree, tree2:DependencyTree, equal: (DepNode,DepNode) => Boolean, toDepth:Int = Int.MaxValue):Boolean = {
        checkEquality(tree1.root,tree2.root,equal,toDepth)
    }

    def checkEquality(root1:DepNode, root2:DepNode, equal: (DepNode,DepNode) => Boolean, toDepth:Int):Boolean = {
        false
    }

}
