#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Apache SkyWalking Java Agent Release Script
#
# Usage:
#   ./release.sh preflight                  Check tools and environment
#   ./release.sh prepare <version>          Prepare release (branch, maven release:prepare, tag)
#   ./release.sh stage                      Stage release (maven release:perform, build tars)
#   ./release.sh upload                     Upload to Apache SVN dist/dev
#   ./release.sh prepare-vote               Run prepare + stage + upload, then generate vote email
#   ./release.sh email [vote|announce]      Generate email content
#   ./release.sh promote                    Move from dist/dev to dist/release in SVN
#   ./release.sh github-release             Publish the GitHub Release (pushes Docker images via CI)
#   ./release.sh docker                     Push Docker images locally (fallback)
#   ./release.sh vote-passed [--release x.y.z] [--old_version x.y.z]
#   ./release.sh cleanup <old_version>      Remove old release from dist/release

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PRODUCT_NAME="apache-skywalking-java-agent"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Diagnostics go to stderr. resolve_version and the detect_* helpers are called
# inside $( ), which captures stdout - an error printed there would be swallowed
# into the variable instead of reaching the release manager.
info()  { echo -e "${GREEN}[INFO]${NC} $*" >&2; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*" >&2; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ============================================================
# validate_version — the only accepted shape for a version
# ============================================================
# Anchored, so nothing can ride along after the digits. A glob like
# [0-9]*.[0-9]*.[0-9]* accepts "9.7.0/", which is textually different from
# "9.7.0" and so slips past the equality guard in vote-passed, yet SVN
# canonicalises the trailing slash - cleanup would then delete the release that
# promote had just published. Every version entering an svn path goes through
# here.
validate_version() {
    local version="$1"
    local what="${2:-version}"
    if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        error "${what} must be exactly x.y.z, got: '${version}'"
    fi
}

# ============================================================
# detect_release_version — best guess at the release in flight
# ============================================================
# Ordered by when the tag was made, not by version number. Releases are not
# monotonic: a 9.6.1 patch cut from the 9.6.0 line after 9.7.0 has shipped is
# newer in time but lower in version, and sorting by version would pick the
# already-released 9.7.0. maven-release-plugin writes annotated tags, so
# creatordate is the tag's own timestamp and is stable across fetches.
#
# This is only ever a default offered to the release manager, never the final
# word - vote-passed asks them to confirm it.
detect_release_version() {
    local found
    found=$(git for-each-ref --sort=-creatordate --format='%(refname:short)' \
        'refs/tags/v[0-9]*.[0-9]*.[0-9]*' 2>/dev/null | head -1 | sed 's/^v//') || found=""
    printf '%s' "$found"
}

# ============================================================
# detect_old_version — the release currently published in dist/release
# ============================================================
# ASF policy keeps only the current release in dist/release; older ones are
# served from archive.apache.org. Whatever is there now is therefore what this
# release replaces. Excludes the version being released, so re-running after a
# partial failure - when promote has already copied it in - does not offer to
# delete the release itself. Best effort: no network, no default.
detect_old_version() {
    local exclude="${1:-}"
    local found
    # Under `set -euo pipefail` a failed svn ls, or a grep that matches nothing,
    # would abort the whole release rather than simply yield no suggestion. This
    # is only ever a hint, so swallow both and return empty.
    found=$(svn ls "https://dist.apache.org/repos/dist/release/skywalking/java-agent/" 2>/dev/null \
        | sed 's#/$##' \
        | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' \
        | grep -vx "$exclude" \
        | sort -V | tail -1) || found=""
    printf '%s' "$found"
}

# ============================================================
# resolve_version — identify the release from its tag
# ============================================================
# Every step after `prepare` acts on the release tag, never on whatever branch
# happens to be checked out. `release:perform` builds from the tag, the source
# tar is cut from the tag, and the vote email quotes the tag's commit IDs.
#
# The version therefore has to be derived from the tag as well. `git describe`
# cannot do this: release tags are created on release/x.y.z branches and never
# become ancestors of main, so once the release PR is merged and the branch is
# deleted, describe walks past them to an unrelated ancient tag (v3.2.6 here).
# That would aim SVN moves and Docker pushes at the wrong version. Tags are
# branch-independent and outlive the release branch, so select from the tag list.
#
# Order of precedence: explicit argument, then $RELEASE_VERSION, then the most
# recently created vX.Y.Z tag (see detect_release_version for why not the highest).
resolve_version() {
    local explicit="${1:-}"
    [ -z "$explicit" ] && explicit="${RELEASE_VERSION:-}"

    local version
    if [ -n "$explicit" ]; then
        version="${explicit#v}"
    else
        local latest
        latest=$(detect_release_version)
        if [ -z "$latest" ]; then
            error "No vX.Y.Z release tag found. Pass the version explicitly, e.g. '$0 <command> 9.7.0'."
        fi
        version="$latest"
    fi

    validate_version "$version" "Release version"

    # Refuse to act on a release that was never tagged.
    if ! git rev-parse -q --verify "refs/tags/v${version}" >/dev/null 2>&1; then
        error "Tag v${version} does not exist locally. Run '$0 prepare ${version}' first, or fetch it with 'git fetch origin --tags'."
    fi

    echo "$version"
}

# ============================================================
# preflight — check tools and environment
# ============================================================
cmd_preflight() {
    info "Running pre-flight checks..."

    local failed=0

    # Required tools
    for tool in git gpg svn shasum mvn java tar gh; do
        if command -v "$tool" &>/dev/null; then
            info "  $tool: $(command -v $tool)"
        else
            error "  $tool: NOT FOUND"
            failed=1
        fi
    done

    # Java version
    local java_version
    java_version=$(java -version 2>&1 | head -1)
    info "  Java: $java_version"

    # GPG key
    local gpg_keys
    gpg_keys=$(gpg --list-secret-keys --keyid-format SHORT 2>/dev/null | grep -c "sec" || true)
    if [ "$gpg_keys" -eq 0 ]; then
        error "  GPG: No secret keys found. Import your GPG key first."
        failed=1
    else
        info "  GPG: $gpg_keys secret key(s) found"
    fi

    # GPG signing without password prompt
    info "  Testing GPG signing (should not ask for password)..."
    local test_file
    test_file=$(mktemp)
    echo "test" > "$test_file"
    if gpg --batch --yes --armor --detach-sig "$test_file" 2>/dev/null; then
        info "  GPG signing: OK (no password prompt)"
        rm -f "$test_file" "${test_file}.asc"
    else
        rm -f "$test_file" "${test_file}.asc"
        echo ""
        echo "  GPG signing FAILED. The agent must be configured to cache the passphrase."
        echo ""
        echo "  Options to fix:"
        echo "    1. Configure gpg-agent with a longer cache TTL in ~/.gnupg/gpg-agent.conf:"
        echo "       default-cache-ttl 86400"
        echo "       max-cache-ttl 86400"
        echo "       Then: gpgconf --kill gpg-agent"
        echo ""
        echo "    2. Or run 'gpg --sign /dev/null' manually first to cache the passphrase."
        echo ""
        failed=1
    fi

    # Maven settings (Apache credentials)
    local settings_file="${HOME}/.m2/settings.xml"
    if [ -f "$settings_file" ]; then
        if grep -q "apache.releases.https" "$settings_file"; then
            info "  Maven settings: apache.releases.https server found"
        else
            warn "  Maven settings: apache.releases.https server NOT found in $settings_file"
            failed=1
        fi
    else
        warn "  Maven settings: $settings_file not found"
        failed=1
    fi

    # Git status
    cd "$PROJECT_ROOT"
    if [ -n "$(git status --porcelain)" ]; then
        warn "  Git: working tree is dirty"
    else
        info "  Git: working tree is clean"
    fi

    local branch
    branch=$(git rev-parse --abbrev-ref HEAD)
    info "  Git branch: $branch"

    if [ "$failed" -ne 0 ]; then
        echo ""
        error "Pre-flight checks failed. Fix the issues above before releasing."
    fi

    echo ""
    info "All pre-flight checks passed."
}

# ============================================================
# prepare — prepare the release (CHANGES.md, maven)
# ============================================================
cmd_prepare() {
    local version="${1:-}"
    local next_version="${2:-}"
    if [ -z "$version" ]; then
        error "Usage: $0 prepare <version> [next_version]  (e.g., 9.7.0 9.8.0)"
    fi

    cd "$PROJECT_ROOT"

    if [ -z "$next_version" ]; then
        next_version=$(echo "$version" | awk -F. '{printf "%s.%s.%s", $1, $2+1, 0}')
    fi
    local branch_name="release/${version}"

    info "Preparing release ${version}..."
    echo "  Release version: ${version}"
    echo "  Tag: v${version}"
    echo "  Next dev version: ${next_version}-SNAPSHOT"
    echo "  Branch: ${branch_name}"
    echo ""

    # At the end of this step CHANGES.md is reset for the next development
    # version, and its milestone link needs that version's GitHub milestone ID.
    # Ask for it here, up front, so the release does not stop for input after
    # the long release:prepare build. Set NEXT_MILESTONE=<id> to skip the prompt.
    local next_milestone="${NEXT_MILESTONE:-}"
    if [ -z "$next_milestone" ]; then
        echo "  CHANGES.md will be reset for ${next_version}, and its milestone link needs an ID."
        echo "  Find 'Java - ${next_version}' at https://github.com/apache/skywalking/milestones"
        read -rp "  Milestone ID for ${next_version} (number, or blank to fill in manually later): " next_milestone
    fi
    if [ -n "$next_milestone" ]; then
        case "$next_milestone" in
            *[!0-9]*) error "Milestone ID must be a number, got: ${next_milestone}" ;;
        esac
        # gh api writes the error body to stdout on failure, so gate on its
        # exit status rather than letting a 404 payload become the title.
        local milestone_title
        if ! milestone_title=$(gh api "repos/apache/skywalking/milestones/${next_milestone}" -q .title 2>/dev/null); then
            milestone_title=""
        fi
        if [ -z "$milestone_title" ]; then
            warn "  Could not verify milestone ${next_milestone} on apache/skywalking; using it as given."
        elif [ "$milestone_title" != "Java - ${next_version}" ]; then
            warn "  Milestone ${next_milestone} is '${milestone_title}', expected 'Java - ${next_version}'. Double-check it."
        else
            info "  Next milestone: ${next_milestone} (${milestone_title})"
        fi
    else
        next_milestone="xxx"
        warn "  No milestone ID given; CHANGES.md will keep 'milestone/xxx' - edit it before merging the release PR."
    fi

    echo ""
    read -rp "Continue? [y/N] " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        info "Aborted."
        exit 0
    fi

    # Step 1: Create release branch from main
    info "Creating branch ${branch_name}..."
    git checkout -b "${branch_name}"

    # Step 2: Maven release:prepare
    # This creates two commits:
    #   1. [maven-release-plugin] prepare release vx.y.z  (pom versions set to x.y.z)
    #   2. [maven-release-plugin] prepare for next development iteration (pom versions set to next-SNAPSHOT)
    # And a tag vx.y.z pointing to commit 1.
    # CHANGES.md is kept as-is so the tag includes the full changelog.
    info "Running maven release:prepare..."
    ./mvnw release:clean
    ./mvnw release:prepare -DautoVersionSubmodules=true -Pall \
        -DreleaseVersion="${version}" \
        -DdevelopmentVersion="${next_version}-SNAPSHOT" \
        -Dtag="v${version}" \
        -DpushChanges=false

    # Step 3: After the tag is created, move CHANGES.md for next dev cycle
    info "Moving changelog to changes/changes-${version}.md..."
    local changes_file="changes/changes-${version}.md"

    # Archive CHANGES.md as the release changelog: everything up to and including
    # the milestone link, dropping only the trailing "Find change logs of all
    # versions" footer. Uses POSIX sed addressing so it works on BSD and GNU alike.
    if ! grep -q "All issues and pull requests" CHANGES.md; then
        error "CHANGES.md has no 'All issues and pull requests' milestone line; cannot archive changelog."
    fi
    sed -n '1,/All issues and pull requests/p' CHANGES.md > "$changes_file"
    info "Created $changes_file ($(wc -l < "$changes_file" | tr -d ' ') lines)"

    # Reset CHANGES.md for next development version
    cat > CHANGES.md << EOF
Changes by Version
==================
Release Notes.

${next_version}
------------------


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/${next_milestone}?closed=1)

------------------
Find change logs of all versions [here](changes).
EOF

    git add CHANGES.md "$changes_file"
    git commit -m "Move ${version} changelog and reset for ${next_version} development"

    # Step 4: Push branch and tag
    info "Pushing branch and tag..."
    git push -u origin "${branch_name}"
    git push origin "v${version}"

    # Step 5: Create PR
    info "Creating pull request..."
    if command -v gh &>/dev/null; then
        gh pr create --base main --head "${branch_name}" \
            --title "Release ${version}" \
            --body "Release Apache SkyWalking Java Agent ${version}.

- Maven release:prepare completed (tag \`v${version}\` created)
- CHANGES.md archived to \`changes/changes-${version}.md\`
- Next development version: ${next_version}-SNAPSHOT"
        info "PR created."
    else
        warn "GitHub CLI (gh) not found. Create PR manually for branch ${branch_name}."
    fi

    info "Release ${version} prepared."
    info "  Tag v${version} is ready."
    info "  PR created for branch ${branch_name} → main."
    info "Next step: $0 stage"
}

# ============================================================
# stage — stage the release (maven, build source & binary tars)
# ============================================================
cmd_stage() {
    cd "$PROJECT_ROOT"

    local version
    version=$(resolve_version "${1:-}")
    local tag_name="v${version}"

    info "Staging release ${version} (from tag ${tag_name})..."

    # Maven release:perform
    info "Running maven release:perform..."
    ./mvnw release:perform -DskipTests -Pall

    # Build source and binary packages (inlined from create_release.sh)
    info "Building source and binary packages..."

    cd "${SCRIPT_DIR}"
    local product_dir="${PRODUCT_NAME}-${version}"

    rm -rf "${product_dir}"
    mkdir "${product_dir}"

    git clone https://github.com/apache/skywalking-java.git "./${product_dir}"
    cd "${product_dir}"

    TAG_EXIST=$(git tag -l "${tag_name}" | wc -l)
    if [ "${TAG_EXIST}" -ne 1 ]; then
        error "Could not find the tag named ${tag_name}"
    fi

    git checkout "${tag_name}"
    git submodule init
    git submodule update

    # Generate static version properties (no Git info in source tar)
    ./mvnw -q -pl apm-sniffer/apm-agent-core initialize \
           -DgenerateGitPropertiesFilename="$(pwd)/apm-sniffer/apm-agent-core/src/main/resources/skywalking-agent-version.properties"

    cd "${SCRIPT_DIR}"

    # Source tar
    info "Creating source tar..."
    tar czf "${product_dir}-src.tgz" \
        --exclude .git \
        --exclude .DS_Store \
        --exclude .github \
        --exclude .gitignore \
        --exclude .gitmodules \
        "${product_dir}"

    gpg --armor --detach-sig "${product_dir}-src.tgz"
    shasum -a 512 "${product_dir}-src.tgz" > "${product_dir}-src.tgz.sha512"

    # Binary tar
    info "Creating binary tar..."
    cd "${product_dir}"
    export TAG="${version}"
    make dist

    echo ""
    info "Release ${version} staged."
    info "Source tar: ${SCRIPT_DIR}/${product_dir}-src.tgz"
    info "Binary tar: ${SCRIPT_DIR}/${product_dir}/${PRODUCT_NAME}-${version}.tgz"
    info "Next step: $0 upload"
}

# ============================================================
# upload — upload to Apache SVN dist/dev
# ============================================================
cmd_upload() {
    cd "$PROJECT_ROOT"

    local version
    version=$(resolve_version "${1:-}")
    local svn_dev="https://dist.apache.org/repos/dist/dev/skywalking/java-agent"

    info "Uploading release ${version} to Apache SVN (dist/dev)..."

    local staging_dir="${SCRIPT_DIR}/${PRODUCT_NAME}-${version}"

    # Verify files exist
    local src_tar="${SCRIPT_DIR}/${PRODUCT_NAME}-${version}-src.tgz"
    local bin_tar="${staging_dir}/${PRODUCT_NAME}-${version}.tgz"

    for f in "$src_tar" "${src_tar}.asc" "${src_tar}.sha512" \
             "$bin_tar" "${bin_tar}.asc" "${bin_tar}.sha512"; do
        if [ ! -f "$f" ]; then
            error "Missing file: $f. Run '$0 stage' first."
        fi
    done

    # Create SVN directory and upload
    read -rp "SVN username (Apache ID): " svn_user

    local tmp_svn
    tmp_svn=$(mktemp -d)
    info "Checking out SVN dist/dev..."
    svn checkout --depth empty "$svn_dev" "$tmp_svn" --username "$svn_user"

    mkdir -p "${tmp_svn}/${version}"

    cp "$src_tar" "${src_tar}.asc" "${src_tar}.sha512" "${tmp_svn}/${version}/"
    cp "$bin_tar" "${bin_tar}.asc" "${bin_tar}.sha512" "${tmp_svn}/${version}/"

    cd "$tmp_svn"
    svn add "${version}"
    svn commit -m "Stage Apache SkyWalking Java Agent ${version}" --username "$svn_user"

    rm -rf "$tmp_svn"
    info "Uploaded to ${svn_dev}/${version}"
    info "Next step: $0 email vote"
}

# ============================================================
# email — generate email templates
# ============================================================
cmd_email() {
    local type="${1:-}"
    if [[ ! "$type" =~ ^(vote|announce)$ ]]; then
        error "Usage: $0 email <vote|announce> [version]"
    fi

    cd "$PROJECT_ROOT"

    local version
    version=$(resolve_version "${2:-}")
    local tag="v${version}"
    local commit_id
    commit_id=$(git rev-list -n1 "$tag" 2>/dev/null || echo "<GIT_COMMIT_ID>")
    local submodule_commit
    submodule_commit=$(git ls-tree "$tag" apm-protocol/apm-network/src/main/proto 2>/dev/null | awk '{print $3}' || echo "<SUBMODULE_COMMIT_ID>")

    # Get sha512 checksums
    local src_sha512=""
    local bin_sha512=""
    local src_sha_file="${SCRIPT_DIR}/${PRODUCT_NAME}-${version}-src.tgz.sha512"
    local bin_sha_file="${SCRIPT_DIR}/${PRODUCT_NAME}-${version}/${PRODUCT_NAME}-${version}.tgz.sha512"
    [ -f "$src_sha_file" ] && src_sha512=$(cat "$src_sha_file")
    [ -f "$bin_sha_file" ] && bin_sha512=$(cat "$bin_sha_file")

    echo ""
    echo "============================================================"

    case "$type" in
    vote)
        cat << EOF
Mail to: dev@skywalking.apache.org
Subject: [VOTE] Release Apache SkyWalking Java Agent version ${version}

Hi All,
This is a call for vote to release Apache SkyWalking Java Agent version ${version}.

Release notes:

 * https://github.com/apache/skywalking-java/blob/main/changes/changes-${version}.md

Release Candidate:

 * https://dist.apache.org/repos/dist/dev/skywalking/java-agent/${version}
 * sha512 checksums
   - ${src_sha512}
   - ${bin_sha512}

Maven 2 staging repository:

 * https://repository.apache.org/content/repositories/<STAGING_REPO_ID>/org/apache/skywalking/

Release Tag :

 * (Git Tag) v${version}

Release CommitID :

 * https://github.com/apache/skywalking-java/tree/${commit_id}
 * Git submodule
   * apm-protocol/apm-network/src/main/proto: https://github.com/apache/skywalking-data-collect-protocol/tree/${submodule_commit}

Keys to verify the Release Candidate :

 * https://dist.apache.org/repos/dist/release/skywalking/KEYS

Guide to build the release from source :

 > ./mvnw clean package

Voting will start now ($(date '+%B %d, %Y')) and will remain open for at least 72 hours, Request all PMC members to give their vote.
[ ] +1 Release this package.
[ ] +0 No opinion.
[ ] -1 Do not release this package because....
EOF
        ;;
    announce)
        cat << EOF
Mail to: dev@skywalking.apache.org, announce@apache.org
Subject: [ANNOUNCE] Apache SkyWalking Java Agent ${version} released

Hi all,

Apache SkyWalking Team is glad to announce the release of Apache SkyWalking Java Agent ${version}.

SkyWalking: APM (application performance monitor) tool for distributed systems,
especially designed for microservices, cloud native and container-based (Docker, Kubernetes, Mesos) architectures.

The Java Agent for Apache SkyWalking, which provides the native tracing/metrics/logging abilities for Java projects.

This release contains a number of new features, bug fixes and improvements compared to
the previous version. The notable changes include:

(Highlight key changes from changes-${version}.md)
1. ...
2. ...
3. ...

Please refer to the change log for the complete list of changes:
https://github.com/apache/skywalking-java/blob/main/changes/changes-${version}.md

Apache SkyWalking website:
http://skywalking.apache.org/

Downloads:
http://skywalking.apache.org/downloads/

Twitter:
https://twitter.com/AsfSkyWalking

SkyWalking Resources:
- GitHub: https://github.com/apache/skywalking-java
- Issue: https://github.com/apache/skywalking/issues
- Mailing list: dev@skywalking.apache.org


- Apache SkyWalking Team
EOF
        ;;
    esac

    echo "============================================================"
    echo ""
    warn "Replace <STAGING_REPO_ID> with the actual Nexus staging repository ID."
}

# ============================================================
# docker — build and push Docker images
# ============================================================
cmd_docker() {
    cd "$PROJECT_ROOT"

    local version
    version=$(resolve_version "${1:-}")

    info "Building and pushing Docker images for ${version} (from tag v${version})..."

    local dist_tar="${SCRIPT_DIR}/${PRODUCT_NAME}-${version}/${PRODUCT_NAME}-${version}.tgz"

    if [ ! -f "$dist_tar" ]; then
        error "Binary tar not found: $dist_tar. Run '$0 stage' first."
    fi

    # Extract agent package
    tar -xzf "$dist_tar" -C "$PROJECT_ROOT"

    export NAME=skywalking-java-agent
    export HUB=apache
    export TAG="$version"

    make docker.push.alpine docker.push.java8 docker.push.java11 docker.push.java17 docker.push.java21 docker.push.java25

    info "Docker images pushed for ${version}."
}

# ============================================================
# github-release — publish the GitHub Release
# ============================================================
# This is what ships the official Docker images. Publishing a non-prerelease
# fires the `release: released` trigger in .github/workflows/publish-docker.yaml,
# which builds every base variant and pushes them to Docker Hub. Running
# `$0 docker` by hand is only a fallback for when that workflow fails.
cmd_github_release() {
    cd "$PROJECT_ROOT"

    local version
    version=$(resolve_version "${1:-}")
    local tag="v${version}"
    local notes_file="changes/changes-${version}.md"

    info "Publishing GitHub Release ${tag}..."

    if ! git ls-remote --tags origin "refs/tags/${tag}" | grep -q .; then
        error "Tag ${tag} is not on origin. Push it before publishing the release."
    fi

    if gh release view "${tag}" >/dev/null 2>&1; then
        warn "GitHub Release ${tag} already exists; leaving it alone."
        warn "If the images were not pushed, re-run the workflow or use '$0 docker ${version}'."
        return 0
    fi

    local -a notes_args
    if [ -f "$notes_file" ]; then
        notes_args=(--notes-file "$notes_file")
    else
        warn "  ${notes_file} not found; using auto-generated notes."
        notes_args=(--generate-notes)
    fi

    gh release create "${tag}" --title "${version}" "${notes_args[@]}"

    info "GitHub Release ${tag} published."
    info "  publish-docker.yaml is now pushing to Docker Hub:"
    info "    apache/skywalking-java-agent:${version}-{alpine,java8,java11,java17,java21,java25}"
    info "  Watch: https://github.com/apache/skywalking-java/actions/workflows/publish-docker.yaml"
}

# ============================================================
# promote — move from dist/dev to dist/release
# ============================================================
cmd_promote() {
    cd "$PROJECT_ROOT"

    local version
    version=$(resolve_version "${1:-}")

    info "Promoting release ${version} from dist/dev to dist/release..."

    read -rp "SVN username (Apache ID): " svn_user

    svn mv "https://dist.apache.org/repos/dist/dev/skywalking/java-agent/${version}" \
           "https://dist.apache.org/repos/dist/release/skywalking/java-agent/${version}" \
           -m "Release Apache SkyWalking Java Agent ${version}" \
           --username "$svn_user"

    info "Release ${version} promoted."
    info "Next steps:"
    info "  1. Release the Nexus staging repository at https://repository.apache.org"
    info "  2. Update website download page"
    info "  3. Run: $0 github-release ${version}   (pushes the Docker images via GitHub Actions)"
    info "  4. Run: $0 email announce ${version}"
}

# ============================================================
# cleanup — remove old release from dist/release
# ============================================================
cmd_cleanup() {
    local old_version="${1:-}"
    if [ -z "$old_version" ]; then
        error "Usage: $0 cleanup <old_version>  (e.g., 9.5.0)"
    fi
    validate_version "$old_version" "Old version"

    info "Removing old release ${old_version} from dist/release..."

    read -rp "SVN username (Apache ID): " svn_user

    svn rm "https://dist.apache.org/repos/dist/release/skywalking/java-agent/${old_version}" \
           -m "Remove old Apache SkyWalking Java Agent ${old_version} release" \
           --username "$svn_user"

    info "Removed ${old_version} from dist/release."
    warn "Remember to update download page links to point to archive.apache.org."
}

# ============================================================
# prepare-vote — run all steps before the vote
# ============================================================
cmd_prepare_vote() {
    local version="${1:-}"
    local next_version="${2:-}"
    if [ -z "$version" ]; then
        error "Usage: $0 prepare-vote <version> [next_version]  (e.g., 9.7.0 9.8.0)"
    fi

    cmd_preflight
    echo ""
    cmd_prepare "$version" "$next_version"
    echo ""
    cmd_stage "$version"
    echo ""
    cmd_upload "$version"
    echo ""
    cmd_email vote "$version"
}

# ============================================================
# vote-passed — run all steps after the vote passes
# ============================================================
cmd_vote_passed() {
    cd "$PROJECT_ROOT"

    # Two versions, and they do opposite things: one is published, the other is
    # deleted. Never take them positionally - `vote-passed 9.7.0` while releasing
    # 9.7.0 reads as "release this" but would svn rm what promote just copied in.
    # Name them, or be asked for them. Detection only supplies defaults.
    local version="${RELEASE_VERSION:-}"
    local old_version="${OLD_VERSION:-}"
    local old_version_given=0
    [ -n "$old_version" ] && old_version_given=1

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --release|--release-version)
                [ -n "${2:-}" ] || error "$1 needs a version, e.g. --release 9.7.0"
                version="$2"; shift 2 ;;
            --old_version|--old-version)
                [ -n "${2:-}" ] || error "$1 needs a version, e.g. --old_version 9.6.0 (or --no-cleanup)"
                old_version="$2"; old_version_given=1; shift 2 ;;
            --no-cleanup)
                old_version=""; old_version_given=1; shift ;;
            -h|--help)
                echo "Usage: $0 vote-passed [--release x.y.z] [--old_version x.y.z | --no-cleanup]"
                echo ""
                echo "  --release       the version being published; must already be tagged"
                echo "  --old_version   the version removed from dist/release"
                echo "  --no-cleanup    leave dist/release alone"
                echo ""
                echo "Anything not given is asked for. RELEASE_VERSION and OLD_VERSION are"
                echo "honoured as well, and the flags win over them."
                return 0 ;;
            -*)
                error "Unknown option: $1
  Usage: $0 vote-passed [--release x.y.z] [--old_version x.y.z | --no-cleanup]" ;;
            *)
                error "'$0 vote-passed' does not take positional arguments - it is too easy to
  confuse the version being released with the one being deleted. Name them:
    $0 vote-passed --release <new> --old_version <old>
  or run it with no arguments and answer the prompts." ;;
        esac
    done

    info "Publishing a release. Two versions are needed."
    echo ""

    if [ -z "$version" ]; then
        local suggested
        suggested=$(detect_release_version)
        echo "  The version being released. It must already be tagged and voted on."
        if [ -n "$suggested" ]; then
            echo "  Most recently tagged: ${suggested} (tag v${suggested})"
        fi
        read -rp "  Release version${suggested:+ [$suggested]}: " version
        version="${version:-$suggested}"
    fi
    [ -z "$version" ] && error "No release version given."
    version=$(resolve_version "$version")
    echo ""

    if [ "$old_version_given" -eq 0 ]; then
        local current
        current=$(detect_old_version "$version")
        echo "  The version to remove from dist/release. ASF policy keeps only the"
        echo "  current release there; older ones are served from archive.apache.org."
        if [ -n "$current" ]; then
            echo "  Currently published in dist/release: ${current}"
        else
            echo "  Could not read dist/release, so there is no suggestion."
        fi
        read -rp "  Old version to remove${current:+ [$current]} (or 'none' to skip): " old_version
        old_version="${old_version:-$current}"
    fi
    # 'none' is the explicit opt out; blank accepts the suggestion above.
    [ "$old_version" = "none" ] && old_version=""
    [ -n "$old_version" ] && validate_version "$old_version" "Old version ('none' or --no-cleanup skips cleanup);"

    # The mistake this whole prompt exists to prevent.
    if [ -n "$old_version" ] && [ "$old_version" = "$version" ]; then
        error "Old version and release version are both ${version}; that would delete the release being published."
    fi
    echo ""

    info "Publishing release ${version}:"
    echo "  Release tag     : v${version}"
    echo "  SVN promote     : dist/dev/skywalking/java-agent/${version} -> dist/release/..."
    echo "  GitHub Release  : v${version} (this is what triggers the Docker Hub push)"
    echo "  Docker Hub tags : apache/skywalking-java-agent:${version}-{alpine,java8,java11,java17,java21,java25}"
    echo "                    pushed by .github/workflows/publish-docker.yaml, not from here"
    if [ -n "$old_version" ]; then
        echo "  Remove from SVN : dist/release/skywalking/java-agent/${old_version}"
    else
        echo "  Remove from SVN : (nothing - no old version given)"
    fi
    echo ""
    read -rp "Continue? [y/N] " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        info "Aborted."
        exit 0
    fi
    echo ""

    cmd_promote "$version"
    echo ""
    cmd_github_release "$version"
    echo ""
    cmd_email announce "$version"

    if [ -n "$old_version" ]; then
        echo ""
        cmd_cleanup "$old_version"
    else
        echo ""
        warn "To clean up an old release, run: $0 cleanup <old_version>"
    fi
}

# ============================================================
# Main dispatcher
# ============================================================
main() {
    local cmd="${1:-}"
    shift || true

    case "$cmd" in
        preflight)    cmd_preflight "$@" ;;
        prepare)      cmd_prepare "$@" ;;
        stage)        cmd_stage "$@" ;;
        upload)       cmd_upload "$@" ;;
        email)          cmd_email "$@" ;;
        github-release) cmd_github_release "$@" ;;
        docker)         cmd_docker "$@" ;;
        promote)      cmd_promote "$@" ;;
        cleanup)      cmd_cleanup "$@" ;;
        prepare-vote) cmd_prepare_vote "$@" ;;
        vote-passed)  cmd_vote_passed "$@" ;;
        *)
            echo "Apache SkyWalking Java Agent Release Tool"
            echo ""
            echo "Usage: $0 <command> [args]"
            echo ""
            echo "Quick start (two-step release):"
            echo "  $0 prepare-vote 9.7.0 [9.8.0]     # before vote (next version auto-calculated if omitted)"
            echo "  (wait for 72h vote to pass)"
            echo "  $0 vote-passed                     # after vote (asks for the versions)"
            echo "  $0 vote-passed --release 9.7.0 --old_version 9.6.0"
            echo ""
            echo "Every command after 'prepare' identifies the release by its tag (vX.Y.Z), not by the"
            echo "checked-out branch, so they still work once release/x.y.z has been merged and deleted."
            echo "The version defaults to the most recently created vX.Y.Z tag - not the highest, since a"
            echo "patch release can be newer in time but lower in version. Override with an argument or"
            echo "RELEASE_VERSION."
            echo ""
            echo "Individual commands:"
            echo "  preflight                     Check tools and environment"
            echo "  prepare <ver> [next_ver]      Prepare release (branch, tag, PR)"
            echo "  stage [ver]                   Stage release (maven release:perform, build tars)"
            echo "  upload [ver]                  Upload to Apache SVN dist/dev"
            echo "  prepare-vote <ver> [next_ver] Run preflight + prepare + stage + upload + vote email"
            echo "  email <vote|announce> [ver]   Generate email content"
            echo "  promote [ver]                 Move from dist/dev to dist/release in SVN"
            echo "  github-release [ver]          Publish the GitHub Release; this is what pushes"
            echo "                                the Docker images, via publish-docker.yaml"
            echo "  docker [ver]                  Push Docker images from this machine (fallback"
            echo "                                for when the workflow fails)"
            echo "  vote-passed [--release x.y.z] [--old_version x.y.z | --no-cleanup]"
            echo "                                Run promote + github-release + announce [+ cleanup]."
            echo "                                Anything not named is asked for. Never positional:"
            echo "                                the two versions do opposite things."
            echo "  cleanup <old_version>         Remove old release from dist/release"
            ;;
    esac
}

main "$@"
