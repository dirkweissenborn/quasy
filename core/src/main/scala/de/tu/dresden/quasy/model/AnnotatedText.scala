/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tu.dresden.quasy.model

import annotation.Token
import org.apache.commons.logging.LogFactory
import com.thoughtworks.xstream.annotations.{XStreamOmitField}
import de.tu.dresden.quasy.util.{Xmlizer}
import de.tu.dresden.quasy.model.db.AnnotationCache


/**
 * Simple wrapper for strings
 * @param text
 */
class AnnotatedText(val id:String, var text : String) extends Annotatable {

    def this(text:String) = this("NO_ID",text)

    @XStreamOmitField
    val LOG = LogFactory.getLog(getClass)

    // do some clean up on the text
    text = AnnotatedText.cleanText(text)

    AnnotationCache.addToCache(this)

    override def equals(that : Any) = {
        that match {
            case t: AnnotatedText => this.text.equals(t.text)
            case _ => false
        }
    }

    override def hashCode() : Int = {
      (if (text != null) text.hashCode else 0)
    }
    
    override def toString = "AnnotatedText["+text+"]"

    var enhancedBy = Set[String]()

    //TODO make this better; this is a real HACK
    override def clone = {
        Xmlizer.fromXml[AnnotatedText](Xmlizer.toXml(this))
    }
}

object AnnotatedText {
    def main(args:Array[String]) {
        val t = new AnnotatedText("a b")
        new Token(0,1,t,1)
        new Token(2,3,t,2)
        new Token(0,1,t,1)
        val tokens = t.getAnnotations[Token]
        assert(tokens.size == 2)
    }

    def cleanText(text:String) = text.replaceAll("’", "'").replaceAll("–","-").replaceAll("([^\\s])(:)([^\\s])","$1 $2 $3").replaceAll(" ( )+"," ")
}
