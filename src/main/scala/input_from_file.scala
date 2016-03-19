package adapter

import cc.factorie.db.mongo.{MongoCubbieCollection, MongoCubbieConverter}
import cc.factorie.util.Cubbie
import org.bson._
import com.mongodb._

import scala.collection.mutable
import scala.util.parsing.json.JSON
import scala.io._
import cc.factorie.app.nlp.lemma.WordNetLemmatizer
import cc.factorie.app.nlp._
import cc.factorie.app.nlp.parse.WSJTransitionBasedParser
import cc.factorie.app.nlp.pos.OntonotesForwardPosTagger
import cc.factorie.app.nlp.segment.{DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter}

object Adapter{
    def main(args: Array[String]) {

        val mongo = new MongoClient("localhost", 27972)
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
        val newCubbie : Cubbie = new Cubbie(map)
        val dbo = MongoCubbieConverter.eagerDBO(newCubbie)
        val docsMap = newCubbie._map.apply("corpus").asInstanceOf[List[Map[String, String]]]
        println(dbo)
        println(newCubbie)

        val cubbieCollection = new MongoCubbieCollection[StandardDocumentCubbie](collection, () => new StandardDocumentCubbie)
        val annotator = DocumentAnnotatorPipeline(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WSJTransitionBasedParser)

        val docsInfo = mutable.Map[String, Any]()
        var docInfoList : List[mutable.Map[String, Any]] = Nil
        for (d <- docsMap) {
            val doc = new Document(d("text"))
            val docInfo = mutable.Map[String, Any]()
            var textsInfo : List[mutable.Map[String, Any]] = Nil
            annotator.process(doc)
            println("Input document:")
            println(d("text"))
            cubbieCollection += new StandardDocumentCubbie(doc)
            println(s"sentences: ${doc.sentenceCount} tokens: ${doc.tokenCount}")
            doc.tokens.foreach{t =>
                //if (!t.string.matches("[\\.,'\"!/\\-:;]+")) {
                    val tokenInfo = mutable.Map[String, Any]()
                    println(s"${t.positionInSection}\t${t.stringStart}\t${t.stringEnd}\t${t.string}\t${t.posTag.categoryValue}\t${t.lemma.lemma}")
                    tokenInfo.put("position", t.positionInSection)
                    tokenInfo.put("start", t.stringStart)
                    tokenInfo.put("end", t.stringEnd)
                    tokenInfo.put("POS", t.posTag.categoryValue)
                    tokenInfo.put("lemma", t.lemma.lemma)
                    if (t.string != ".")
                        textsInfo :+= mutable.Map[String, Any]((t.string, tokenInfo))
                    else
                        textsInfo :+= mutable.Map[String, Any](("\"period\"", tokenInfo))
            }
            docInfo.put("docNum", d("docNum"))
            docInfo.put("textsInfo", textsInfo)
            docInfoList :+= docInfo
        }
        docsInfo.put("docs", docInfoList)
        val parseCubbie = new Cubbie(docsInfo)
        println(parseCubbie)
        val parseDBO = MongoCubbieConverter.eagerDBO(parseCubbie)
        collection.insert(parseDBO)

        //val annotators = Seq(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WordNetLemmatizer)
        //val pipeline = new DocumentAnnotationPipeline(annotators)
    }
}

object InputFromFile extends App{

}
