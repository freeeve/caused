## A simple neo4j unmanaged extension to find caused nodes from a set of root causes
A node is only returned if it is exclusively caused by the set of root cause nodes passed in.

### usage example:

`findcaused` takes a JSON array of node ids, and returns a JSON array of node ids that are exclusively caused by the input.

#### graph looks like this:
```
(10)-[:Causes]->(9)-[:Causes]->(8)<-[:Causes]-(7)
```

#### curl output:
``` shell
$ curl -X POST -H Content-Type:application/json -d '[10,7,8]' http://localhost:7474/caused/findcaused
[10,9,8,7] 
$ curl -X POST -H Content-Type:application/json -d '[10,7]' http://localhost:7474/caused/findcaused
[10,9,8,7] 
$ curl -X POST -H Content-Type:application/json -d '[10]' http://localhost:7474/caused/findcaused
[10,9]
$ curl -X POST -H Content-Type:application/json -d '[10,9]' http://localhost:7474/caused/findcaused
[10,9]
$ curl -X POST -H Content-Type:application/json -d '[9]' http://localhost:7474/caused/findcaused
[] 
$ curl -X POST -H Content-Type:application/json -d '[7]' http://localhost:7474/caused/findcaused
[7]
```

### configuration

Add this line to the end of your neo4j-server.properties file:
`org.neo4j.server.thirdparty_jaxrs_classes=caused=/caused`

Copy from dist `caused_2.10-0.1.jar`, `lift-json_2.10-2.5.jar` and `paranamer-2.4.1.jar` into the `lib/` folder in your neo4j server installation.
