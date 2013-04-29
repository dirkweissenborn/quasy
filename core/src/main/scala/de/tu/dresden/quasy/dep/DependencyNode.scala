package de.tu.dresden.quasy.dep

import de.tu.dresden.quasy.model.annotation.Token

/**
 * @author dirk
 * Date: 4/17/13
 * Time: 12:13 PM
 */
class DependencyNode(var tokens:List[Token], var nodeHead:Token, var optional:Boolean = false) {
    tokens = tokens.sortBy(_.position)

    def this(token:Token) = this(List(token),token)

    override def hashCode() = tokens.hashCode() + nodeHead.hashCode()

    override def toString = "DependencyNode["+tokens.map(_.coveredText).mkString(" ")+"]"

    def getLabel = tokens.map(_.lemma).mkString(" ")

    override def equals(obj:Any) = obj match {
        case templateNode:TemplateDependencyNode => {
            templateNode.equals(this)
        }
        case depNode:DependencyNode => {
            this.tokens.equals(depNode.tokens)  && depNode.nodeHead.equals(this.nodeHead)
        }
        case _ => false
    }
}

class TemplateDependencyNode(templateToken:Token, optional:Boolean = false) extends DependencyNode(List(templateToken),templateToken,optional) {
    private val tagRegex = nodeHead.depTag.tag.r
    private val labelRegex = getLabel.r

    override def equals(obj:Any)  = obj match {
        case depNode:DependencyNode => {
            var matching = tagRegex.findFirstIn(depNode.nodeHead.depTag.tag).isDefined
            matching = labelRegex.findFirstIn(depNode.getLabel).isDefined

            if (matching) {
                tokens = depNode.tokens
                nodeHead = depNode.nodeHead
            }
            matching
        }
        case _ => false
    }
}

object DependencyNode {

    def similarity(node1:DependencyNode, node2:DependencyNode) = {
        var sim = Token.similarity(node1.nodeHead,node2.nodeHead)

       /* var ts1 = node1.tokens

        node1.tokens.foreach( _ => {
            sim = math.max(sim, ts1.zip(node2.tokens).map {
                                    case (t1,t2) => Token.similarity(t1,t2)
                                }.sum )
            ts1 = ts1.tail
        })                */

        /*if (node1.chunkType.equals(node2.chunkType))
            if (node1.chunkType == "NA")
                sim += .5
            else
                sim += 1*/

        //normalize similarity so that it is at most one
        sim
    }

    def equalDependencyTag(node1:DependencyNode, node2:DependencyNode):Boolean = node1.nodeHead.depTag.tag.equals(node2.nodeHead.depTag.tag)

}
