package de.tu.dresden.quasy.webservices.bioasq.model

import java.net.URL

/**
 * @author dirk
 * Date: 4/10/13
 * Time: 3:58 PM
 */
case class LinkedLifeTriples(var query:String, var timeMS:Int,var entities:Array[Entity])

case class Entity(var entity:URL,var score:Double,var relations:Array[Relation])

case class Relation(var labels:Array[String],var pred:URL, var obj:String =null, var subj:String = null)
