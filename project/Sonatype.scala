import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.autoImport._

object Sonatype extends AutoPlugin {
  override def requires = JvmPlugin && xerial.sbt.Sonatype
  override def trigger = allRequirements
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    publishTo := sonatypePublishToBundle.value,
    // sonatypeProfileName := organization.value
    publishMavenStyle := true,
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    sonatypeProjectHosting := Some(GitHubHosting("giabao", "asm-flow", "thanhbv@sandinh.com"))
  )
}
