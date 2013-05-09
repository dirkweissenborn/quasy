import edu.smu.tspell.wordnet.{SynsetType, WordNetDatabase, NounSynset}

/**
 * @author dirk
 *          Date: 5/7/13
 *          Time: 2:26 PM
 */
var database: WordNetDatabase = WordNetDatabase.getFileInstance

var synsets= database.getSynsets("event", SynsetType.NOUN)

var depthMap = Map[NounSynset,Int]()

def traverseSubtree(synset :NounSynset, depth:Int) {
    depthMap += (synset -> depth)
    synset.getHyponyms.foreach(child => traverseSubtree(child, depth+1))
}

traverseSubtree(synsets.head.asInstanceOf[NounSynset], 0)

depthMap.toSeq.sortBy(_._2).foreach(synset => println(synset._1.getWordForms.mkString(",")+"  --  "+synset._1.getDefinition+"  --  "+synset._2))


