#!/usr/bin/env python3
"""Generate original shrine NBT and 26.2 data packs, enchantments and translations."""
import json, struct, gzip
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
IDS=['soul_healer','satchel_of_elixirs','totem_of_shielding','lightning_rod','corrupted_seeds','gong_of_weakening','satchel_of_elements','death_cap_mushroom','ghost_cloak','updraft_tome','boots_of_levitation']
def js(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def text(s):
 b=s.encode();return struct.pack('>H',len(b))+b
def payload(kind,v):
 if kind==1:return struct.pack('>b',v)
 if kind==6:return struct.pack('>d',v)
 if kind==11:return struct.pack('>i',len(v))+b''.join(struct.pack('>i',n) for n in v)
 if kind==3:return struct.pack('>i',v)
 if kind==8:return text(v)
 if kind==9:
  subtype,values=v;return bytes([subtype])+struct.pack('>i',len(values))+b''.join(payload(subtype,e) for e in values)
 if kind==10:return b''.join(bytes([t])+text(n)+payload(t,x) for n,(t,x) in v.items())+b'\0'
 raise ValueError(kind)

for variant,stone,accent in [('soul','deepslate_tiles','chiseled_deepslate'),('grove','mossy_stone_bricks','chiseled_stone_bricks'),('tempest','cut_copper','chiseled_tuff_bricks')]:
 palette=[];blocks={}
 def put(x,y,z,name,props=None,nbt=None):
  state={'Name':(8,'minecraft:'+name)}
  if props:state['Properties']=(10,{k:(8,v) for k,v in props.items()})
  if state not in palette:palette.append(state)
  b={'pos':(9,(3,[x,y,z])),'state':(3,palette.index(state))}
  if nbt:b['nbt']=(10,nbt)
  blocks[x,y,z]=b
 # Raised plinth, four pillars, open archways, a glowing reliquary with a loot chest.
 for x in range(9):
  for z in range(9):
   put(x,0,z,stone)
   if x in (0,8) or z in (0,8):put(x,1,z,accent)
 for x in (1,7):
  for z in (1,7):
   for y in range(1,5):put(x,y,z,accent)
   put(x,5,z,'sea_lantern')
 for i in range(1,8):
  for x,z in [(i,1),(i,7),(1,i),(7,i)]:put(x,5,z,stone)
 for z in (0,8):put(4,1,z,'air')
 for x in (0,8):put(x,1,4,'air')
 # Stepped arches over the four entrances.
 for side in (1,7):
  for v in range(2,7):
   height=6 if v in (3,4,5) else 5
   put(v,height,side,stone);put(side,height,v,stone)
 put(4,1,4,'sea_lantern');put(4,2,4,accent)
 for x,z in [(2,3),(6,5)]:
  put(x,1,z,'decorated_pot',{'facing':'north','cracked':'false','waterlogged':'false'}, {'id':(8,'minecraft:decorated_pot'),'LootTable':(8,'dungeons_artifacts:pots/artifact_shrine')})
 for artifact in [i for i in IDS if i!='death_cap_mushroom']:
  entity={'pos':(9,(6,[4.5,2.5,5.03125])),'blockPos':(9,(3,[4,2,5])),'nbt':(10,{'id':(8,'minecraft:item_frame'),'Facing':(1,3),'dungeons_artifacts_frame':(1,1),'block_pos':(11,[4,2,5]),'Item':(10,{'id':(8,'dungeons_artifacts:'+artifact),'count':(3,1)})})}
  tree={'DataVersion':(3,4903),'size':(9,(3,[9,8,9])),'palette':(9,(10,palette)),'blocks':(9,(10,list(blocks.values()))),'entities':(9,(10,[entity]))}
  p=ROOT/f'data/dungeons_artifacts/structure/{variant}_{artifact}_shrine.nbt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(gzip.compress(b'\x0a\x00\x00'+payload(10,tree),mtime=0))
  if artifact=='soul_healer': (ROOT/f'data/dungeons_artifacts/structure/{variant}_shrine.nbt').write_bytes(p.read_bytes())

js(Path('data/dungeons_artifacts/worldgen/template_pool/artifact_shrine.json'),{'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':f'dungeons_artifacts:{v}_{a}_shrine','processors':{'processors':[]},'projection':'rigid'}} for v in ('soul','grove','tempest') for a in IDS if a!='death_cap_mushroom']})

