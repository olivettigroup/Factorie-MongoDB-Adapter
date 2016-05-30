Factorie-MongoDB-Adapter
========
Read raw text from a Mongo collection, process with [Factorie](https://github.com/factorie/factorie), and serialize back to Mongo.

Dependencies
----
Clone and install [nn-depparse](https://github.com/strubell/nn-depparse) to use the neural net dependency parser:
```
git clone git@github.com:strubell/nn-depparse.git
cd nn-depparse
sbt publish
```

Compilation
----
Compile using maven (from root dir of project):
```
mvn compile
```

Running
----
Example using Maven to run:

```
mvn exec:java -Dexec.mainClass="adapter.Adapter" -Dexec.args="--inputDB predsynth --input-collection paragraphs --port-num 27017 --port-name localhost --outputDB outputDB --output-collection outputCollection
```
