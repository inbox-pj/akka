package akka.cluster.routing.routing_identity_problem

import akka.config.Config
import akka.actor.{ ActorRef, Actor }
import akka.cluster.{ ClusterTestNode, MasterClusterTestNode, Cluster }

object RoutingIdentityProblemMultiJvmSpec {

  val NrOfNodes = 2

  class SomeActor extends Actor with Serializable {
    println("---------------------------------------------------------------------------")
    println("SomeActor has been created on node [" + Config.nodename + "]")
    println("---------------------------------------------------------------------------")

    def receive = {
      case "identify" ⇒ {
        println("The node received the 'identify' command: " + Config.nodename)
        self.reply(Config.nodename)
      }
    }
  }
}

class RoutingIdentityProblemMultiJvmNode1 extends MasterClusterTestNode {

  import RoutingIdentityProblemMultiJvmSpec._

  val testNodes = NrOfNodes

  "___" must {
    "___" in {
      Cluster.node.start()

      Cluster.barrier("waiting-for-begin", NrOfNodes).await()
      Cluster.barrier("waiting-to-end", NrOfNodes).await()
      Cluster.node.shutdown()
    }
  }
}

class RoutingIdentityProblemMultiJvmNode2 extends ClusterTestNode {

  import RoutingIdentityProblemMultiJvmSpec._

  "deployment of round robin actor" must {
    "obay homenode configuration" in {
      Cluster.node.start()
      Cluster.barrier("waiting-for-begin", NrOfNodes).await()

      Cluster.barrier("get-ref-to-actor-on-node2", NrOfNodes) {}

      val actor = Actor.actorOf[SomeActor]("service-hello")
      val name: String = (actor ? "identify").get.asInstanceOf[String]
      name must equal("node1")

      Cluster.barrier("waiting-to-end", NrOfNodes).await()
      Cluster.node.shutdown()
    }
  }
}
