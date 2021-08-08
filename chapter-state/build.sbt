libraryDependencies ++= {
  val akkaVersion = "2.6.15"
  Seq(
    "com.typesafe.akka"   %% "akka-actor-typed"         % akkaVersion,
    "com.typesafe.akka"   %% "akka-slf4j"               % akkaVersion,
    "ch.qos.logback"       % "logback-classic"          % "1.2.3",
    "net.logstash.logback" % "logstash-logback-encoder" % "6.6",
    "com.typesafe.akka"   %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "org.scalatest"       %% "scalatest"                % "3.2.9"     % Test
  )
}
