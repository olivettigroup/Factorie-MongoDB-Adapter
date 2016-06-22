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
The run script will work on dishwasher:

```
./bin/run.sh
```

Note that to use the nn parser you must specify an intmaps directory and model file (see the run script).
