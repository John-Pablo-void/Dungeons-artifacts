#!/usr/bin/env python3
"""Validate resource links, model geometry and PNG integrity without launching Minecraft."""
import importlib.util
import json
import struct
import zlib
from pathlib import Path

base = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('assets', base / 'generate-assets.py')
assets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assets)
root = assets.ROOT

for item in assets.MODELS:
    definition = json.loads((root / f'items/{item}.json').read_text())
    assert definition['model']['type'] == 'minecraft:model'
    assert definition['model']['model'] == f'dungeons_artifacts:item/{item}'
    model = json.loads((root / f'models/item/{item}.json').read_text())
    assert len(model['elements']) >= 4
    assert {'gui', 'ground', 'fixed', 'firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'} <= model['display'].keys()
    for element in model['elements']:
        assert all(-16 <= a < b <= 32 for a, b in zip(element['from'], element['to']))
        assert set(element['faces']) == set(assets.FACES)
        for face in element['faces'].values():
            texture = model['textures'][face['texture'][1:]]
            namespace, path = texture.split(':')
            assert namespace == 'dungeons_artifacts'
            assert (root / f'textures/{path}.png').is_file()
            assert all(0 <= n <= 16 for n in face['uv'])
    data = (root / f'textures/item/{item}.png').read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    i, compressed = 8, bytearray()
    while i < len(data):
        size = struct.unpack('>I', data[i:i+4])[0]
        kind, payload = data[i+4:i+8], data[i+8:i+8+size]
        crc = struct.unpack('>I', data[i+8+size:i+12+size])[0]
        assert crc == zlib.crc32(kind + payload)
        if kind == b'IDAT': compressed.extend(payload)
        if kind == b'IHDR': assert struct.unpack('>2I5B', payload) == (16, 16, 8, 6, 0, 0, 0)
        i += size + 12
    assert len(zlib.decompress(compressed)) == 16 * (1 + 16 * 4)
    for lang in ('en_us', 'pl_pl'):
        assert json.loads((root / f'lang/{lang}.json').read_text(encoding='utf-8'))[f'item.dungeons_artifacts.{item}']
print('PASS: resource references, cuboid bounds, display contexts, PNG integrity and names for 11 artifacts. In-game rendering not tested.')
