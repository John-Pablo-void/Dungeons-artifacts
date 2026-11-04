#!/usr/bin/env python3
"""Original bottle sprites and soul-system data; no external artwork."""
import json,struct,zlib
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def js(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
for name,fill in [('souls_in_a_bottle',4),('full_soul_bottle',9),('liquid_souls',9)]:
 pixels=[]
 for y in range(16):
  row=[]
  for x in range(16):
   c=(0,0,0,0)
   if 6<=x<=9 and 1<=y<=4 or 3<=x<=12 and 5<=y<=13:
    c=(83,142,160,190)
    if 4<=x<=11 and 6<=y<=12:c=(30,60,71,100)
    if 4<=x<=11 and 14-fill<=y<=12:c=(52,231,216,255) if name!='liquid_souls' else (55,165,245,255)
    if x==4 and 6<=y<=11:c=(199,247,251,230)
    if y==1:c=(130,98,63,255)
   row.extend(c)
  pixels.append(bytes([0]+row))
 def chunk(k,d):return struct.pack('>I',len(d))+k+d+struct.pack('>I',zlib.crc32(k+d))
 png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(pixels)))+chunk(b'IEND',b'')
 (root/f'assets/dungeons_artifacts/textures/item/{name}.png').write_bytes(png)
 js(Path(f'assets/dungeons_artifacts/models/item/{name}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'dungeons_artifacts:item/{name}'}})
 js(Path(f'assets/dungeons_artifacts/items/{name}.json'),{'model':{'type':'minecraft:model','model':f'dungeons_artifacts:item/{name}'}})
# Use vanilla's animated compass needle with the lodestone target component.
import zipfile
jar=Path('/workspace/toolchains/gradle-home/caches/fabric-loom/26.2/minecraft-client.jar')
# The source definition is checked in; regenerating outside the cloud needs no Minecraft JAR.
compass=root/'assets/dungeons_artifacts/items/attuned_compass.json'
if jar.exists():
 with zipfile.ZipFile(jar) as z:compass.write_bytes(z.read('assets/minecraft/items/compass.json'))
js(Path('data/dungeons_artifacts/tags/item/soul_vacuum_weapons.json'),{'replace':False,'values':['#minecraft:swords','#minecraft:axes','#minecraft:spears','minecraft:bow','minecraft:mace']})
js(Path('data/dungeons_artifacts/recipe/attuned_compass.json'),{'type':'minecraft:crafting_shaped','category':'misc','pattern':['IEI','SCS','IEI'],'key':{'I':'minecraft:iron_ingot','E':'minecraft:echo_shard','S':'dungeons_artifacts:full_soul_bottle','C':'minecraft:compass'},'result':{'id':'dungeons_artifacts:attuned_compass','count':1}})
extra={
'item.dungeons_artifacts.souls_in_a_bottle':('Souls in a Bottle','Dusze w butelce'),
'item.dungeons_artifacts.full_soul_bottle':('Souls in a Bottle (Full)','Dusze w butelce (pełna)'),
'item.dungeons_artifacts.liquid_souls':('Liquid Souls','Płynne dusze'),
'item.dungeons_artifacts.attuned_compass':('Attuned Compass','Dostrojony kompas'),
'enchantment.dungeons_artifacts.soul_vacuum':('Soul Vacuum','Próżnia dusz'),
'key.dungeons_artifacts.soul_meter':('Toggle soul meter','Przełącz licznik dusz'),
'tooltip.dungeons_artifacts.bottle':('Souls: %s / 50','Dusze: %s / 50'),
'message.dungeons_artifacts.compass_broke':('Your attuned compass shattered: the altar is within 20 blocks.','Twój dostrojony kompas pękł: ołtarz jest bliżej niż 20 bloków.'),
'message.dungeons_artifacts.attuned':('Soul attunement level %s: +%s souls gathered','Poziom dostrojenia dusz %s: +%s zbieranych dusz')}
for i,lang in enumerate(['en_us','pl_pl']):
 p=Path(f'assets/dungeons_artifacts/lang/{lang}.json');d=json.loads((root/p).read_text());d.update({k:v[i] for k,v in extra.items()});js(p,d)
