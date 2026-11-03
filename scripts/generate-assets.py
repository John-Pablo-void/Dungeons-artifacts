#!/usr/bin/env python3
"""Original cuboid models and small procedural palettes; no third-party artwork.

Regenerates only the resource files owned by this generator. Geometry, colors and
display transforms are defined here so resources can be maintained reproducibly.
"""
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/dungeons_artifacts'
FACES = ('up', 'down', 'north', 'south', 'east', 'west')

def box(a, b, color=0):
    return {'from': a, 'to': b, 'faces': {
        f: {'texture': '#palette', 'uv': [color * 4, 0, color * 4 + 4, 16]} for f in FACES}}

def png(colors):
    pixels = bytearray()
    for y in range(16):
        pixels.append(0)
        for x in range(16):
            c = colors[x // 4]
            shade = (9 if (x + y) % 5 == 0 else -8 if (x * 3 + y) % 7 == 0 else 0)
            pixels.extend(max(0, min(255, v + shade)) for v in c)
            pixels.append(255)
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>2I5B', 16, 16, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(bytes(pixels), 9)) + chunk(b'IEND', b''))

def satchel():
    return [box([3, 2, 5], [13, 10, 11]), box([2, 8, 4], [14, 11, 12], 1),
            box([7, 7, 3], [9, 10, 5], 2), box([4, 11, 7], [6, 15, 9], 3),
            box([10, 11, 7], [12, 15, 9], 3), box([6, 14, 7], [10, 16, 9], 3)]

MODELS = {
    'soul_healer': ('Soul Healer', 'Uzdrowiciel dusz', ['164051', '37bfb0', 'c3f4dc', 'cb9f65'],
        [box([4, 2, 4], [12, 4, 12], 3), box([5, 4, 5], [11, 11, 11]),
         box([6, 6, 4], [10, 10, 5], 1), box([7, 7, 3], [9, 9, 4], 2),
         box([6, 11, 6], [10, 13, 10], 3), box([7, 13, 7], [9, 16, 9], 1)]),
    'satchel_of_elixirs': ('Satchel of Elixirs', 'Torba eliksirów', ['643842', 'bd7160', '8fe3c8', 'c5a580'], satchel()),
    'totem_of_shielding': ('Totem of Shielding', 'Totem osłony', ['455d67', '84bcd2', 'd2f8ff', 'e2b56b'],
        [box([2, 0, 2], [14, 3, 14]), box([6, 3, 6], [10, 13, 10], 3),
         box([3, 11, 4], [13, 15, 12], 1), box([1, 8, 6], [5, 13, 10]),
         box([11, 8, 6], [15, 13, 10]), box([6, 12, 3], [10, 14, 4], 2)]),
    'lightning_rod': ('Lightning Rod', 'Różdżka piorunów', ['473f66', 'b7a0e5', 'f6e88a', 'ae8456'],
        [box([7, 0, 7], [9, 12, 9], 3), box([4, 11, 5], [12, 13, 11]),
         box([4, 13, 6], [6, 16, 10], 1), box([10, 13, 6], [12, 16, 10], 1),
         box([7, 13, 7], [9, 17, 9], 2)]),
    'corrupted_seeds': ('Corrupted Seeds', 'Skażone nasiona', ['403047', '73508d', 'b0ca5a', '593f29'],
        [box([3, 2, 5], [13, 7, 11]), box([5, 7, 6], [11, 10, 10], 1),
         box([7, 10, 7], [9, 14, 9], 3), box([2, 11, 7], [7, 13, 9], 2),
         box([9, 13, 7], [14, 15, 9], 2)]),
    'gong_of_weakening': ('Gong of Weakening', 'Gong osłabienia', ['725544', 'bf8c45', 'f0cc71', '44445e'],
        [box([1, 0, 6], [15, 2, 10]), box([1, 2, 7], [3, 16, 9]),
         box([13, 2, 7], [15, 16, 9]), box([1, 14, 7], [15, 16, 9]),
         box([4, 5, 7], [12, 13, 9], 1), box([5, 4, 7], [11, 14, 9], 1),
         box([6, 7, 6], [10, 11, 10], 2)]),
    'satchel_of_elements': ('Satchel of Elements', 'Torba żywiołów', ['294d74', '528d97', 'e9b060', 'a9cadb'], satchel()),
    'death_cap_mushroom': ('Death Cap Mushroom', 'Grzyb śmierci', ['533867', 'a56cbd', 'dbceba', 'e7abd8'],
        [box([6, 1, 6], [10, 9, 10], 2), box([2, 8, 2], [14, 11, 14]),
         box([4, 11, 4], [12, 14, 12], 1), box([6, 14, 6], [10, 16, 10], 1),
         box([4, 11, 5], [6, 12, 7], 3), box([9, 11, 9], [11, 12, 11], 3)]),
    'ghost_cloak': ('Ghost Cloak', 'Upiorny płaszcz', ['334856', '648f9d', 'aee4df', 'd1b46d'],
        [box([2, 0, 7], [14, 4, 10]), box([3, 4, 7], [13, 8, 10], 1),
         box([4, 8, 6], [12, 12, 10], 1), box([5, 12, 5], [11, 16, 11]),
         box([6, 13, 4], [10, 15, 5], 2), box([7, 10, 5], [9, 12, 6], 3)]),
    'updraft_tome': ('Updraft Tome', 'Księga podmuchu', ['326478', 'ddd5b7', '9fe6dc', 'c4a165'],
        [box([2, 2, 5], [14, 3, 11]), box([2, 13, 5], [14, 14, 11]),
         box([2, 3, 5], [4, 13, 11]), box([4, 3, 6], [13, 13, 10], 1),
         box([7, 7, 4], [9, 12, 5], 2), box([5, 10, 4], [11, 12, 5], 2),
         box([11, 6, 4], [14, 8, 6], 3)]),
    'boots_of_levitation': ('Boots of Levitation', 'Buty lewitacji', ['344a75', '859cc9', 'dceaf1', 'd6b275'],
        [box([1, 1, 3], [7, 4, 13]), box([9, 1, 3], [15, 4, 13]),
         box([1, 4, 7], [7, 12, 13], 1), box([9, 4, 7], [15, 12, 13], 1),
         box([1, 11, 7], [7, 13, 13], 3), box([9, 11, 7], [15, 13, 13], 3),
         box([-1, 5, 7], [1, 9, 11], 2), box([15, 5, 7], [17, 9, 11], 2)])
}

DISPLAY = {
    'gui': {'rotation': [25, -35, 0], 'translation': [0, 0, 0], 'scale': [.85, .85, .85]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.4, .4, .4]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [.7, .7, .7]},
}
for side in ('right', 'left'):
    DISPLAY[f'firstperson_{side}hand'] = {'rotation': [0, -25 if side == 'right' else 25, 0], 'translation': [0, 1, 0], 'scale': [.6, .6, .6]}
    DISPLAY[f'thirdperson_{side}hand'] = {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]}

def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def main():
    langs = {'en_us': {}, 'pl_pl': {}}
    for name, (en, pl, palette, elements) in MODELS.items():
        write_json(ROOT / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'dungeons_artifacts:item/{name}'}})
        write_json(ROOT / f'models/item/{name}.json', {'gui_light': 'side', 'textures': {'palette': f'dungeons_artifacts:item/{name}', 'particle': f'dungeons_artifacts:item/{name}'}, 'display': DISPLAY, 'elements': elements})
        texture = ROOT / f'textures/item/{name}.png'
        texture.parent.mkdir(parents=True, exist_ok=True)
        texture.write_bytes(png([tuple(bytes.fromhex(c)) for c in palette]))
        langs['en_us'][f'item.dungeons_artifacts.{name}'] = en
        langs['pl_pl'][f'item.dungeons_artifacts.{name}'] = pl
    for lang, values in langs.items():
        write_json(ROOT / f'lang/{lang}.json', values)
    print(f'Generated {len(MODELS)} original models, item definitions and textures; 2 localization files.')

if __name__ == '__main__':
    main()
