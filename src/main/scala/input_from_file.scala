package adapter

import cc.factorie.db.mongo.MongoCubbieConverter
import cc.factorie.util.Cubbie
import org.bson._
import com.mongodb._



import scala.io._
import cc.factorie.app.nlp.lemma.WordNetLemmatizer
import cc.factorie.app.nlp.{Sentence, DocumentAnnotationPipeline, Document}
import cc.factorie.app.nlp.pos.OntonotesForwardPosTagger
import cc.factorie.app.nlp.segment.{DeterministicSentenceSegmenter, DeterministicNormalizingTokenizer}

object Adapter{
    def main(args: Array[String]) {
        val mongoClient = new MongoClient("localhost", 27972)
        val mongoDB = mongoClient.getDB("MyTest")
        val mongoCollection = mongoDB.getCollection("MyCollection")

        mongoCollection.drop()
        println(mongoCollection.getCount())
        val map = collection.mutable.Map[String, Any]()
        map.put("Ao", "Liu")
        map.put("Emma", "Strubell")
        map.put("Andrew", "McCallum")
        val cubbie : Cubbie = new Cubbie(map)
        val myObject = MongoCubbieConverter.eagerDBO(cubbie)

        //mongoCollection.remove(myObject)
        mongoCollection.insert(myObject)
        //println(mongoCollection.findOne())

        val example : DBObject  = new BasicDBObject
        example.put("name", "MongoDB")
        example.put("type", "database")
        example.put("count", 1)
        val info : DBObject = new BasicDBObject
        info.put("x", 203)
        info.put("y", 102)

        example.put("info", info)

        //mongoCollection.remove(example)
        mongoCollection.insert(example)
        println(mongoCollection.findOne())

        var o : DBObject = null
        while((o = mongoCollection.findOne()) != null &&  mongoCollection.getCount() > 0){
            mongoCollection.remove(o)
            println(o)
        }

        /*
        val file = Source.fromFile("src/main/resources/input.json")
        val content = file.mkString
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
