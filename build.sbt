name := "crawler-scala-index"

version := "1.0"

scalaVersion := "2.11.8"
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ws" % "2.5.9"
  , "org.jsoup" % "jsoup" % "1.10.1"
  , "org.slf4j" % "slf4j-api" % "1.7.12"
  , "ch.qos.logback" % "logback-classic" % "1.1.3"
  , "ch.qos.logback" % "logback-core" % "1.1.3"
  , "com.typesafe.akka" %% "akka-actor" % "2.4.12"
  , "com.typesafe.akka" %% "akka-persistence" % "2.4.12"
  , "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % Test
  , "org.iq80.leveldb" % "leveldb" % "0.7"
  , "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
  , "org.codehaus.groovy" % "groovy" % "2.4.6"
  , "org.scalaj" %% "scalaj-http" % "2.3.0"
  , "org.scalatest" %% "scalatest" % "3.0.1"
)

packAutoSettings
packGenerateWindowsBatFile := false
