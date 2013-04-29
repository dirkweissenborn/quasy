package de.tu.dresden.quasy.run

import org.scalatest.FunSuite
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Chunk, Sentence}
import de.tu.dresden.quasy.dep.{DependencyTree, CollapsedDependencyTree}
import java.util.Properties
import java.io.{File, FileInputStream}
import de.tu.dresden.quasy.io.AnnotatedTextSource

/**
 * @author dirk
 * Date: 4/17/13
 * Time: 3:09 PM
 */
class TestDependencyTree extends FunSuite {

    test("should run") {
        val text = new AnnotatedText("What is the role of thyroid hormones administration in the treatment of heart failure? " +
            "What is the relation of thyroid hormones levels in myocardial infarction? " +
            "How does aspirin work?")

        val props = new Properties()
        props.load(new FileInputStream(new File("conf/configuration.properties")))

        val corpus = new AnnotatedTextSource {
            val it = List(text).iterator
            def next() = it.next()

            def hasNext = it.hasNext

            def reset {}
        }

        RunFullPipeline.run(corpus,null,props)

        val depTrees = text.getAnnotations[Sentence].map(sentence => new CollapsedDependencyTree(sentence)).toArray

        assert(DependencyTree.greedySimilarity(depTrees(0),depTrees(1)) > DependencyTree.greedySimilarity(depTrees(0),depTrees(2)))
    }


}
