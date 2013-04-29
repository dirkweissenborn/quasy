package de.tu.dresden.quasy.cluster


/**
 * @author dirk
 * Date: 4/19/13
 * Time: 12:11 PM
 */
class Cluster[T](val id:String, var center:T, var variance:Double = 1.0) {

    def getGaussianProbability(obj:T, distanceMeasure:(T,T) => Double):Double =
        getGaussianProbability(distanceMeasure(center,obj))

    def getGaussianProbability(distance: Double) =
        1.0/math.sqrt(2*math.Pi*variance)*math.exp(-distance*distance/(2*variance))

}
