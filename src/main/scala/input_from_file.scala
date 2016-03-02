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
        val map = collection.mutable.Map[String, Any]()
        map.put("Ao", "Liu")
        map.put("Emma", "Strubell")
        map.put("Andrew", "McCallum")
        val cubbie : Cubbie = new Cubbie(map)
        val myObject = MongoCubbieConverter.eagerDBO(cubbie)
        mongoCollection.insert(myObject)

        val example : BasicDBObject  = new BasicDBObject()

        example.put("name", "MongoDB")
        example.put("type", "database")
        example.put("count", 1)

        val info : BasicDBObject = new BasicDBObject()

        info.put("x", 203)
        info.put("y", 102)

        example.put("info", info)

        mongoCollection.insert(example)
        println(mongoCollection)


        val file = Source.fromFile("src/main/resources/input.json")
        val content = file.mkString
        val doc = new Document(content)
        val sentences = doc.sentences
        var seq : Seq[Sentence] = Seq()
        for (i <- 0 until sentences.size) seq
        println(MongoCubbieConverter.toMongo())
        /*
        val annotators = Seq(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WordNetLemmatizer)
        val pipeline = new DocumentAnnotationPipeline(annotators)

        pipeline.process(doc)
        println(s"sentences: ${doc.sentenceCount} tokens: ${doc.tokenCount}")
        doc.sentences.foreach{s =>
            s.tokens.foreach{t =>
                println(s"${t.positionInSentence}\t${t.string}\t${t.posTag.categoryValue}\t${t.lemma.lemma}")
            }
        }*/
    }
}

object InputFromFile extends App{

}
