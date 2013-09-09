## A simple neo4j unmanaged extension to find caused nodes from a set of root causes

### usage example:

#### graph looks like this:
```
(10)-[:Caused]->(9)-[:Caused]->(8)<-[:Caused]-(7)
```

#### curl output:
``` shell
$ curl -X POST --data-urlencode "ids=[10,7,8]" http://localhost:7474/caused/caused/findcaused
[10,9,8,7] 
$ curl -X POST --data-urlencode "ids=[10,7]" http://localhost:7474/caused/caused/findcaused
[10,9,8,7] 
$ curl -X POST --data-urlencode "ids=[10]" http://localhost:7474/caused/caused/findcaused
[10,9]
$ curl -X POST --data-urlencode "ids=[10,9]" http://localhost:7474/caused/caused/findcaused
[10,9]
$ curl -X POST --data-urlencode "ids=[9]" http://localhost:7474/caused/caused/findcaused
[] 
$ curl -X POST --data-urlencode "ids=[7]" http://localhost:7474/caused/caused/findcaused
[7]
```

### configuration

Add this line to the end of your neo4j-server.properties file:
`org.neo4j.server.thirdparty_jaxrs_classes=caused=/caused`

Copy from dist `caused_2.10-0.1.jar`, `lift-json_2.10-2.5.jar` and `paranamer-2.4.1.jar` into the `lib/` folder in your neo4j server installation.
