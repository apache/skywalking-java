Apache SkyWalking Java Agent Release Guide
--------------------
If you're a committer, you can learn how to release SkyWalking in The Apache Way and start the voting process by reading this document.


## Set up your development environment
Follow the steps in the [Apache maven deployment environment document](http://www.apache.org/dev/publishing-maven-artifacts.html#dev-env)
to set gpg tool and encrypt passwords.

Use the following block as a template and place it in `~/.m2/settings.xml`.

```
<settings>
...
  <servers>
    <!-- To publish a snapshot of some part of Maven -->
    <server>
      <id>apache.snapshots.https</id>
      <username> <!-- YOUR APACHE LDAP USERNAME --> </username>
      <password> <!-- YOUR APACHE LDAP PASSWORD (encrypted) --> </password>
    </server>
    <!-- To stage a release of some part of Maven -->
    <server>
      <id>apache.releases.https</id>
      <username> <!-- YOUR APACHE LDAP USERNAME --> </username>
      <password> <!-- YOUR APACHE LDAP PASSWORD (encrypted) --> </password>
    </server>
   ...
  </servers>
</settings>
```

## Add your GPG public key
1. Add your GPG public key into the [SkyWalking GPG KEYS](https://dist.apache.org/repos/dist/release/skywalking/KEYS) file.
If you are a committer, use your Apache ID and password to log in this svn, and update the file. **Don't override the existing file.**
1. Upload your GPG public key to the public GPG site, such as [MIT's site](http://pgp.mit.edu:11371/). This site should be in the
Apache maven staging repository checklist.

## Test your settings
This step is only for testing purpose. If your env is correctly set, you don't need to check every time.
```
./mvnw clean install(this will build artifacts, sources and sign)
```

## Prepare for the release
```
./mvnw release:clean
./mvnw release:prepare -DautoVersionSubmodules=true -Pall
```

- Set version number as x.y.z, and tag as **v**x.y.z (The version tag must start with **v**. You will find out why this is necessary in the next step.)

_You could do a GPG signature before preparing for the release. If you need to input the password to sign, and the maven doesn't provide you with the opportunity to do so, this may lead to failure of the release. To resolve this, you may run `gpg --sign xxx` in any file. This will allow it to remember the password for long enough to prepare for the release._

## Stage the release
```
./mvnw release:perform -DskipTests -Pall
```

- The release will be automatically inserted into a temporary staging repository.

## Build and sign the source code and binary package
```shell
export RELEASE_VERSION=x.y.z (example: RELEASE_VERSION=5.0.0-alpha)
cd tools/releasing
bash create_release.sh
```

This script takes care of the following things:
1. Use `v` + `RELEASE_VERSION` as tag to clone the codes.
2. Complete `git submodule init/update`.
3. Exclude all unnecessary files in the target source tar, such as `.git`, `.github`, and `.gitmodules`. See the script for more details.
4. Execute `gpg` and `shasum 512` for source code tar.
5. Use maven package to build the agent tar.
6. Execute `gpg` and `shasum 512` for binary tar.

`apache-skywalking-java-agent-x.y.z-src.tgz` and files ending with `.asc` and `.sha512` may be found in the `tools/releasing` folder.
`apache-skywalking-java-agent-x.y.z.tgz` and files ending with `.asc` and `.sha512` may be found in the `tools/releasing/apache-skywalking-java-agent-x.y.z` folder.


## Upload to Apache svn
1. Use your Apache ID to log in to `https://dist.apache.org/repos/dist/dev/skywalking/java-agent/`.
1. Create a folder and name it by the release version and round, such as: `x.y.z`
1. Upload the source code package to the folder with files ending with `.asc` and `.sha512`.
1. Upload the distribution package to the folder with files ending with `.asc` and `.sha512`.

## Make the internal announcements
Send an announcement mail in dev mail list.

```
Mail title: [ANNOUNCE] SkyWalking Java Agent x.y.z test build available

Mail content:
The test build of Java Agent x.y.z is available.

We welcome any comments you may have, and will take all feedback into
account if a quality vote is called for this build.

Release notes:

 * https://github.com/apache/skywalking-java/blob/master/changes/changes-x.y.z.md

Release Candidate:

 * https://dist.apache.org/repos/dist/dev/skywalking/java-agent/xxxx
 * sha512 checksums

Maven 2 staging repository:

 * https://repository.apache.org/content/repositories/xxxx/org/apache/skywalking-java/

Release Tag :

 * (Git Tag) x.y.z

Release CommitID :

 * https://github.com/apache/skywalking-java/tree/(Git Commit ID)
 * Git submodule
   * apm-protocol/apm-network/src/main/proto: https://github.com/apache/skywalking-data-collect-protocol/tree/(Git Commit ID)

Keys to verify the Release Candidate :

 * https://dist.apache.org/repos/dist/release/skywalking/KEYS

Guide to build the release from source :

 > ./mvnw clean package

A vote regarding the quality of this test build will be initiated
within the next couple of days.
```

## Wait for at least 48 hours for test responses
Any PMC member, committer or contributor can test the release features and provide feedback.
Based on that, the PMC will decide whether to start the voting process.

## Call a vote in dev
Call a vote in `dev@skywalking.apache.org`

```
Mail title: [VOTE] Release Apache SkyWalking Java Agent version x.y.z

Mail content:
Hi All,
This is a call for vote to release Apache SkyWalking Java Agent version x.y.z.

Release notes:

 * https://github.com/apache/skywalking-java/blob/master/changes/changes-x.y.z.md

Release Candidate:

 * https://dist.apache.org/repos/dist/dev/skywalking/java-agent/xxxx
 * sha512 checksums

Maven 2 staging repository:

 * https://repository.apache.org/content/repositories/xxxx/org/apache/skywalking/

Release Tag :

 * (Git Tag) x.y.z

Release CommitID :

 * https://github.com/apache/skywalking-java/tree/(Git Commit ID)
 * Git submodule
   * apm-protocol/apm-network/src/main/proto: https://github.com/apache/skywalking-data-collect-protocol/tree/(Git Commit ID)

Keys to verify the Release Candidate :

 * https://dist.apache.org/repos/dist/release/skywalking/KEYS

Guide to build the release from source :

 > ./mvnw clean package

Voting will start now (xxxx date) and will remain open for at least 72 hours, Request all PMC members to give their vote.
[ ] +1 Release this package.
[ ] +0 No opinion.
[ ] -1 Do not release this package because....
```

## Vote Check
All PMC members and committers should check these before casting +1 votes.

1. Features test.
1. All artifacts in staging repository are published with `.asc`, `.md5`, and `*sha1` files.
1. Source code and distribution package (`apache-skywalking-java-agent-x.y.z-src.tar.gz`, `apache-skywalking-java-agent-x.y.z.tar.gz`)
are found in `https://dist.apache.org/repos/dist/dev/skywalking/java-agent/x.y.z` with `.asc` and `.sha512`.
1. `LICENSE` and `NOTICE` are in the source code and distribution package.
1. Check `shasum -c apache-skywalking-java-agent-x.y.z-src.tgz.sha512`.
1. Check `gpg --verify apache-skywalking-java-agent-x.y.z-src.tgz.asc apache-skywalking-apm-x.y.z-src.tgz`
1. Build a distribution package from the source code package (`apache-skywalking-java-agent-x.y.z-src.tar.gz`).
1. Check the Apache License Header. Run `docker run --rm -v $(pwd):/github/workspace apache/skywalking-eyes header check`. (No binaries in source codes)


The voting process is as follows:
1. All PMC member votes are +1 binding, and all other votes are +1 but non-binding.
1. If you obtain at least 3 (+1 binding) votes with more +1 than -1 votes within 72 hours, the release will be approved.


## Publish the release
1. Move source codes tar and distribution packages to `https://dist.apache.org/repos/dist/release/skywalking/java-agent/`.
```
> export SVN_EDITOR=vim
> svn mv https://dist.apache.org/repos/dist/dev/skywalking/java-agent/x.y.z https://dist.apache.org/repos/dist/release/skywalking/java-agent
....
enter your apache password
....

```
2. Release in the nexus staging repo.
3. Public download source and distribution tar/zip are located in `http://www.apache.org/dyn/closer.cgi/skywalking/java-agent/x.y.z/xxx`.
The Apache mirror path is the only release information that we publish.
4. Public asc and sha512 are located in `https://www.apache.org/dist/skywalking/java-agent/x.y.z/xxx`.
5. Public KEYS point to  `https://www.apache.org/dist/skywalking/KEYS`.
6. Update the website download page. http://skywalking.apache.org/downloads/ . Add a new download source, distribution, sha512, asc, and document
links. The links can be found following rules (3) to (6) above.
7. Add a release event on the website homepage and event page. Announce the public release with changelog or key features.
8. Send ANNOUNCE email to `dev@skywalking.apache.org`, `announce@apache.org`. The sender should use the Apache email account.
```
Mail title: [ANNOUNCE] Apache SkyWalking Java Agent x.y.z released

Mail content:
Hi all,

Apache SkyWalking Team is glad to announce the first release of Apache SkyWalking Java Agent x.y.z.

SkyWalking: APM (application performance monitor) tool for distributed systems,
especially designed for microservices, cloud native and container-based (Docker, Kubernetes, Mesos) architectures.

The Java Agent for Apache SkyWalking, which provides the native tracing/metrics/logging abilities for Java projects.

This release contains a number of new features, bug fixes and improvements compared to
version a.b.c(last release). The notable changes since x.y.z include:

(Highlight key changes)
1. ...
2. ...
3. ...

Please refer to the change log for the complete list of changes:
https://github.com/apache/skywalking-java/blob/master/changes/changes-x.y.z.md

Apache SkyWalking website:
http://skywalking.apache.org/

Downloads:
http://skywalking.apache.org/downloads/

Twitter:
https://twitter.com/AsfSkyWalking

SkyWalking Resources:
- GitHub: https://github.com/apache/skywalking-java
- Issue: https://github.com/apache/skywalking/issues
- Mailing list: dev@skywalkiing.apache.org


- Apache SkyWalking Team
```

## Release Docker images

```shell
export SW_VERSION=x.y.z
git clone --depth 1 --branch v$SW_VERSION https://github.com/apache/skywalking-java.git
cd skywalking-java

curl -O https://dist.apache.org/repos/dist/release/skywalking/java-agent/$SW_VERSION/apache-skywalking-java-agent-$SW_VERSION.tgz
tar -xzvf apache-skywalking-java-agent-$SW_VERSION.tgz

export NAME=skywalking-java-agent
export HUB=apache
export TAG=$SW_VERSION

make docker.push.alpine docker.push.java8 docker.push.java11 docker.push.java17
```

## Clean up the old releases
Once the latest release has been published, you should clean up the old releases from the mirror system.
1. Update the download links (source, dist, asc, and sha512) on the website to the archive repo (https://archive.apache.org/dist/skywalking).
2. Remove previous releases from https://dist.apache.org/repos/dist/release/skywalking/java-agent.
