#!/bin/bash
# Copyright (c) 2020, Amazon.com, Inc. or its affiliates. All rights reserved.
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The contents of this file are subject to the terms of either the Universal Permissive License
# v 1.0 as shown at http://oss.oracle.com/licenses/upl
#
# or the following license:
#
# Redistribution and use in source and binary forms, with or without modification, are permitted
# provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this list of conditions
# and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice, this list of
# conditions and the following disclaimer in the documentation and/or other materials provided with
# the distribution.
#
# 3. Neither the name of the copyright holder nor the names of its contributors may be used to
# endorse or promote products derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
# IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
# FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
# WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

set -o errexit
set -o nounset
set -o pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
SRC_DIR="$ROOT_DIR/src"

HELP_MESSAGE=$(cat <<EOF
$0: Build JMC locally or inside a docker instance. Local is the default.
Usage: $0 [local|docker|clean|clean-docker-image|help]
EOF
)

DOCKER="docker"
IMAGE_TAG="jmc-build-p2:latest"
docker_build_dir="/jmc"

build_target="local"

JETTY_LOCAL_PORT=8080

VERSION="$(cat version.txt)"

if [[ $# -gt 0 ]]; then
  build_target="$1"
fi

mk_build_dir() {
  mkdir "$BUILD_DIR" 2>/dev/null || true
}

function copy_src_to_docker() {
  instance_name=$1
  pushd "$SRC_DIR"
  for f in core application configuration license pom.xml; do
    $DOCKER cp $f "$instance_name:/$docker_build_dir"
  done
  popd
}

function copy_artifacts_from_docker() {
  mk_build_dir
  instance_name=$1
  for f in $($DOCKER exec "$instance_name" bash -c "ls $docker_build_dir/target/products/org.openjdk.jmc-*"); do
    $DOCKER cp "$instance_name:$f" "$BUILD_DIR"
  done
}

function start_p2_jetty_local() {
  # Verify the port status and fail fast
  if netstat -ln | grep $JETTY_LOCAL_PORT; then
    echo "[ERROR] Port $JETTY_LOCAL_PORT is required but is occupied." >&2
    return 1
  fi
  pushd "$SRC_DIR/releng/third-party"
  mvn p2:site
  mvn jetty:run &
  jetty_pid=$!
  sleep 5 # Wait for jetty to start. A more appropriate approach is to detect when 8088 is listened
  trap 'kill -SIGINT $jetty_pid' EXIT
  popd
}

function repackage_artifact() {
  # Repackage raw JMC artifacts and add top level directory
  # org.openjdk.jmc-linux.gtk.x86_64.tar.gz
  # org.openjdk.jmc-win32.win32.x86_64.zip
  # org.openjdk.jmc-macosx.cocoa.x86_64.tar.gz

  pushd $BUILD_DIR
  for raw_artifact in *; do
    case "$raw_artifact" in
      *"linux"*)  OS="linux" ;;
      *"win32"*)  OS="windows" ;;
      *"macosx"*) OS="mac" ;;
      *)          echo "Cannot recognize the OS platfor of artifact $raw_artifact" ;;
    esac

    case "$raw_artifact" in
      *"x86_64"*)   ARCH="x64" ;;
      *)            echo "Cannot recognize the architecture of artifact $raw_artifact" ;;
    esac

    if [ -z ${OS+x} ] | [ -z ${ARCH+x} ]; then
      echo "Unable to repackage $raw_artifact"
      exit 1
    fi

    new_artifact_name="amazon-corretto-jmc-$VERSION-$OS-$ARCH"
    if [[ $raw_artifact == *"tar.gz" ]]; then
      echo "Repackaging $raw_artifact as $new_artifact_name.tar.gz"
      mkdir $new_artifact_name
      tar xfz $raw_artifact -C $new_artifact_name
      tar cfz "$new_artifact_name.tar.gz" $new_artifact_name
    elif [[ $raw_artifact == *"zip" ]]; then
      echo "Repackaging $raw_artifact as $new_artifact_name.zip"
      unzip -q $raw_artifact -d $new_artifact_name
      zip -rq "$new_artifact_name.zip" $new_artifact_name
    fi
    rm -rf $new_artifact_name $raw_artifact
  done
  popd
}

case "$build_target" in
  docker ) # docker build
    if ! hash $DOCKER; then
      echo "Cannot find $DOCKER: $DOCKER is required for $build_target" >&2
      exit 1
    fi
    INSTANCE_NAME="jmc-build-p2-$(uuidgen)"
    # Do not rebuild image if present
    if ! $DOCKER image inspect $IMAGE_TAG > /dev/null; then
      $DOCKER build --tag $IMAGE_TAG --file "$SRC_DIR/docker/Dockerfile-p2" "$SRC_DIR"
    fi
    $DOCKER run -d --rm --name "$INSTANCE_NAME" $IMAGE_TAG
    trap 'docker stop $INSTANCE_NAME' EXIT
    copy_src_to_docker "$INSTANCE_NAME"
    $DOCKER cp "$ROOT_DIR/script/build-jmc.sh" "$INSTANCE_NAME:/"
    $DOCKER exec "$INSTANCE_NAME" "/build-jmc.sh" "$docker_build_dir"
    copy_artifacts_from_docker "$INSTANCE_NAME"
    repackage_artifact
    ;;
  local ) # local build
    mk_build_dir
    start_p2_jetty_local
    bash "$ROOT_DIR/script/build-jmc.sh" "$SRC_DIR"
    cp "$SRC_DIR/target/products/org.openjdk.jmc-"* "$BUILD_DIR"
    repackage_artifact
    ;;
  clean ) # clearn artifacts
    rm -r "$BUILD_DIR"
    # mvn clean is a more proper way to delete all artifacts
    # but find is way more faster
    find "$SRC_DIR" -type d -name "target" -exec rm -r {} +
    ;;
  clean-docker-image ) # clean docker build image
    $DOCKER image rm "$IMAGE_TAG"
    ;;
  help ) # print help
    echo "$HELP_MESSAGE" >&2
    ;;
  * ) # default: unknown option
    echo "[ERROR] Build target \"$build_target\" is not supported" >&2
    echo "$HELP_MESSAGE" >&2
    exit 1
    ;;
esac

# vim: set ft=sh ts=2 sw=2 tw=79 :
