package de.tu.dresden.quasy.dep

import de.tu.dresden.quasy.model.annotation.{ Sentence}
import com.thoughtworks.xstream.InitializationException
import scalax.collection.GraphTraversal.VisitorReturn
import scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._
import DepEdge._

/**
 * @author dirk
 * Date: 4/17/13
 * Time: 11:47 AM
 */
class DependencyTree(val graph:Graph[DependencyNode,DepEdge])  {
    if (graph.isCyclic)
        throw new InitializationException("Graph of dependency tree must not be cyclic")

    val root = graph.nodes.find(node => node.incoming.size == 0).get

    def getSubtree(node:DependencyNode): Graph[DependencyNode,DepEdge] = {
        var edges = List[DepEdge[DependencyNode]]()
        graph.get(node).traverse()(node => VisitorReturn.Continue, edge => edges ::= edge.toEdgeIn)
        Graph.from(List[DependencyNode](), edges)
    }

}

object DependencyTree {

    def apply(elems: GraphParamIn[DependencyNode,DepEdge]*)  =
        new DependencyTree(Graph.from(elems.filter(_.isNode).map(_.asInstanceOf[DependencyNode]),elems.filter(_.isEdge).map(_.asInstanceOf[DepEdge[DependencyNode]])))

    def fromSentence(sentence:Sentence) = {
        DependencyTree(sentence.getTokens.map[DepEdge[DependencyNode],Seq[DepEdge[DependencyNode]]]( t => {
            if (t.depTag.dependsOn > 0)
                new DependencyNode(sentence.getTokens.find(_.position.equals(t.depTag.dependsOn)).get) ~> new DependencyNode(t) ## (t.depTag.tag)
            else
                null
        }).filter(_ != null):_*)
    }

    def greedySubtreeSimilarity(currentRoot1:DependencyNode, currentRoot2:DependencyNode):Double = {
       0.0
    }

    def greedySimilarityDistance(tree1:DependencyTree, tree2:DependencyTree) = 1 - greedySimilarity(tree1,tree2)

    def greedySimilarity(tree1:DependencyTree, tree2:DependencyTree):Double = {
       0.0
    }

    private def calculateSimilarities(nodes1:List[DependencyNode], nodes2:List[DependencyNode]) = {
         0.0
    }

    private def calculateWeightingFactor(node1:DependencyNode,node2:DependencyNode) =
        1.0/math.pow(2,(node1.nodeHead.depDepth+node2.nodeHead.depDepth)/2.0)

    def checkEquality(tree1:DependencyTree, tree2:DependencyTree, equal: (DependencyNode,DependencyNode) => Boolean, toDepth:Int = Int.MaxValue):Boolean = {
        checkEquality(tree1.root,tree2.root,equal,toDepth)
    }

    def checkEquality(root1:DependencyNode, root2:DependencyNode, equal: (DependencyNode,DependencyNode) => Boolean, toDepth:Int):Boolean = {
        false
    }

}
