package adapter

import cc.factorie.app.nlp.SharedNLPCmdOptions
import cc.factorie.app.nlp.parse.WSJTransitionBasedParser
import cc.factorie.app.nlp.pos.OntonotesForwardPosTagger
import cc.factorie.app.nlp.segment.{DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter}
import com.mongodb.MongoClient
import com.mongodb.{BasicDBObject, DBObject}
import edu.umass.cs.iesl.nndepparse.FeedForwardNNParser

import scala.collection.mutable
import scala.io.Source
import scala.util.parsing.json.JSON

/**
  * Created by Ao on 2016/2/8.
  */
object Adapter {
    def main(args: Array[String]) {
        val opts = new AdapterOptions
        opts.parse(args)
        assert(opts.inputDB.wasInvoked || opts.inputCollection.wasInvoked)

        // set the port to use
        val mongo = new MongoClient(opts.portName.value, opts.portNum.value)

        // set the DB and Collection of input and output files
        val inputDB = mongo.getDB(opts.inputDB.value)
        val inputCollection = inputDB.getCollection(opts.inputCollection.value)
        val outputDB = mongo.getDB(opts.outputDB.value)
        val outputCollection = inputDB.getCollection(opts.outputCollection.value)

        /*
        // used while testing
        inputCollection.drop()
        val file = Source.fromFile("src/main/resources/input.json")
        val content = file.mkString
        val map = mutable.Map[String, List[Map[String, String]]]() ++ JSON.parseFull(content).get.asInstanceOf[Map[String, List[Map[String, String]]]]
        println(map)*/

        /*
        val newCubbie : Cubbie = new Cubbie(map.asInstanceOf[mutable.Map[String, Any]])
        val dbo = MongoCubbieConverter.eagerDBO(newCubbie)
        val docsMap = newCubbie._map("corpus").asInstanceOf[List[Map[String, String]]]
        //println(dbo)
        //println(newCubbie)
        */

        /*
        val corpus = map("corpus")
        for (i <- corpus.indices) {
          for (para <- corpus(i).get("paragraphs")) {
            val o: DBObject = new BasicDBObject
            o.put("text", para.get("text"))
            inputCollection.insert(o)
          }
        }
        */

      val FeedForwardNNParser = new FeedForwardNNParser(opts.modelFile.value, opts.mapsDir.value, opts.numToPrecompute.value)


      // Factorie DocumentAnnotators to run
        val pipelineComponents = Seq(
            DeterministicNormalizingTokenizer,
            DeterministicSentenceSegmenter,
            OntonotesForwardPosTagger,
            WSJTransitionBasedParser
        )


        //add the parsed docs to the output collection
        val docs = new DocumentStore(pipelineComponents, outputDB.getName, outputCollection.toString)
        //docs.collection.drop() //used while testing
        val cursor = inputCollection.find
        while (cursor.hasNext) {
            val next = cursor.next.toMap
            //docs +=(next.get("text").toString, next.get("_id").toString)
            docs +=(next)
        }

        docs.show()
        val ob = docs.collection.findOne
        println(ob)
    }
}

class AdapterOptions extends cc.factorie.util.DefaultCmdOptions with SharedNLPCmdOptions {
  val portNum = new CmdOption("port-num", 'p', 27017, "INT", "The port of the database to use", false)
  val portName = new CmdOption("port-name", 'n', "", "STRING", "Hostname of the database to use", false)
  val inputDB = new CmdOption("inputDB", "predsynth", "STRING", "The input database name", false)
  val inputCollection = new CmdOption("input-collection", "papers", "STRING", "The input collection name", false)
  val outputDB = new CmdOption("outputDB", "predsynth", "STRING", "The output database name", false)
  val outputCollection = new CmdOption("output-collection", "parsed_papers", "STRING", "The output collection name", false)
  val numToPrecompute = new CmdOption("precompute-words", -1, "INT", "Number of word embeddings to precompute")
  val mapsDir = new CmdOption("maps-dir", "", "STRING", "Dir under which to look for existing maps to use; If empty write new maps")
  val modelFile = new CmdOption("model", "", "STRING", "Serialized model in HDF5 format")
}
