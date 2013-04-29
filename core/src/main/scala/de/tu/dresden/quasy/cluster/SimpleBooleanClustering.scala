package de.tu.dresden.quasy.cluster

import collection.mutable._

/**
 * @author dirk
 * Date: 4/23/13
 * Time: 10:12 AM
 */
class SimpleBooleanClustering[T](val equals: (T,T) => Boolean) extends ClusteringAlgorithm[T] {
    def cluster(objects: List[T], maxNrOfClusters: Int) = {
        reset
        var id = 0
        objects.foreach(obj => {
            clusters.find(cluster => equals(cluster.center,obj)) match {
                case Some(cluster) => posteriors += (obj -> Map(cluster -> 1.0))
                case None => {
                    val cluster = new Cluster[T](id.toString,obj)
                    clusters ::= cluster
                    posteriors += (obj -> Map(cluster -> 1.0))
                    id += 1
                }
            }
        })

        clusters
    }
}
