name := "dcop-algorithms-evaluation"

scalaVersion := "2.11.4"

lazy val dcopAlgorithms = ProjectRef(file("../dcop-algorithms"), "dcop-algorithms")

lazy val root = project.in(file(".")).dependsOn(dcopAlgorithms)
