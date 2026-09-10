ThisBuild / scalaVersion := "3.8.4"

val http4sVersion = "0.23.30"
val circeVersion  = "0.14.15"
val tapirVersion  = "1.13.10"

lazy val scalacSettings = Seq(
  semanticdbEnabled := true,
  scalacOptions ++= Seq(
    "-rewrite",
    "-no-indent",
    "-Wunused:all",
    "-Wunused:imports",
    "-deprecation",
    "-Werror",
    "-Wvalue-discard",
    "-Wnonunit-statement",
    "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error"
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .settings(scalacSettings)
  .settings(
    name := "cats-actors-monitor",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      /* cats-actors */
      "com.github.cloudmark.cats-actors" %% "cats-actors" % "2.2.0",
      /* htmfx */
      "com.netherite_systems"            %% "htmfx4"              % "0.1.0-SNAPSHOT",
      /* Tapir http4s bridge */
      "com.softwaremill.sttp.tapir"      %% "tapir-http4s-server" % tapirVersion,
      /* http4s */
      "org.http4s"                       %% "http4s-ember-server" % http4sVersion,
      "org.http4s"                       %% "http4s-dsl"          % http4sVersion,
      "org.http4s"                       %% "http4s-circe"        % http4sVersion,
      /* Circe */
      "io.circe"                         %% "circe-generic"       % circeVersion,
      /* Logging */
      "ch.qos.logback"                    % "logback-classic"     % "1.5.18",
      /* Testing */
      "org.typelevel"                    %% "weaver-cats"         % "0.13.0"               % Test,
      "org.http4s"                       %% "http4s-ember-client" % http4sVersion          % Test,
      "com.github.cloudmark.cats-actors" %% "cats-actors-testkit" % "2.2.0"                % Test
    )
  )

lazy val playground = project
  .in(file("playground"))
  .dependsOn(root)
  .settings(scalacSettings)
  .settings(
    name := "cats-actors-monitor-playground",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      "com.github.cloudmark.cats-actors" %% "cats-actors" % "2.2.0"
    )
  )
