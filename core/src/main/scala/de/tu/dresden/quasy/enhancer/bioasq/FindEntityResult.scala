package de.tu.dresden.quasy.enhancer.bioasq

import java.net.URL


/**
 * @author dirk
 * Date: 4/10/13
 * Time: 11:00 AM
 */
protected[bioasq] class FindEntityResult {
    var findings : Array[EntityFinding] = null
    var keywords:String = ""
    var timeMS = 0
}

protected[bioasq] class EntityFinding {
    var concept: EntityConcept = null
    var matchedLabel = ""

    var ranges : Array[EntitySpan] = null
    var score:Double = 0.0
}

protected[bioasq] class EntityConcept {
    var label = ""
    var termId = ""
    var uri:URL = null
}

protected[bioasq] class EntitySpan {
    var begin = 0
    var end = 0
}