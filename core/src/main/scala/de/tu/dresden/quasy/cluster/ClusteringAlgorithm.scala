package de.tu.dresden.quasy.cluster

import collection.mutable.{Map, Set}

/**
 * @author dirk
 * Date: 4/23/13
 * Time: 10:13 AM
 */
trait ClusteringAlgorithm[T] {

    var distances = Map[Set[T],Double]()

    var clusters = List[Cluster[T]]()

    var posteriors = Map[T,Map[Cluster[T],Double]]()

    def reset {
        distances = Map[Set[T],Double]()

        clusters = List[Cluster[T]]()

        posteriors = Map[T,Map[Cluster[T],Double]]()
    }

    def cluster(objects:List[T], maxNrOfClusters:Int) : List[Cluster[T]]

}
