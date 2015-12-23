
lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization := "org.nlp4l",
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "NLP4L-framework-library",
    version := "0.1.0",
    scalacOptions := Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    parallelExecution := true,
    libraryDependencies ++= Seq(
       "joda-time" % "joda-time" % "2.7",
       "org.joda" % "joda-convert" % "1.7"
    )
  )

