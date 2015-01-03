name := "dcop-algorithms-evaluation"

scalaVersion := "2.11.4"

lazy val dcopAlgorithms = ProjectRef(file("../dcop-algorithms"), "dcop-algorithms")

lazy val dcopAlgorithmsEvaluation = Project(id = "dcop-algorithms-evaluation", base = file(".")).dependsOn(dcopAlgorithms)