js(Path('data/dungeons_artifacts/worldgen/structure/artifact_shrine.json'),{'type':'minecraft:jigsaw','biomes':'#dungeons_artifacts:has_artifact_shrine','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'dungeons_artifacts:artifact_shrine','size':1,'start_height':{'absolute':0},'project_start_to_heightmap':'WORLD_SURFACE_WG','max_distance_from_center':32,'use_expansion_hack':False})
js(Path('data/dungeons_artifacts/worldgen/structure_set/artifact_shrines.json'),{'structures':[{'structure':'dungeons_artifacts:artifact_shrine','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':32,'separation':12,'salt':17319573}})
js(Path('data/dungeons_artifacts/tags/worldgen/biome/has_artifact_shrine.json'),{'replace':False,'values':['minecraft:plains','minecraft:forest','minecraft:birch_forest','minecraft:taiga','minecraft:meadow','minecraft:savanna']})
js(Path('data/dungeons_artifacts/loot_table/pots/artifact_shrine.json'),{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:emerald','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':4}}]},{'type':'minecraft:item','name':'dungeons_artifacts:death_cap_mushroom','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':2,'max':4}}]}]}]})
# Keep unopened legacy shrine chests compatible, with the same mushroom restrictions.
js(Path('data/dungeons_artifacts/loot_table/chests/artifact_shrine.json'),{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'dungeons_artifacts:'+i} for i in IDS if i!='death_cap_mushroom']},{'rolls':1,'entries':[{'type':'minecraft:item','name':'dungeons_artifacts:death_cap_mushroom','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':2,'max':4}}]}]}]})
js(Path('data/dungeons_artifacts/tags/item/artifacts.json'),{'replace':False,'values':['dungeons_artifacts:'+i for i in IDS if i!='death_cap_mushroom']})
for name,items,slots,levels in [('soul_vessel','#minecraft:enchantable/armor',['armor'],3),('soul_reservoir','#dungeons_artifacts:artifacts',['hand'],2),('soul_vacuum','#dungeons_artifacts:soul_vacuum_weapons',['mainhand'],1)]:
 js(Path(f'data/dungeons_artifacts/enchantment/{name}.json'),{'description':{'translate':'enchantment.dungeons_artifacts.'+name},'supported_items':items,'weight':5,'max_level':levels,'min_cost':{'base':5,'per_level_above_first':8},'max_cost':{'base':35,'per_level_above_first':8},'anvil_cost':2,'slots':slots,'effects':{}})
for tag in ['in_enchanting_table','non_treasure','on_random_loot','tradeable']:
 js(Path(f'data/minecraft/tags/enchantment/{tag}.json'),{'replace':False,'values':['dungeons_artifacts:soul_vessel','dungeons_artifacts:soul_reservoir','dungeons_artifacts:soul_vacuum']})
