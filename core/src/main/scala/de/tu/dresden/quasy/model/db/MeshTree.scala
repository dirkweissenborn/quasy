package de.tu.dresden.quasy.model.db

import io.Source

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 10:23 AM
 */
object MeshTree {

    private var headingToTreeNrs = Map[String,Set[String]]()
    private var treeNrToHeading = Map[String,String]()

    Source.fromFile(getClass.getClassLoader.getResource("mesh/mtrees2013.bin").getPath).getLines().foreach(line => {
        if (!line.isEmpty) {
            val Array(heading,treeNr) = line.split(";",2)
            headingToTreeNrs += (heading -> (headingToTreeNrs.getOrElse(heading,Set[String]()) + treeNr))
            treeNrToHeading += (treeNr -> heading)
        }
    })

    def getTreeNrs(heading:String) = headingToTreeNrs.getOrElse(heading,Set[String]())

    def getHeading(treeNr:String) = treeNrToHeading.getOrElse(treeNr,"")
}
