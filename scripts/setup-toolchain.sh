#!/usr/bin/env bash
set -euo pipefail
toolchain_root=/workspace/toolchains
mkdir -p "$toolchain_root"
if [[ -x "$toolchain_root/java25/bin/javac" ]]; then
    "$toolchain_root/java25/bin/javac" -version 2>&1 | grep -Eq '^javac 25([ .]|$)'
    exit 0
fi
if [[ -e "$toolchain_root/java25" || -L "$toolchain_root/java25" ]]; then
    echo 'Existing java25 installation is incomplete; inspect it before replacing it.' >&2
    exit 1
fi
stage=$(mktemp -d "$toolchain_root/.jdk-install.XXXXXX")
trap 'rm -rf "$stage"' EXIT
curl --fail --silent --show-error --location --connect-timeout 30 \
    https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz \
    --output "$stage/jdk.tar.gz"
curl --fail --silent --show-error --location --connect-timeout 30 \
    https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz.sha256 \
    --output "$stage/jdk.sha256"
python3 - "$stage" <<'PY'
import hashlib, pathlib, re, sys
p = pathlib.Path(sys.argv[1])
expected = (p / 'jdk.sha256').read_text().strip()
if not re.fullmatch('[0-9a-f]{64}', expected):
    raise SystemExit('Invalid official SHA-256 response')
with (p / 'jdk.tar.gz').open('rb') as f:
    if hashlib.file_digest(f, 'sha256').hexdigest() != expected:
        raise SystemExit('JDK SHA-256 verification failed; archive will not be used')
print('JDK SHA-256 verified')
PY
mkdir "$stage/jdk"
tar -xzf "$stage/jdk.tar.gz" --strip-components=1 -C "$stage/jdk"
"$stage/jdk/bin/javac" -version 2>&1 | grep -Eq '^javac 25([ .]|$)'
mv "$stage/jdk" "$toolchain_root/java25"
"$toolchain_root/java25/bin/java" -version
