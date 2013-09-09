package caused

import org.neo4j.graphdb.{GraphDatabaseService, Direction, Node, Relationship, PropertyContainer, DynamicRelationshipType}
import org.neo4j.kernel.Traversal

import javax.ws.rs.{Path, POST, Produces, FormParam}
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType

import scala.collection.JavaConverters._

import net.liftweb.json._
import net.liftweb.json.Extraction._

@Path("/caused")
class caused {

  implicit val formats = net.liftweb.json.DefaultFormats

  val causesType = DynamicRelationshipType.withName( "Causes" )

  @POST
  @Path("/findcaused/")
  @Produces(Array("application/json"))
  def findCaused(@FormParam("ids")jsonIds:String, @Context db:GraphDatabaseService) = {
    val tx = db.beginTx
    try {
      val ids:List[Long] = Serialization.read[List[Long]](jsonIds)
      val nodes:List[Node] = ids.map(e => db.getNodeById(e))
      val rootNodes = nodes.filter(n => !n.hasRelationship(causesType, Direction.INCOMING))
      val outCausesExpander = Traversal.pathExpanderForTypes(causesType, Direction.OUTGOING)
      val inCausesExpander = Traversal.pathExpanderForTypes(causesType, Direction.INCOMING)
      val td = Traversal.description.expand(outCausesExpander)
      val traverser = td.traverse(rootNodes:_*) // :_* expands a list so varargs work
      val potentialCausedNodes = traverser.nodes.asScala
      potentialCausedNodes.map(n => tx.acquireWriteLock(n)) // this will hopefully make this thread safe
      //println(potentialCausedNodes.map(e => e.getId)) // debug
      val causedNodes = potentialCausedNodes.filter(n => {
        //println("checking potential caused node: " + n.getId)
        val backtd = Traversal.description.expand(inCausesExpander)
        val backTraverser = backtd.traverse(n)
        var canBeDeleted = true
        for(path <- backTraverser.iterator().asScala) {
          //val pathstr = path.nodes.asScala.map(n => ""+n.getId).foldLeft("")((acc, e) => acc + e + ", ")
          //println("checking path: " + pathstr)
          if(!path.endNode.hasRelationship(causesType, Direction.INCOMING) // is the farthest we can go in this path
          && !rootNodes.contains(path.endNode)) { // is one of our root nodes
            canBeDeleted = false // this caused node has a root cause not in our start list
            //println("found non-root node: " + path.endNode.getId)
          }
        }
        canBeDeleted
      })
      val causedIds = causedNodes.map(n => n.getId).toList
      // causedNodes.map(n => n.delete) // uncomment this if you want to delete the nodes
      tx.success
      Response.ok(compact(render(decompose(causedIds))), MediaType.APPLICATION_JSON).build()
    } catch {
      case e:Exception => Response.status(500).entity(e.getMessage).build()
    } finally {
      tx.finish
    }
  }

}
