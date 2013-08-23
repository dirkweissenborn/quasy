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

    def cluster(objects:List[T], maxNrOfClusters:Int) : Map[Cluster[T],List[(T,Double)]]

    protected def getAssignmentsFromPosterior: Map[Cluster[T], List[(T,Double)]] = {
        val result = Map[Cluster[T], List[(T,Double)]]()
        posteriors.toList.map(obj => (obj._2.maxBy(_._2), obj._1)).
            groupBy(_._1._1).foreach(assignment => result += (assignment._1 -> assignment._2.map(a => (a._2,a._1._2))))
        result
    }

}
