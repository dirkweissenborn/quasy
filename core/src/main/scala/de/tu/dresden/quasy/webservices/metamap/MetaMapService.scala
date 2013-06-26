package de.tu.dresden.quasy.webservices.metamap

import gov.nih.nlm.nls.metamap._
import scala.collection.JavaConversions._
/**
 * @author dirk
 *          Date: 6/17/13
 *          Time: 3:52 PM
 */
object MetaMapService {
    /** MetaMap api instance */
    private[metamap] var api: MetaMapApi = new MetaMapApiImpl
    api.setHost(MetaMapApi.DEFAULT_SERVER_HOST)
    api.setPort(MetaMapApi.DEFAULT_SERVER_PORT)
    api.setOptions("-Al")

    /**
     * Process terms using MetaMap API and display result to standard output.
     *
     * @param text input terms
     */
    def process(text: String) : List[Result] = {
        api.processCitationsFromString(text).toList
    }
}
