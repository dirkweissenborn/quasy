package de.tu.dresden.quasy.cluster

import collection.mutable._
import scala.math
import scala.Some

/**
 * @author dirk
 * Date: 4/19/13
 * Time: 12:03 PM
 */
class MedoidEM[T](val distanceMeasure: (T,T) => Double) extends ClusteringAlgorithm[T]{

    def cluster(objects:List[T], maxNrOfClusters:Int) = {
        def getDistance(obj1:T,obj2:T) = {
            val set = Set(obj1,obj2)
            distances.get(set) match {
                case Some(dist) => dist
                case None => {
                    val dist = distanceMeasure(obj1,obj2)
                    distances += (set -> dist)
                    dist
                }
            }
        }

        reset

        val size = objects.size

        objects.foreach(obj => distances += (Set(obj) -> 0.0))

        initClusters(objects.take(math.max(size/10,math.min(maxNrOfClusters,size))), distanceMeasure, math.min(maxNrOfClusters,size))

        objects.foreach(obj => {
            clusters.foreach(cluster => {
                posteriors += (obj -> (posteriors.getOrElse(obj,Map[Cluster[T],Double]()) + (cluster -> 0.0)))
            })
        })

        var done = false

        var oldCenters = Map[Cluster[T],T]()
        clusters.foreach(cluster => oldCenters += (cluster -> cluster.center))

        while(!done) {
            done = true
            //E-step
            //p(C|Obj) = p(Obj|C) / Sum_C'( p(Obj|C') )
            objects.foreach(obj => {
                clusters.foreach(cluster => {
                    posteriors(obj)(cluster) =
                        cluster.getGaussianProbability(getDistance(obj,cluster.center)) /
                            clusters.foldLeft(0.0)( (acc, clusterP) => acc + clusterP.getGaussianProbability(getDistance(obj,clusterP.center)) )
                })
            })

            //M-step
            /*
            medoid(C) = argmin_obj ( Sum_obj'( dist(obj,obj') * p(C|obj') ) )
            variance(C) = Sum_obj( dist( obj, medoid(C) )^2 *p(C|obj) ) / (Sum_obj( p(C|obj) ) * (n-1)/n )
             */
            var toRemove = List[Cluster[T]]()
            clusters.foreach(cluster => {
                val candidates = posteriors.filter(_._2.maxBy(_._2)._1.equals(cluster)).map(_._1)
                if (candidates.isEmpty || candidates.size.equals(1))
                    toRemove ::= cluster
                else {
                    val (newCenter,(_,sumD2P,sumP)) = candidates.map(candidateCenter  =>
                        //sum distance * posterior, sum dist^2 * posterior, sum posterior
                        (candidateCenter, candidates.foldLeft((0.0, 0.0, 0.0)) {
                            case ((sumDistPost,sumDist2Post,sumPost), obj) => {
                                val dist = getDistance(candidateCenter,obj)
                                val posterior = posteriors(obj)(cluster)

                                (sumDistPost + dist * posterior, sumDist2Post + dist*dist*posterior, sumPost + posterior)
                            }
                        })
                    ).minBy(_._2._1)

                    done &&= (cluster.center.equals(newCenter) || oldCenters(cluster).equals(newCenter) )
                    oldCenters += (cluster -> cluster.center)

                    cluster.center = newCenter
                    cluster.variance = sumD2P / (sumP * (size-1)/size)
                }
            } )

            toRemove.foreach(cluster => clusters -= cluster)
        }

        clusters
    }

    private def initClusters(objects:List[T], distanceMeasure: (T,T) => Double, nrOfClusters:Int) {
        val head = objects.head
        clusters ::= new Cluster[T]("0",head,0.0)

        var leftObjects = objects.tail.toIterable

        var candidates = Map[T,Double]()

        (1 until nrOfClusters).foreach(id => {
            candidates = leftObjects.foldLeft(candidates)( (map,obj) => {
                val center = clusters.last.center
                val dis = distanceMeasure(center,obj)
                distances += (Set(center,obj) -> dis)

                map + (obj -> (map.getOrElse(obj,0.0) + dis))
            })

            val candidate = candidates.maxBy(_._2)._1
            clusters ++= List(new Cluster[T](id.toString,candidate,0.0))
            candidates -= candidate
            leftObjects = candidates.keys
        })

        val initVariance = distances.values.map(d => d*d).sum / (distances.size)
        clusters.foreach(cluster => cluster.variance = initVariance)

    }

}
