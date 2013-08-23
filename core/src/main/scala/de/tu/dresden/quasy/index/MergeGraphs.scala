package de.tu.dresden.quasy.index

import java.io.File
import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.TitanFactory
import com.tinkerpop.blueprints.{Edge, Direction, Vertex}
import scala.collection.JavaConversions._
import com.tinkerpop.blueprints.Query.Compare
import de.tu.dresden.quasy.model.annotation.UmlsConcept

/**
 * @author dirk
 *          Date: 8/23/13
 *          Time: 10:33 AM
 */
object MergeGraphs {

    def main(args:Array[String]) {
        val inDir = new File(args(0))
        val outDir = new File(args(1))

        val _override = if(args.size > 3)
            args(3) == "override"
        else
            true

        val newGraph = outDir.mkdirs() || _override || outDir.list().isEmpty

        if(_override){
            println("Overriding output directory!")
            def deleteDir(dir:File) {
                dir.listFiles().foreach(f => {
                    if (f.isDirectory) {
                        deleteDir(f)
                        f.delete()
                    }
                    else
                        f.delete()
                })
            }
            deleteDir(outDir)
        }

        val titanConf = new BaseConfiguration()
        titanConf.setProperty("storage.directory",new File(outDir,"standard").getAbsolutePath)
        titanConf.setProperty("storage.index.search.backend","lucene")
        titanConf.setProperty("storage.index.search.directory",new File(outDir,"searchindex").getAbsolutePath)

        val bigGraph = TitanFactory.open(titanConf)
        if(newGraph) {
            bigGraph.makeType().name("cui").dataType(classOf[String]).indexed(classOf[Vertex]).unique(Direction.BOTH).makePropertyKey()
            bigGraph.makeType().name("semtypes").dataType(classOf[String]).indexed("search",classOf[Vertex]).unique(Direction.OUT).makePropertyKey()
            bigGraph.makeType().name("uttIds").dataType(classOf[String]).indexed("search",classOf[Edge]).unique(Direction.OUT).makePropertyKey()
            bigGraph.makeType().name("count").dataType(classOf[java.lang.Integer]).indexed(classOf[Edge]).unique(Direction.OUT).makePropertyKey()
            bigGraph.commit()
        }

        inDir.listFiles().foreach(indexDir => {
            val smallConf = new BaseConfiguration()
            smallConf.setProperty("storage.directory",new File(indexDir,"standard").getAbsolutePath)
            smallConf.setProperty("storage.index.search.backend","lucene")
            smallConf.setProperty("storage.index.search.directory",new File(indexDir,"searchindex").getAbsolutePath)

            val smallGraph = TitanFactory.open(smallConf)

            smallGraph.query().vertices().iterator().foreach(fromSmall => {
                val fromCui = fromSmall.getProperty[String]("cui")
                val it1 = bigGraph.query.has("cui", Compare.EQUAL, fromCui).limit(1).vertices()
                val from =
                    if (!it1.isEmpty)
                        it1.head
                    else {
                        val f = bigGraph.addVertex(null)
                        f.setProperty("cui", fromCui)
                        val fromSemTypes = fromSmall.getProperty[String]("semtypes")
                        f.setProperty("semtypes", fromSemTypes)
                        f
                    }

                fromSmall.getEdges(Direction.OUT).iterator().foreach(e => {
                    val toSmall = e.getVertex(Direction.IN)
                    val toCui = toSmall.getProperty[String]("cui")

                    val newIds = e.getProperty[String]("uttIds")
                    val count = e.getProperty[java.lang.Integer]("count")
                    val rel = e.getLabel
                    val it2 = bigGraph.query.has("cui", Compare.EQUAL, toCui).limit(1).vertices()

                    val to = if (!it2.isEmpty)
                        it2.head
                    else {
                        val t = bigGraph.addVertex(null)
                        t.setProperty("cui", toCui)
                        val toSemTypes = toSmall.getProperty[String]("semtypes")
                        t.setProperty("semtypes", toSemTypes)
                        t
                    }

                    from.getEdges(Direction.OUT, rel).iterator().find(_.getVertex(Direction.IN) == to) match {
                        case Some(edge) => {
                            if(edge.getLabel == "hmod -> contain:VB -> pobj")
                                edge
                            val ids = edge.getProperty[String]("uttIds")
                            edge.setProperty("count", edge.getProperty[java.lang.Integer]("count") + count)
                            edge.setProperty("uttIds", ids + "," + newIds)
                        }
                        case None => {
                            val edge = bigGraph.addEdge(null, from, to, rel)
                            edge.setProperty("uttIds", newIds)
                            edge.setProperty("count", count)
                        }
                    }
                })
            })
            smallGraph.shutdown()
            bigGraph.commit()
        })
        bigGraph.shutdown()
    }

}
