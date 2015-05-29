package org.bigbluebutton

import akka.actor.{ ActorSystem, Props }
import scala.concurrent.duration._
import redis.RedisClient
import scala.concurrent.{ Future, Await }
import scala.concurrent.ExecutionContext.Implicits.global
import org.bigbluebutton.endpoint.redis.RedisPublisher
import org.bigbluebutton.endpoint.redis.AppsRedisSubscriberActor
import org.bigbluebutton.core.api.MessageOutGateway
import org.bigbluebutton.core.api.IBigBlueButtonInGW
import org.bigbluebutton.core.BigBlueButtonGateway
import org.bigbluebutton.core.BigBlueButtonInGW
import org.bigbluebutton.core.MessageSender
import org.bigbluebutton.core.pubsub.receivers.RedisMessageReceiver
import org.bigbluebutton.core.api.OutMessageListener2
import org.bigbluebutton.core.pubsub.senders._

object Boot extends App with SystemConfiguration {

  implicit val system = ActorSystem("bigbluebutton-apps-system")

  val redisPublisher = new RedisPublisher(system)
  val msgSender = new MessageSender(redisPublisher)

  val chatSender = new ChatEventRedisPublisher(msgSender)
  val meetingSender = new MeetingEventRedisPublisher(msgSender)
  val presSender = new PresentationEventRedisPublisher(msgSender)
  val userSender = new UsersEventRedisPublisher(msgSender)
  val whiteboardSender = new WhiteboardEventRedisPublisher(msgSender)

  val senders = new java.util.ArrayList[OutMessageListener2]()
  senders.add(chatSender)
  senders.add(meetingSender)
  senders.add(presSender)
  senders.add(userSender)
  senders.add(whiteboardSender)

  val outGW = new MessageOutGateway(senders)
  val bbbGW = new BigBlueButtonGateway(system, outGW)
  val bbbInGW = new BigBlueButtonInGW(bbbGW)
  val redisMsgReceiver = new RedisMessageReceiver(bbbInGW)

  val redisSubscriberActor = system.actorOf(AppsRedisSubscriberActor.props(redisMsgReceiver), "redis-subscriber")
}