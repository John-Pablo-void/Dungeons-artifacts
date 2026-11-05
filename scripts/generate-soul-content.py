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
js(Path('data/dungeons_artifacts/recipe/attuned_compass.json'),{'type':'minecraft:crafting_shaped','category':'misc','pattern':['ESE','ICI',' E '],'key':{'I':'minecraft:iron_ingot','E':'minecraft:echo_shard','S':'dungeons_artifacts:full_soul_bottle','C':'minecraft:compass'},'result':{'id':'dungeons_artifacts:attuned_compass','count':1}})
extra={
'message.dungeons_artifacts.echo_left':('Your soul echo waits at %s, %s, %s. Return to reclaim it.','Twoje echo dusz czeka w %s, %s, %s. Wróć, aby je odzyskać.'),
'message.dungeons_artifacts.echo_reclaimed':('Reclaimed %s souls from your echo','Odzyskano %s dusz z twojego echa'),
'commands.dungeons_artifacts.soul':('Soul %s: updated %s player(s), amount %s','Soul %s: zmieniono %s graczy, liczba %s'),

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

# Original segmented cyan XP-style bar, at vanilla's 182 x 5 dimensions.
for name,filled in [('background',False),('progress',True)]:
 rows=[]
 for y in range(5):
  row=[]
  for x in range(182):
   border=y in (0,4) or x in (0,181) or x%10==0
   color=(10,38,42,255) if border else ((82,236,239,255) if filled else (28,65,71,255))
   if filled and y==1 and not border:color=(161,255,255,255)
   row.extend(color)
  rows.append(bytes([0]+row))
 png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',182,5,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b'')
 path=root/f'assets/dungeons_artifacts/textures/gui/sprites/hud/soul_bar_{name}.png';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(png)
js(Path('data/dungeons_artifacts/tags/item/soul_artifacts.json'),{'replace':False,'values':['dungeons_artifacts:'+n for n in ['soul_healer','lightning_rod','ghost_cloak']]})
js(Path('data/dungeons_artifacts/advancement/recipes/attuned_compass.json'),{
 'parent':'minecraft:recipes/root',
 'criteria':{'artifact':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'#dungeons_artifacts:soul_artifacts'}]}}},
 'requirements':[['artifact']], 'rewards':{'recipes':['dungeons_artifacts:attuned_compass']}})
challenges={
 'root':('Artifacts','Artefakty','Find an artifact and begin your collection.','Znajdź artefakt i rozpocznij kolekcję.','soul_healer','task'),
 'god_of_lightning':('God of lightning','Bóg piorunów','Use the Lightning Rod 10 times within 5 seconds.','Użyj Różdżki piorunów 10 razy w ciągu 5 sekund.','lightning_rod','challenge'),
 'sold_your_soul':('You sold me your soul','Sprzedałeś mi swoją duszę','Capture a credited kill in an offhand bottle.','Zbierz dusze z zabójstwa do butelki w drugiej ręce.','souls_in_a_bottle','task'),
 'brought_a_bucket':('I should have brought a bucket','Trzeba było zabrać wiadro','Keep the Boots of Levitation active for their full 25 seconds.','Używaj Butów lewitacji przez pełne 25 sekund.','boots_of_levitation','challenge'),
 'deathcap':('DEATHcap shroom','ŚMIERĆomor','Die from eating a Death Cap Mushroom.','Zgiń po zjedzeniu muchomora śmierci.','death_cap_mushroom','challenge'),
 'soul_searching':('Soul searching','W poszukiwaniu duszy','Return to your death echo and reclaim your souls.','Wróć do echa śmierci i odzyskaj dusze.','ghost_cloak','goal'),
 'attuned':('The sculk chose me','Sculk wybrał mnie','Complete a soul attunement ritual.','Ukończ rytuał dostrojenia dusz.','attuned_compass','goal'),
 'collector':('A very heavy backpack','Bardzo ciężki plecak','Carry all eleven artifacts at once.','Noś jednocześnie wszystkie jedenaście artefaktów.','satchel_of_elements','challenge')}
artifacts=['soul_healer','satchel_of_elixirs','totem_of_shielding','lightning_rod','corrupted_seeds','gong_of_weakening','satchel_of_elements','death_cap_mushroom','ghost_cloak','updraft_tome','boots_of_levitation']
for name,(en,pl,desc_en,desc_pl,icon,frame) in challenges.items():
 criteria={'completed':{'trigger':'minecraft:impossible'}}
 if name=='root':criteria={'completed':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['dungeons_artifacts:'+a for a in artifacts]}]}}}
 if name=='collector':criteria={'completed':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'dungeons_artifacts:'+a} for a in artifacts]}}}
 data={'criteria':criteria,'requirements':[['completed']],'display':{'icon':{'id':'dungeons_artifacts:'+icon},'title':{'translate':'advancements.dungeons_artifacts.'+name+'.title'},'description':{'translate':'advancements.dungeons_artifacts.'+name+'.description'},'frame':frame,'show_toast':name!='root','announce_to_chat':name!='root','hidden':False}}
 if name=='root':data['display']['background']='minecraft:gui/advancements/backgrounds/adventure'
 else:data['parent']='dungeons_artifacts:artifacts/root'
 js(Path(f'data/dungeons_artifacts/advancement/artifacts/{name}.json'),data)
 for lang,title,desc in [('en_us',en,desc_en),('pl_pl',pl,desc_pl)]:
  path=Path(f'assets/dungeons_artifacts/lang/{lang}.json');data=json.loads((root/path).read_text());data['advancements.dungeons_artifacts.'+name+'.title']=title;data['advancements.dungeons_artifacts.'+name+'.description']=desc;js(path,data)
