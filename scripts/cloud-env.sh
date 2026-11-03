#!/usr/bin/env bash
# Source this in the managed cloud environment; no credentials are stored here.
export JAVA_HOME=/workspace/toolchains/java25
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
# Java does not automatically consume HTTPS_PROXY. Preserve the platform route.
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    _da_proxy_opts=$(python3 - <<'PY'
import os
from urllib.parse import urlparse
p = urlparse(os.environ['HTTPS_PROXY'])
if p.username or p.password:
    raise SystemExit('Credential-bearing proxy URLs require supported platform Java configuration')
if not p.hostname or not p.port:
    raise SystemExit('Expected an explicit platform proxy host and port')
print(f'-Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port} -Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port} -Dhttp.nonProxyHosts=localhost|127.*|[::1]')
PY
    ) || return 1
    export GRADLE_OPTS="${GRADLE_OPTS:-} $_da_proxy_opts"
    unset _da_proxy_opts
fi
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
    export GRADLE_OPTS="${GRADLE_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
