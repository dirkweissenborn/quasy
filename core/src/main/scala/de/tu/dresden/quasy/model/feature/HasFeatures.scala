package de.tu.dresden.quasy.model.feature

import collection.mutable.HashMap

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 11:47 AM
 */
//from DBpedia Spotlight
trait HasFeatures {

    val features = HashMap[String, Feature]()

    def feature(featureName: String): Option[Feature] = {
        features.get(featureName)
    }

    def featureValue[T](featureName: String): Option[T] = {
        features.get(featureName) match {
            case Some(f) => Option(f.value.asInstanceOf[T])
            case _ => None
        }
    }

    def setFeature(feature: Feature) {
        features.put(feature.featureName, feature)
    }

    /**
     * Adds a number of features to this instance. Mostly a convenience method for Java.
     * @param otherFeatures
     */
    def setFeatures(otherFeatures: HashMap[String,Feature]) {
        features ++= otherFeatures
    }

}