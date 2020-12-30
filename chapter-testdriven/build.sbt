name := "testdriven"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val akkaVersion = "2.6.10"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"               % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % "test",
    "org.scalatest"     %% "scalatest"                % "3.2.2"     % "test"
  )
}
