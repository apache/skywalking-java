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
#   ./release.sh docker                     Build and push Docker images
#   ./release.sh vote-passed                Run promote + docker, then generate announce email
#   ./release.sh cleanup <old_version>      Remove old release from dist/release

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PRODUCT_NAME="apache-skywalking-java-agent"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

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

    # Extract current version section from CHANGES.md
    sed -n "/^${version}$/,/^------------------$/p" CHANGES.md | head -n -1 > "$changes_file"
    grep "All issues and pull requests" CHANGES.md >> "$changes_file" || true
    info "Created $changes_file"

    # Reset CHANGES.md for next development version
    cat > CHANGES.md << EOF
Changes by Version
==================
Release Notes.

${next_version}
------------------


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/xxx?closed=1)

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

    # Detect version from latest tag
    local version
    version=$(git describe --tags --abbrev=0 | sed 's/^v//')
    local tag_name="v${version}"

    info "Staging release ${version}..."

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
    version=$(git describe --tags --abbrev=0 | sed 's/^v//')
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
        error "Usage: $0 email [vote|announce]"
    fi

    cd "$PROJECT_ROOT"

    local version
    version=$(git describe --tags --abbrev=0 | sed 's/^v//')
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

 * https://github.com/apache/skywalking-java/blob/master/changes/changes-${version}.md

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
https://github.com/apache/skywalking-java/blob/master/changes/changes-${version}.md

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
    version=$(git describe --tags --abbrev=0 | sed 's/^v//')

    info "Building and pushing Docker images for ${version}..."

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
# promote — move from dist/dev to dist/release
# ============================================================
cmd_promote() {
    cd "$PROJECT_ROOT"

    local version
    version=$(git describe --tags --abbrev=0 | sed 's/^v//')

    info "Promoting release ${version} from dist/dev to dist/release..."

    read -rp "SVN username (Apache ID): " svn_user

    svn mv "https://dist.apache.org/repos/dist/dev/skywalking/java-agent/${version}" \
           "https://dist.apache.org/repos/dist/release/skywalking/java-agent/${version}" \
           -m "Release Apache SkyWalking Java Agent ${version}" \
           --username "$svn_user"

    info "Release ${version} promoted."
    info "Next steps:"
    info "  1. Release the Nexus staging repository"
    info "  2. Update website download page"
    info "  3. Run: $0 email announce"
    info "  4. Run: $0 docker"
}

# ============================================================
# cleanup — remove old release from dist/release
# ============================================================
cmd_cleanup() {
    local old_version="${1:-}"
    if [ -z "$old_version" ]; then
        error "Usage: $0 cleanup <old_version>  (e.g., 9.5.0)"
    fi

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
    cmd_stage
    echo ""
    cmd_upload
    echo ""
    cmd_email vote
}

# ============================================================
# vote-passed — run all steps after the vote passes
# ============================================================
cmd_vote_passed() {
    local old_version="${1:-}"

    cmd_promote
    echo ""
    cmd_docker
    echo ""
    cmd_email announce

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
        email)        cmd_email "$@" ;;
        docker)       cmd_docker "$@" ;;
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
            echo "  $0 vote-passed [old_version]       # after vote"
            echo ""
            echo "Individual commands:"
            echo "  preflight                     Check tools and environment"
            echo "  prepare <ver> [next_ver]      Prepare release (branch, tag, PR)"
            echo "  stage                         Stage release (maven release:perform, build tars)"
            echo "  upload                        Upload to Apache SVN dist/dev"
            echo "  prepare-vote <ver> [next_ver] Run preflight + prepare + stage + upload + vote email"
            echo "  email [vote|announce]  Generate email content"
            echo "  promote                Move from dist/dev to dist/release in SVN"
            echo "  docker                 Build and push Docker images"
            echo "  vote-passed [old_ver]  Run promote + docker + announce email [+ cleanup]"
            echo "  cleanup <old_version>  Remove old release from dist/release"
            ;;
    esac
}

main "$@"
