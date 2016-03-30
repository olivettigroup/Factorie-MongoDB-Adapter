package adapter

import cc.factorie.db.mongo.{MongoCubbieCollection, MongoCubbieConverter}
import cc.factorie.util.{ArrayIntSeq, Cubbie, IntArrayBuffer, IntSeq}
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

        val mongo = new MongoClient() //new MongoClient("localhost", 27972)
        val db = mongo.getDB("DocumentDB")
        val collection = db.getCollection("documents")

        collection.drop()
        println(collection.getCount())

        /*
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
        */
        /*
        var o : DBObject = null
        while((o = collection.findOne()) != null &&  collection.getCount() > 0){
            collection.remove(o)
            println(o)
        }
        */

        val file = Source.fromFile("src/main/resources/input.json")
        val content = file.mkString
        val map = mutable.Map[String, List[Map[String, String]]]() ++ JSON.parseFull(content).get.asInstanceOf[Map[String,List[Map[String, String]]]]
        println(map)
        val newCubbie : Cubbie = new Cubbie(map.asInstanceOf[mutable.Map[String, Any]])
        val dbo = MongoCubbieConverter.eagerDBO(newCubbie)
        val docsMap = newCubbie._map.apply("corpus").asInstanceOf[List[Map[String, String]]]
        println(dbo)
        println(newCubbie)

        object DocStore extends DocumentStore{
            override val annotator = DocumentAnnotatorPipeline(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WSJTransitionBasedParser)
            override def show(): Unit = {
                val cubbieIterator = cubbieCollection.iterator
                for (cubbie <- cubbieIterator) {
                    val doc = cubbie.document
                    println(doc.owplString(annotator))
                    println()
                }
                cubbieIterator.close()
            }


            override def intSeqToTokens(doc:Document, tokenOffsets:IntSeq): Unit = {
                val section = doc.asSection
                var len = tokenOffsets.length; var o = 0; var i = 0; while (i < len) {
                    val tokenOffset = tokenOffsets(i)
                    val stringStart = o + (tokenOffset >>> 16)
                    val stringEnd = stringStart + (tokenOffset & 0xffff)
                    println(s"to=$tokenOffset stringStart=$stringStart stringEnd=$stringEnd o=$o")
                    o = stringEnd - 1
                    new Token(section, stringStart, stringEnd)
                    i += 1
                }
            }

        }

        val docs = DocStore
        for (d <- docsMap)
            docs += (d("text"), "shakes")
        val ob = collection.findOne()
        println(ob)
        docs.show()

        val cubbie = MongoCubbieConverter.toCubbie(ob)
        //println(cubbie)

        val section = ob.get("section").asInstanceOf[DBObject]

        //val pp = MongoCubbieConverter.toCubbie(section.get("pp")).asInstanceOf[ArrayIntSeq].toSeq
        //println(pp, "\t", pp.length)
        val doc = new Document(ob.get("string").asInstanceOf[String])
        val ts = MongoCubbieConverter.toCubbie(section.get("ts")).asInstanceOf[IntSeq]

        println(ts.length + "\t" + ts.toSeq)
        docs.annotator.process(doc)
        val is = docs.tokensToIntSeq(doc)
        println(is.length + "\t" + is.toSeq)
        //docs.intSeqToTokens(doc, docs.tokensToIntSeq(doc))


        /*
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
        */

        //val annotators = Seq(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WordNetLemmatizer)
        //val pipeline = new DocumentAnnotationPipeline(annotators)
    }
}

class AdapterOptions extends cc.factorie.util.DefaultCmdOptions with SharedNLPCmdOptions{
    val portNum = new CmdOption("portNum", 'p', "", "INT", "The port of the database to use")
    val portName = new CmdOption("portName", 'n', "", "INT", "The port of the database to use")
    val db = new CmdOption("db", "", "STRING", "The database name")
    val collection = new CmdOption("collection", "", "STRING", "The collection name")
}

object ReadDB{
    def main(args: Array[String]): Unit = {
        val mongo = new MongoClient()
        val db = mongo.getDB("predsynth")
        println(db.getCollectionNames)
    }
}
