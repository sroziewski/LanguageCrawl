package actor

import actor.GenericRouter.PoisonChildren
import akka.actor.{Terminated, _}
import akka.routing._

import scala.reflect.ClassTag


object GenericRouter {
  def makeRouter[W <: Actor : ClassTag, T: ClassTag](routerName: String,
                                                     context: ActorContext,
                                                     factory: Option[Unit => W ]= None) = {
    context.actorOf(Props(new GenericRouter[W, T](routerName, factory)), "generic_" + routerName)
  }
  case object PoisonChildren
}

class GenericRouter[W <: Actor : ClassTag, T: ClassTag](configName: String,
                                                        factory: Option[Unit => W]) extends Actor with ActorLogging {
  val router = factory match {
    case None => context.actorOf(Props[W].withRouter(FromConfig()), name = configName)
    case Some(creator) => context.actorOf(Props[W](creator()).withRouter(FromConfig()), name = configName)
  }
  context.watch(router)
  def receive: Receive = {
    case PoisonChildren => router ! Broadcast(PoisonPill)
    case Terminated(corpse) => shutDownGracefully(corpse)
    case m: T => router.tell(m, sender())
  }
  def shutDownGracefully(corpse: ActorRef): Unit = {
    if (corpse == router) {
      context.stop(self)
    }
  }
//  val escalator = OneForOneStrategy() {
////    case e ? testActor ! e; SupervisorStrategy.Escalate
//  }
}