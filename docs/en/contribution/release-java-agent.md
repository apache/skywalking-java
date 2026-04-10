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

## Release using the release script

The release script `tools/releasing/release.sh` automates the full release workflow. The release is a **two-step process** with a vote in between.

### Quick start
```shell
# Step 1: Build, stage, upload, and generate vote email
./tools/releasing/release.sh prepare-vote x.y.z

# (send vote email to dev@skywalking.apache.org, wait 72h for vote to pass)

# Step 2: Promote, push Docker images, generate announce email, and clean up
./tools/releasing/release.sh vote-passed [old_version_to_remove]
```

Run `./tools/releasing/release.sh` without arguments to see all available commands, including individual steps if you need to run them separately.

### Pre-flight checks
Before starting, the script verifies:
- Required tools are installed (git, gpg, svn, shasum, mvn, java, tar, gh)
- GPG signing works **without password prompt** (critical for maven release)
- Maven settings contain Apache server credentials
- Git working tree is clean

If GPG signing fails, configure gpg-agent to cache the passphrase:
```
# ~/.gnupg/gpg-agent.conf
default-cache-ttl 86400
max-cache-ttl 86400
```
Then run `gpgconf --kill gpg-agent` and `gpg --sign /dev/null` to cache it.

### prepare-vote
`prepare-vote` runs the following steps in sequence:
1. **preflight** — verify tools and environment
2. **prepare** — create `release/x.y.z` branch, run `mvn release:prepare` (creates tag `vx.y.z` with full CHANGES.md), then archive changelog and reset for next version, push branch and tag, create PR
3. **stage** — run `mvn release:perform`, build source and binary tars with GPG signatures and sha512 checksums
4. **upload** — upload to Apache SVN `dist/dev` (prompts for SVN credentials)
5. **email vote** — print vote email template with pre-filled version, commit ID, submodule commit, and checksums

Copy the generated email and send it to `dev@skywalking.apache.org`. Voting remains open for at least 72 hours. At least 3 (+1 binding) PMC votes with more +1 than -1 are required.

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

## vote-passed
After the vote passes, run `vote-passed` which executes:
1. **promote** — move packages from `dist/dev` to `dist/release` in Apache SVN (prompts for SVN credentials), then release the Nexus staging repository at https://repository.apache.org and update the website download page
2. **docker** — build and push all Docker image variants (alpine, java8, java11, java17, java21, java25)
3. **email announce** — print announcement email template. Copy and send to `dev@skywalking.apache.org` and `announce@apache.org`
4. **cleanup** (optional) — if old version is provided, remove it from `dist/release`. Update download page links to point to `https://archive.apache.org/dist/skywalking`
