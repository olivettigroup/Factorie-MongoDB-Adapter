/**
  * Edited by Ao on 2016/4/5.
  */

package adapter

import cc.factorie.app.nlp._
import cc.factorie.app.nlp.parse._
import cc.factorie.app.nlp.pos._
import cc.factorie.app.nlp.segment._
import cc.factorie.db.mongo.MongoCubbieCollection
import cc.factorie.util._
import com.mongodb._

class DocumentStore(mongoDB:String = "predsynth", collectionName:String = "paragraphs") {
    val mongo = new MongoClient()
    val db = mongo.getDB(mongoDB)
    val collection = db.getCollection(collectionName)
    val cubbieCollection = new MongoCubbieCollection[StandardDocumentCubbie](collection, () => new StandardDocumentCubbie)

    //val annotator = DocumentAnnotatorPipeline(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WSJTransitionBasedParser, ParseForwardCoref)
    val annotator = DocumentAnnotatorPipeline(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter, OntonotesForwardPosTagger, WSJTransitionBasedParser)
    //val annotator = DocumentAnnotatorPipeline(DeterministicNormalizingTokenizer, DeterministicSentenceSegmenter)

    def +=(doc:Document): Unit = {
        annotator.process(doc)
        //println(s"Adding doc tokens=${doc.tokenCount}")
        println("Input document:"); println(doc.owplString(annotator))
        cubbieCollection += new StandardDocumentCubbie(doc)
    }
    def ++=(docs:Iterable[Document]): Unit = {
        annotator.processParallel(docs)
        cubbieCollection ++= docs.map(d => new StandardDocumentCubbie(d))
    }

    def +=(file:java.io.File): Unit = {
        val doc = new Document(scala.io.Source.fromFile(file).mkString)
        doc.setName("file:/"+file.toString)
        +=(doc)
    }
    def +=(url:java.net.URL): Unit = {
        val doc = new Document(scala.io.Source.fromInputStream(url.openStream).mkString)
        doc.setName(url.toString)
        +=(doc)
    }
    def +=(docString:String, name:String): Unit = {
        val doc = new Document(docString)
        doc.setName(name)
        +=(doc)
    }

    def show(): Unit = {
        val cubbieIterator = cubbieCollection.iterator
        for (cubbie <- cubbieIterator) {
            val doc = cubbie.document
            println(doc.owplString(annotator))
            println()
        }
        cubbieIterator.close()
    }


    // Scraps not currently used:

    class PosCubbie extends Cubbie {
        val annotator = StringSlot("annotator")
        val annotation = StringSlot("annotation")
        val timestamp = DateSlot("ts")
        val data = IntSeqSlot("data")
    }

    def tokensToIntSeq(doc:Document): IntSeq = {
        val tokenOffsets = new IntArrayBuffer(doc.asSection.tokens.size)
        var o = 0
        for (token <- doc.asSection.tokens) {
            val startOffset = token.stringStart - o
            //println(s"startOffset=$startOffset stringStart=${token.stringStart} stringEnd=${token.stringEnd} o=$o token=${token.string}");
            require(startOffset >= 0 && startOffset < 0xffff)
            val endOffset = token.stringEnd - token.stringStart; require(endOffset >= 0 && endOffset < 0xffff)
            tokenOffsets += (startOffset << 16) + endOffset
            o = token.stringEnd - 1
        }
        tokenOffsets
    }
    def intSeqToTokens(doc:Document, tokenOffsets:IntSeq): Unit = {
        val section = doc.asSection
        val len = tokenOffsets.length; var o = 0; var i = 0; while (i < len) {
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