package adapter

import java.io.FileReader

import cc.factorie.db.mongo.MongoCubbieConverter
import cc.factorie.util.Cubbie
import org.bson._
import com.mongodb._
import scala.collection.mutable
import scala.util.parsing.json.JSON

import scala.io._
import cc.factorie.app.nlp.lemma.WordNetLemmatizer
import cc.factorie.app.nlp.{DocumentStore, Sentence, DocumentAnnotationPipeline, Document}
import cc.factorie.app.nlp.pos.OntonotesForwardPosTagger
import cc.factorie.app.nlp.segment.{DeterministicSentenceSegmenter, DeterministicNormalizingTokenizer}

object Adapter{
    def main(args: Array[String]) {

        val mongo = new MongoClient()
        val db = mongo.getDB("Test")
        val collection = db.getCollection("documents")
        
        collection.drop()
        println(collection.getCount())

        val myMap = mutable.Map[String, Any]()
        myMap.put("Ao", "Liu")
        myMap.put("Emma", "Strubell")
        myMap.put("Andrew", "McCallum")
        val cubbie : Cubbie = new Cubbie(myMap)
        val myObject = MongoCubbieConverter.eagerDBO(cubbie)

        //collection.remove(myObject)
        collection.insert(myObject)
        //println(collection.findOne())

        val example : DBObject  = new BasicDBObject
        example.put("name", "MongoDB")
        example.put("type", "database")
        example.put("count", 1)
        val info : DBObject = new BasicDBObject
        info.put("x", 203)
        info.put("y", 102)

        example.put("info", info)

        //collection.remove(example)
        collection.insert(example)
        //println(collection.findOne())

        /*
        var o : DBObject = null
        while((o = collection.findOne()) != null &&  collection.getCount() > 0){
            collection.remove(o)
            println(o)
        }
        */

        val file = Source.fromFile("src/main/resources/input.json")
        val content = file.mkString
        val map = mutable.Map[String, Any]() ++ JSON.parseFull(content).get.asInstanceOf[Map[String, Any]]
        val mongoCotent = MongoCubbieConverter.toMongo(content)
        val newCubbie : Cubbie = new Cubbie(map)
        val dbo = MongoCubbieConverter.eagerDBO(newCubbie)
        val c = collection.findOne()

        println(dbo)
        val doc = new DocumentStore(db.getName)
        //doc += new Document(content)
        
        /*
        val doc = new Document(content)
        val annotators = Seq(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WordNetLemmatizer)
        val pipeline = new DocumentAnnotationPipeline(annotators)

        pipeline.process(doc)
        println(s"sentences: ${doc.sentenceCount} tokens: ${doc.tokenCount}")
        doc.sentences.foreach{s =>
            s.tokens.foreach{t =>
                println(s"${t.positionInSentence}\t${t.string}\t${t.posTag.categoryValue}\t${t.lemma.lemma}")
            }
        }
        */
    }
}

object InputFromFile extends App{

}
