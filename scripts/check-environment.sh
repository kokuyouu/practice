#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
"${PYTHON_BIN:-python3}" cmd/hello.py
"${JAVA_BIN:-java}" internal/Hello.java
git --version
