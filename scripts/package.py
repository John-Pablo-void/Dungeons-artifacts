#!/usr/bin/env python3
"""Package the built mod and a complete, cache-free source project."""
import hashlib
import shutil
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
version = next(line.split('=', 1)[1] for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('version='))
out = root / 'dist'
out.mkdir(exist_ok=True)
artifacts = []
for suffix in ('', '-sources'):
    name = f'dungeons-artifacts-{version}{suffix}.jar'
    source = root / 'build/libs' / name
    if not source.is_file():
        raise SystemExit('Run ./gradlew build successfully before packaging: ' + str(source))
    shutil.copy2(source, out / name)
    artifacts.append(out / name)

archive = out / f'dungeons-artifacts-{version}-project.zip'
folders = ['src', 'gradle', 'scripts', 'docs']
files = ['.gitignore', 'README.md', 'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat']
paths = [root / name for name in files]
for folder in folders:
    paths.extend(path for path in (root / folder).rglob('*') if path.is_file() and '__pycache__' not in path.parts)
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as target:
    for path in sorted(paths):
        target.write(path, Path('Dungeons-artifacts') / path.relative_to(root))
artifacts.append(archive)
lines = []
for path in artifacts:
    with path.open('rb') as stream:
        lines.append(hashlib.file_digest(stream, 'sha256').hexdigest() + '  ' + path.name)
(out / 'SHA256SUMS').write_text('\n'.join(lines) + '\n')
print('\n'.join(f'{path.name}: {path.stat().st_size:,} bytes' for path in artifacts))
