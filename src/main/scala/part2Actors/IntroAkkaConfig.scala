package part2Actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}

object IntroAkkaConfig extends App {
  private class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Message received ${message.toString}")
    }
  }

  /**
   *  1 - Inline configuration
   */
  private val configString ="""
      | akka {
      |   loglevel = "ERROR"
      | }
      |""".stripMargin
  private val config: Config = ConfigFactory.parseString(configString)
  private val system: ActorSystem = ActorSystem("TestConfig", ConfigFactory.load(config))
  private val testActor: ActorRef = system.actorOf(Props[SimpleLoggingActor])
  testActor ! "test message"

  /**
   * 2 - Default config file (MOST COMMON WAY)
   * (Check the resource/application.conf)
   */
  private val anotherSystem: ActorSystem = ActorSystem("DefaultConfigFileDemo")
  private val defaultConfigActor: ActorRef = anotherSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"

  /**
   * 3 - Separate configuration in the same file
   * (Check the resource/application.conf)
   */
  private val mySpecialConfig: Config = ConfigFactory.load().getConfig("mySpecialConfig")
  private val anotherSystemWithSpecialConf: ActorSystem = ActorSystem("SpecialConfig", mySpecialConfig)
  private val specialConfigActor: ActorRef = anotherSystemWithSpecialConf.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "Wow special"

  /**
   * 4 - Separate configuration file
   * (Check the resource/application.conf)
   */
  private val fileConfig: Config = ConfigFactory.load("secretFolder/secretConfig.conf")
  private val anotherSystemWithSeparateFileConf: ActorSystem = ActorSystem("SpecialConfig", fileConfig)
  private val separateFileActor: ActorRef = anotherSystemWithSeparateFileConf.actorOf(Props[SimpleLoggingActor])
  separateFileActor ! "Wow separate"




}
