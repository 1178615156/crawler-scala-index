name := "crawler-scala-index"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.5.9"

libraryDependencies += "org.jsoup" % "jsoup" % "1.10.1"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.12"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.12"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.4.12"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % Test
libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.7"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
libraryDependencies += "org.codehaus.groovy" % "groovy" % "2.4.6"

packAutoSettings
