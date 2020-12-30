name := "all"

version := "1.0"

organization := "dev.zerosum"

lazy val test = project.in(file("chapter-testdriven"))

lazy val up = project.in(file("chapter-up-and-running"))

parallelExecution in Test := false
