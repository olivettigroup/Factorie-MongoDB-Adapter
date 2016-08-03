/**
  * Edited by Ao on 2016/4/5.
  */

package adapter

import cc.factorie.app.nlp._
import cc.factorie.app.nlp.ner.NerTag
import cc.factorie.app.nlp.pos.{PennPosTag, PosTag}
import cc.factorie.db.mongo.MongoCubbieCollection
import cc.factorie.util._
import com.mongodb._
import edu.umass.cs.iesl.nndepparse.StanfordParseTree

class DocumentStore(pipelineComponents: Seq[DocumentAnnotator], mongoDB:String = "predsynth", collectionName:String = "papers") {
    val mongo = new MongoClient()
    val db = mongo.getDB(mongoDB)
    val collection = db.getCollection(collectionName)
    val cubbieCollection = new MongoCubbieCollection[StandardDocumentCubbie](collection, () => new StandardDocumentCubbie)

    val annotator = new DocumentAnnotationPipeline(pipelineComponents)

    def +=(doc:Document): Unit = {
        for (para <- doc.get("paragraphs")){
          annotator.process(para)
          //println(s"Adding doc tokens=${doc.tokenCount}")
          //println("Input document:"); println(doc.owplString(annotator))
        }
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

class StanfordParseSectionAnnotationsCubbie extends DocumentCubbie {
  def this(section:Section) = { this(); this := section }
  val start = IntSlot("start")
  val end = IntSlot("end")
  val ts = SectionTokensAndSentencesSlot("ts")
  val pp = StanfordParseSectionPosAndParseSlot("pp")
  val ner = SectionConllNerTagsSlot("ner")
  def :=(section:Section): this.type = {
    start := section.stringStart
    end := section.stringEnd
    ts := section
    if (section.document.annotatorFor(classOf[PosTag]).isDefined && section.sentences.head.attr.contains(classOf[StanfordParseTree])) pp := section
    if (section.document.annotatorFor(classOf[NerTag]).isDefined) ner := section
    this
  }
  def =:(document:Document): this.type = {
    val section = new BasicSection(document, start.value, end.value); document += section
    section =: ts
    if (pp.isDefined) section =: pp
    if (ner.isDefined) section =: ner
    this
  }

  /** Store together in one IntSeq the part-of-speech tags for every Token within a Section and the ParseTree for every Sentence within a Section. */
  class StanfordParseSectionPosAndParseSlot(name:String) extends IntSeqSlot(name) {
    def :=(section:Section): this.type = {
      val indices = new IntArrayBuffer(section.tokens.length * 3)
      for (token <- section.tokens) indices += token.attr[PennPosTag].intValue
      for (sentence <- section.sentences) {
        val l = indices.length
        val parse = sentence.parse
        indices ++= parse.parents
        indices ++= parse.labels.map(_.intValue)
        assert(l+ 2*sentence.length == indices.length)
      }
      this := indices
      this
    }
    def =:(section:Section): this.type = {
      val indices = this.value; var i = 0
      for (token <- section.tokens) {
        token.attr += new PennPosTag(token, indices(i))
        i += 1
      }
      for (sentence <- section.sentences) {
        val parents = indices.slice(i, i+sentence.length).asArray
        val labels = indices.slice(i+sentence.length, i+2*sentence.length).asArray
        //println(s"n=${parents.length} parents = ${parents.mkString(",")}\n labels = ${labels.mkString(",")}")
        val parse = new StanfordParseTree(sentence, parents, labels)
        sentence.attr += parse
        i += 2*sentence.length
      }
      section.document.annotators(classOf[PennPosTag]) = this.getClass
      section.document.annotators(classOf[StanfordParseTree]) = this.getClass
      this
    }
  }
  object StanfordParseSectionPosAndParseSlot { def apply(name:String) = new StanfordParseSectionPosAndParseSlot(name) }
}

class StanfordParseDocumentCubbie extends DocumentCubbie {
  def this(document:Document) = { this(); this := document }
  val string = StringSlot("string")
  val name = StringSlot("name")
  val date = DateSlot("date")
  val sections = CubbieListSlot("sections", () => new StanfordParseSectionAnnotationsCubbie) // Only present if there are multiple Sections
  val section = CubbieSlot("section", () => new StanfordParseSectionAnnotationsCubbie) // Only present if there is one Section
  def :=(document:Document): this.type = {
    name := document.name
    date := new java.util.Date()
    string := document.string
    if (document.sections.length == 1 && document.sections.head == document.asSection)
      section := new StanfordParseSectionAnnotationsCubbie(document.asSection)
    else
      sections := document.sections.map(section => new StanfordParseSectionAnnotationsCubbie(section))
    this
  }
  def document: Document = {
    val doc = new Document(string.value)
    doc.setName(name.value)
    doc.attr += date.value
    if (sections.isDefined) {
      for (sc <- sections.value)
        doc =: sc
    }
    else { assert(section.isDefined); doc =: section.value }
    doc
  }
}
