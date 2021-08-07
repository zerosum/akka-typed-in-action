name := "all"

version := "1.0"

organization := "dev.zerosum"

lazy val test = project.in(file("chapter-testdriven"))

lazy val up = project.in(file("chapter-up-and-running"))

lazy val fsm = project.in(file("chapter-state"))

Test / parallelExecution := false
