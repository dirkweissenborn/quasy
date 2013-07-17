package de.tu.dresden.quasy.webservices.bioasq.model

import java.net.URL


/**
 * @author dirk
 * Date: 4/10/13
 * Time: 11:00 AM
 */
case class BioASQServiceResult(
    var findings : Array[EntityFinding] = null,
    var keywords:String = "",
    var timeMS:Int = 0
)

protected case class EntityFinding (
    var concept: EntityConcept = null,
    var matchedLabel:String = "",

    var ranges : Array[EntitySpan] = null,
    var score:Double = 0.0
)

protected case class EntityConcept (
    var label:String = "",
    var termId:String = "",
    var uri:URL = null)

protected case class EntitySpan (
    var begin:Int = 0,
    var end:Int = 0
)