package de.tu.dresden.quasy.model.feature

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 11:46 AM
 *
 * from DBpedia Spotlight
 */
/**
 * Generic class for specifying features of occurrences.
 * Getting values can use pattern matching.
 *
 */
class Feature(val featureName: String, val value: Any) {
    override def toString : String = value.toString

    def prettyPrint = value.toString
}

class Score(name: String, value: Double) extends Feature(name,value)
class Number(name:String, value:Int) extends Feature(name,value)
class Nominal(name: String, value: String) extends Feature(name,value)




