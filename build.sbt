name := "its-akka-actor-streams-samples"

version in ThisBuild := "0.0.1"

organization in ThisBuild := "com.its"

scalaVersion in ThisBuild := "2.12.7"

lazy val akkaVersion    = "2.5.19"

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion
    )
  )

lazy val runActorSourceSample = taskKey[Unit]("Run sample: Actor as Source")

runActorSourceSample := (runMain in Compile).toTask(" com.its.akkaactorstreamsamples.actorsource.ActorSourceSample").value
