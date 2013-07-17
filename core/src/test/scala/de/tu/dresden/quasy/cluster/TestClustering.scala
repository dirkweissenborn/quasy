package de.tu.dresden.quasy.cluster

import org.scalatest.FunSuite

/**
 * @author dirk
 * Date: 4/19/13
 * Time: 5:16 PM
 */
class TestClustering  extends FunSuite{

    /*@Test
    def testDEPParser {
        val parser = new DemoDEPParser("/home/dirk/workspace/bioasq/model/dictionary-1.3.1.zip",
            "/home/dirk/workspace/bioasq/model/medical-en-pos-1.1.0g.jar",
            "/home/dirk/workspace/bioasq/model/medical-en-dep-1.1.0b3.jar")

        val parse = parser.parse(question)

        parse.foreach( tree => System.out.println(tree.toStringDEP()+"\n"))
    } */

    test("Clustering should work") {
        class P(val x:Double,val y:Double) {
            def getRandom = new P(x+math.random/10-0.05,y+math.random/10-0.05)
        }

        def dist(p1:P,p2:P) = math.sqrt((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y))

        var c1 = new P(0.5, 0.35)

        var c2 = new P(0.7,0.4)

        val objects = (1 to 200).map( _ =>
            if(math.random > 0.5)
                c1.getRandom
            else
                c2.getRandom).toList
        val alg = new MedoidEM[P](dist)

        val clusters = alg.cluster(objects,2)

        clusters
    }


}
