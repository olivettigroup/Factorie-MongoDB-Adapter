Factorie-MongoDB-Adapter
========
Read raw text from a Mongo collection, process with [Factorie](https://github.com/factorie/factorie), and serialize back to Mongo.

Running
----
Example using Maven to run:

```
mvn exec:java -Dexec.mainClass="adapter.Adapter" -Dexec.args="--inputDB predsynth --input-collection paragraphs --port-num 27017 --port-name localhost --outputDB outputDB --output-collection outputCollection
```
