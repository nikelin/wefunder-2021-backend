import sbt._
import sbt.Keys._

name := "wefunder-pitchdeck-backend"

version := "0.1"

scalaVersion := "2.13.4"

Test / fork := true

enablePlugins(JavaAppPackaging)

libraryDependencies += "co.fs2" %% "fs2-core" % Versions.fs2Version
libraryDependencies += "co.fs2" %% "fs2-io"   % Versions.fs2Version

libraryDependencies += "org.tpolecat" %% "doobie-hikari"   % Versions.doobieVersion
libraryDependencies += "org.tpolecat" %% "doobie-postgres" % Versions.doobieVersion
libraryDependencies += "org.tpolecat" %% "doobie-core"     % Versions.doobieVersion
libraryDependencies += "org.tpolecat" %% "doobie-quill"    % Versions.doobieVersion exclude ("io.getquill", "quill-jdbc")

libraryDependencies += "io.getquill" %% "quill-jdbc" % Versions.quillVersion

libraryDependencies += "org.http4s" %% "http4s-dsl"          % Versions.http4sVersion
libraryDependencies += "org.http4s" %% "http4s-circe"        % Versions.http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % Versions.http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-client" % Versions.http4sVersion

libraryDependencies += "io.circe" %% "circe-fs2"            % Versions.circeVersion
libraryDependencies += "io.circe" %% "circe-core"           % Versions.circeVersion
libraryDependencies += "io.circe" %% "circe-generic"        % Versions.circeVersion
libraryDependencies += "io.circe" %% "circe-generic-extras" % Versions.circeVersion
libraryDependencies += "io.circe" %% "circe-parser"         % Versions.circeVersion

libraryDependencies += "com.lihaoyi" %% "pprint" % Versions.pprintVersion

libraryDependencies += "org.apache.tika"   % "tika-core" % Versions.tikaVersion
libraryDependencies += "org.apache.pdfbox" % "pdfbox"    % Versions.pdfboxVersion
libraryDependencies += "org.apache.poi"    % "poi-ooxml" % Versions.apachePoiVersion

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"   % Versions.scalaLoggingVersion
libraryDependencies += "ch.qos.logback"              % "logback-classic" % Versions.logbackVersion

libraryDependencies += "com.github.pureconfig" %% "pureconfig-enum"        % Versions.pureConfigVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig"             % Versions.pureConfigVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig-cats-effect" % Versions.pureConfigVersion
