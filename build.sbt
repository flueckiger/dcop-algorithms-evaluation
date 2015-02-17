name := "dcop-algorithms-evaluation"

scalaVersion := "2.11.4"

lazy val dcopAlgorithmsEvaluation = Project(id = "dcop-algorithms-evaluation", base = file(".")).dependsOn(dcopAlgorithms, signalCollect)

lazy val dcopAlgorithms = ProjectRef(file("../dcop-algorithms"), "dcop-algorithms")

lazy val signalCollect = ProjectRef(file("../signal-collect"), "signal-collect")

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test"
)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.classpathTransformerFactories := Seq(ResourcesTransformer)