js(Path('assets/minecraft/punchy/compat/dungeons_artifacts.json'),{'customGroups':[{'name':'dungeons_artifacts','items':['dungeons_artifacts:'+i for i in IDS]}],'item':{'dungeons_artifacts':{'physics':'NONE'}}})
descriptions={
 'soul_healer':('Heal allies within 10 blocks with Instant Health II. Cooldown: 5s.','Natychmiastowe leczenie II sojuszników w promieniu 10 bloków. Odnowienie: 5 s.'),
 'satchel_of_elixirs':('Use: two positive effects. Sneak + use: throw a harmful splash potion. Cooldown: 30s.','Użycie: dwa pozytywne efekty. Skradanie + użycie: rzut szkodliwą miksturą. Odnowienie: 30 s.'),
 'totem_of_shielding':('Aim at ground: deploy a projectile shield. Defaults: 4 blocks, 10s, cooldown 30s.','Wyceluj w ziemię: osłona przed pociskami. Domyślnie: 4 bloki, 10 s, odnowienie 30 s.'),
 'lightning_rod':('Aim at a block: lightning with 5 + 5 HP damage. No cooldown.','Wyceluj w blok: piorun z obrażeniami 5 + 5 HP. Bez odnowienia.'),
 'corrupted_seeds':('Poison II for 15s; root enemies for 5s. Cooldown: 25s.','Zatrucie II na 15 s; unieruchomienie na 5 s. Odnowienie: 25 s.'),
 'gong_of_weakening':('Weakness for 30s to enemies within 10 blocks. Cooldown: 45s.','Osłabienie na 30 s dla wrogów w promieniu 10 bloków. Odnowienie: 45 s.'),
 'satchel_of_elements':('Random lightning, fire or levitation around you. Cooldown: 15s.','Losowy piorun, ogień albo lewitacja pobliskich wrogów. Odnowienie: 15 s.'),
 'death_cap_mushroom':('Consumed: Speed II 15s, Strength II 10s; magic self-damage. Cooldown: 10s.','Zużywalny: Szybkość II 15 s, Siła II 10 s; magiczne obrażenia własne. Odnowienie: 10 s.'),
 'ghost_cloak':('Phase through walls, unseen by mobs for 10s. Floors and ceilings remain solid. Cooldown: 30s.','Przenikaj przez ściany przez 10 s, niewidoczny dla mobów. Podłogi i sufity pozostają stałe. Odnowienie: 30 s.'),
 'updraft_tome':('Up to 6 targets: Levitation II 3s, then stun 4.5s. Next fall deals triple damage. Cooldown: 20s.','Maks. 6 celów: lewitacja II 3 s, potem ogłuszenie 4,5 s. Potrójne obrażenia upadku. Odnowienie: 20 s.'),
 'boots_of_levitation':('Toggle Levitation II, up to 25s. Early stop: half-duration cooldown; full use: 15s.','Włącz lub wyłącz lewitację II, maks. 25 s. Wcześniejszy koniec: połowa czasu jako odnowienie; pełne użycie: 15 s.')}
extra={
 'tag.item.dungeons_artifacts.artifacts':('Artifacts','Artefakty'),
 'itemGroup.dungeons_artifacts':('Dungeons Artifacts','Artefakty lochów'),
 'tooltip.dungeons_artifacts.cost':('Soul cost: %s','Koszt dusz: %s'),
 'hud.dungeons_artifacts.souls':('Souls: %s / %s','Dusze: %s / %s'),
 'hud.dungeons_artifacts.boots':('Levitation: %ss','Lewitacja: %s s'),
 'hud.dungeons_artifacts.cooldown':('Boots cooldown: %ss','Odnowienie butów: %s s'),
 'message.dungeons_artifacts.souls':('Not enough souls','Za mało dusz'),
 'message.dungeons_artifacts.cooldown':('Artifact is still recharging','Artefakt jeszcze się odnawia'),
 'message.dungeons_artifacts.target':('No valid target or placement','Brak odpowiedniego celu lub miejsca'),
 'effect.dungeons_artifacts.ghost':('Ghost','Duch'),
 'effect.dungeons_artifacts.rooted':('Entangled','Spętanie'),
 'effect.dungeons_artifacts.stunned':('Stunned','Ogłuszenie'),
 'enchantment.dungeons_artifacts.soul_vessel':('Soul Vessel','Naczynie dusz'),
 'enchantment.dungeons_artifacts.soul_reservoir':('Soul Reservoir','Zbiornik dusz')}
for i,lang in enumerate(['en_us','pl_pl']):
 p=Path(f'assets/dungeons_artifacts/lang/{lang}.json'); data=json.loads((ROOT/p).read_text())
 data.update({'tooltip.dungeons_artifacts.'+k:v[i] for k,v in descriptions.items()});data.update({k:v[i] for k,v in extra.items()});js(p,data)
# Distinct original status icons, generated from the original palette textures.
for name,source in [('ghost','ghost_cloak'),('rooted','corrupted_seeds'),('stunned','updraft_tome')]:
 p=ROOT/f'assets/dungeons_artifacts/textures/mob_effect/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((ROOT/f'assets/dungeons_artifacts/textures/item/{source}.png').read_bytes())
print('Generated 30 framed-artifact shrines plus 3 aliases, pot loot, 3 enchantments and bilingual descriptions.')
