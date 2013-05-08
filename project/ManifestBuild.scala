package manifest

import sbt._
import Keys._

object ManifestBuild extends Build {
  lazy val root =  Project(
    id = "manifest",
    base = file(".")
  )
}