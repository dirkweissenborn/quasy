package de.tu.dresden.quasy.model.annotation

import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.dependency.{DepNode, DependencyTree}
import com.thoughtworks.xstream.annotations.XStreamOmitField
import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 *          Date: 4/15/13
 *          Time: 1:32 PM
 */
class Sentence(spans:Array[Span], context:AnnotatedText) extends Annotation(spans,context) {
    if (spans.size > 1) {
        throw new IllegalArgumentException("Section can just span over one span range!")
    }

    def this(begin:Int,end:Int,context:AnnotatedText) = this(Array(new Span(begin,end)),context)

    def dependencyTree = {
        if (_dependencyTree==null)
            _dependencyTree = DependencyTree.fromSentence(this)
        _dependencyTree
    }

    private var _dependencyTree: DependencyTree = null

    def printRoleLabels = {
        this.getTokens.flatMap(token => {
            token.srls.map(srl => {
                var res = ""
                try {
                    val head: DepNode = dependencyTree.find((node: DepNode) => node.nodeHead.position.equals(srl.head)).get
                    val depNode: DepNode = dependencyTree.find((node: DepNode) => node.nodeHead.equals(token)).get

                    res = srl.label + "("
                    res += head.tokens.map(_.lemma).mkString(" ") + ","
                    if (depNode.nodeHead.depDepth > head.nodeHead.depDepth) {
                        val subtree = dependencyTree.getSubtree(depNode)
                        res += subtree.nodes.toList.flatMap((node: subtree.NodeT) => node.value.asInstanceOf[DepNode].tokens).sortBy(_.position).map(_.coveredText).mkString(" ")
                    }
                    else {
                        res += depNode.tokens.map(_.lemma).mkString(" ")
                    }
                    res += ")"
                }
                catch {
                    case e => e.printStackTrace()
                }
                res
            })
        }).mkString("\t")
    }
}

class Question(spans:Array[Span], context:AnnotatedText) extends Sentence(spans,context) {
    def this(begin:Int,end:Int,context:AnnotatedText) = this(Array(new Span(begin,end)),context)

    var answerType:AnswerType = null
    var questionType:QuestionType = null
}

abstract class AnswerType

trait SimpleAnswerTypeLike {
    val coveredTokens:List[Token]
}

case class SimpleAnswerType(coveredTokens:List[Token]) extends AnswerType with SimpleAnswerTypeLike

case class SemanticAnswerType(semanticTypes:Set[String]) extends AnswerType

case class ConceptualAnswerType(coveredTokens:List[Token],concepts:List[OntologyConcept])
    extends SemanticAnswerType(concepts.filter(_.isInstanceOf[UmlsConcept]).map(_.asInstanceOf[UmlsConcept]).flatMap(_.semanticTypes).toSet)
    with SimpleAnswerTypeLike

case class DecisionAnswerType(answerOptions: Array[FactoidAnswer], criterion:String) extends AnswerType

trait QuestionType
object QuestionType {
    def fromString(strType:String) = strType match {
        case "factoid" => FactoidQ
        case "list" => ListQ
        case "yesno" => YesNoQ
        case "summary" => SummaryQ
    }
}

object FactoidQ extends QuestionType
object ListQ extends QuestionType
object YesNoQ extends QuestionType
object SummaryQ extends QuestionType


