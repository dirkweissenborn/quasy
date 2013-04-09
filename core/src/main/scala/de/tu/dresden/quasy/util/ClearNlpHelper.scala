package de.tu.dresden.quasy.util

import com.googlecode.clearnlp.pos.POSNode
import de.tu.dresden.quasy.model.feature.Feature
import de.tu.dresden.quasy.model.annotation.Token

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 2:46 PM
 */
object ClearNlpHelper {

    def toPOSNodes(tokens:List[Token]):Array[POSNode] = {
        tokens.map(token => {
            new POSNode(token.coveredText, token.posTag, token.lemma)
        }).toArray
    }

}
