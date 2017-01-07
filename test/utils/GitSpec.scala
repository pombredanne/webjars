package utils


import java.io.BufferedInputStream
import java.net.URI

import akka.util.Timeout
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import play.api.test._

import scala.concurrent.duration._

class GitSpec extends PlaySpecification with GlobalApplication {

  override implicit def defaultAwaitTimeout: Timeout = 120.seconds

  lazy val git: Git = application.injector.instanceOf[Git]

  "git versions" should {
    "not work with an invalid git url" in {
      await(git.versions("foo/bar")) must throwA[Exception]
    }
    "work with git://host/path.git" in {
      val versions = await(git.versions("git://github.com/mochajs/mocha.git"))
      versions.length must beGreaterThan (0)
    }
    "work with git+http://host/path.git" in {
      val versions = await(git.versions("git+http://github.com/mochajs/mocha.git"))
      versions.length must beGreaterThan (0)
    }
    "work with git+https://host/path.git" in {
      val versions = await(git.versions("git+https://github.com/mochajs/mocha.git"))
      versions.length must beGreaterThan (0)
    }
    "work with http://host/path.git" in {
      val versions = await(git.versions("http://github.com/mochajs/mocha.git"))
      versions.length must beGreaterThan (0)
    }
    "work with https://host/path.git" in {
      val versions = await(git.versions("https://github.com/mochajs/mocha.git"))
      versions.length must beGreaterThan (0)
    }
    "work with githuborg/repo" in {
      val versions = await(git.versions("mochajs/mocha"))
      versions.length must beGreaterThan (0)
    }
    "work with a redirect" in {
      val versions = await(git.versions("visionmedia/mocha"))
      versions.length must beGreaterThan (0)
    }
  }

  "git file" should {
    "fetch a file with a version" in {
      val file = await(git.file("mochajs/mocha", Some("2.2.5"), "package.json"))
      file.length must beEqualTo (1683)
      file.contains(""""version": "2.2.5"""") must beTrue
    }
    "fetch a file" in {
      val file = await(git.file("mochajs/mocha", None, "package.json"))
      file.length must beGreaterThan (0)
    }
    "fetch a file with a different version" in {
      val file = await(git.file("mochajs/mocha", Some("2.2.4"), "package.json"))
      file.length must beEqualTo (1663)
      file.contains(""""version": "2.2.4"""") must beTrue
    }
    "fetch a file with a git url syntax" in {
      val file = await(git.file(new URI("https://github.com/yiminghe/async-validator.git"), None, "LICENSE.md"))
      file.length must beEqualTo (1083)
      file must contain ("The MIT License (MIT)")
    }
  }

  "git tar" should {
    "fetch a tar" in {
      val tar = await(git.tar("mochajs/mocha", Some("2.2.5"), Set("node_modules")))

      val bufferedInputStream = new BufferedInputStream(tar)
      val archiveStream = new ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream)

      bufferedInputStream.available() must beEqualTo (645120)

      val files = Stream.continually(archiveStream.getNextEntry).takeWhile(_ != null).map(_.getName)
      files.size must beEqualTo (178)
      files.exists(_.contains("node_modules")) must beFalse
      files.exists(_.contains(".git")) must beFalse
    }
  }

  "artifactId" should {
    "convert a name to a name" in {
      await(git.artifactId("foo")) must beEqualTo ("foo")
    }
    "convert a github url to a name" in {
      await(git.artifactId("mochajs/mocha")) must beEqualTo ("github-com-mochajs-mocha")
    }
    "convert a git:// url to a name" in {
      await(git.artifactId("git://github.com/mochajs/mocha.git")) must beEqualTo ("github-com-mochajs-mocha")
    }
    "convert a https:// url to a name" in {
      await(git.artifactId("https://github.com/mochajs/mocha.git")) must beEqualTo ("github-com-mochajs-mocha")
    }
    "convert a scoped name" in {
      await(git.artifactId("@reactivex/rxjs")) must beEqualTo ("reactivex__rxjs")
    }
    "not go case insensitive for github repos" in {
      await(git.artifactId("MochaJS/Mocha")) must beEqualTo ("github-com-MochaJS-Mocha")
    }
    "not go case insensitive for non-github repos" in {
      await(git.artifactId("Foo")) must beEqualTo ("Foo")
    }
  }

  "versionsOnBranch" should {
    "get the commits on a branch" in {
      await(git.versionsOnBranch("git://github.com/mochajs/mocha.git", "master")) must contain("8a100df959")
    }
  }

}
