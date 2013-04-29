package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{SemanticRoleLabel, DepTag, Token, Sentence}
import de.tu.dresden.quasy.dep.{DependencyTree}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 10:54 AM
 */
object NounalRelationEnhancer extends TextEnhancer{

    private val templateTree = {
        val text = new AnnotatedText("  ")

        val nSubj = new Token(0,0,text,1)
        nSubj.depTag = new DepTag("nsubj",0)

        val prep = new Token(1,1,text,2)
        prep.depTag = new DepTag("prep",1)

        val pObj = new Token(2,2,text,3)
        pObj.depTag = new DepTag("pobj",2)

        val sentence = new Sentence(0,2,text)

        DependencyTree.fromSentence(sentence)
    }

    //private val template = new TreeTemplate(templateTree)

    def enhance(text: AnnotatedText) {
        text.getAnnotations[Sentence].foreach(sentence => {
            /*sentence.getDependencyTree.foreach(node => {
                template.matchTo(node) match {
                    case Some(filledTemplate) => {
                        val nsubj = filledTemplate.depTree.root
                        val prep = nsubj.children.find(_.nodeHead.depTag.tag.equals("prep")).get
                        try {
                        val pobj = prep.children.find(_.nodeHead.depTag.tag.equals("pobj")).get

                        pobj.nodeHead.srls ++= List(new SemanticRoleLabel(nsubj.nodeHead.position,"A1"))
                        }
                        catch {
                            case e => e.printStackTrace()
                        }
                    }
                    case None =>
                }
            }) */
        })
    }
}
