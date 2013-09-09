package findpaths

import org.neo4j.graphdb.{GraphDatabaseService, Direction, Node, Relationship, PropertyContainer, DynamicRelationshipType}
import org.neo4j.kernel.Traversal
import org.neo4j.graphdb.traversal.TraversalDescription

import javax.ws.rs._
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType

import scala.collection.JavaConverters._

import net.liftweb.json._
import net.liftweb.json.Extraction._

@Path("/caused")
class findpaths {

  implicit val formats = net.liftweb.json.DefaultFormats

  val causesType = DynamicRelationshipType.withName( "CAUSES" )

  @POST
  @Path("/findcaused/")
  @Produces(Array("application/json"))
  def findCaused(@FormParam("ids")jsonIds:String, @Context db:GraphDatabaseService) = {
    val tx = db.beginTx
    try {
      val ids:List[Int] = Serialization.read(jsonIds)
      val nodes:List[Node] = ids.map(e => db.getNodeById(e))
      val rootNodes = nodes.filter(n => !n.hasRelationship(causesType, Direction.INCOMING))
      val outCausesExpander = Traversal.pathExpanderForTypes(causesType, Direction.OUTGOING)
      val inCausesExpander = Traversal.pathExpanderForTypes(causesType, Direction.INCOMING)
      val td = Traversal.description
      td.depthFirst
      td.expand(outCausesExpander)
      val traverser = td.traverse(rootNodes:_*) // :_* expands a list so varargs work
      val potentialCausedNodes = traverser.nodes.asScala
      potentialCausedNodes.map(n => tx.acquireWriteLock(n)) // this will hopefully make this thread safe
      val causedNodes = potentialCausedNodes.filter(n => {
        val backtd = Traversal.description
        backtd.depthFirst
        backtd.expand(inCausesExpander)
        val backTraverser = backtd.traverse(n)
        var canBeDeleted = true
        for(path <- backTraverser.iterator().asScala) {
          if(!rootNodes.contains(path.endNode)) {
            canBeDeleted = false // this caused node has a root cause not in our start list
          }
        }
        canBeDeleted
      })
      val causedIds = causedNodes.map(n => n.getId).toList
      tx.success
      Response.ok(compact(render(decompose(causedIds))), MediaType.APPLICATION_JSON).build()
    } finally {
      tx.finish
    }
    Response.status(500).build()
  }

}
