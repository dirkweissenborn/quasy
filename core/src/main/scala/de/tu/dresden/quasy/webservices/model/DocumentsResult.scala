package de.tu.dresden.quasy.webservices.model

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 2:55 PM
 */
class DocumentsResult(var documents:Array[Document])

class Document(var pmid:String, var documentAbstract:String, var title:String, var sections:Array[String])
