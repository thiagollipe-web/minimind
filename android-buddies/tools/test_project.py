#!/usr/bin/env python3
"""Headless tests for the policy, neural core, catalogs and actual SQL statements.
Does not emulate Android lifecycle, UI, audio or permissions.
"""
import json,re,sqlite3,subprocess,tempfile
from pathlib import Path
base=Path(__file__).resolve().parents[1];src=base/'app/src/main/java/com/brittech/minibuddy'
catalogs=list((base/'catalogs').glob('*.json'));assert len(catalogs)==12
ids=set();lesson_count=0
references=json.loads((base/'curriculum/references.json').read_text())['references']
for p in catalogs:
 c=json.loads(p.read_text());assert c['id']==p.stem
 lessons={l['id']:l for l in c['lessons']};expected={6,7,8,9} if p.stem in ['matematica','ingles','portugues','ciencias','historia','geografia'] else {11,12,13}
 assert {l['grade'] for l in lessons.values()}==expected
 assert len([q for q in c['questions'] if 'lesson_id' not in q])==3 # old review IDs remain available
 for lesson in lessons.values():
  lesson_count+=1;assert len(lesson['body'])>100 and lesson['example'] and lesson['objective']
  ref=references[lesson['bncc']];assert lesson['source']==ref['source'] and lesson['source_pdf_page']==ref['pdf_page']
  assert len([q for q in c['questions'] if q.get('lesson_id')==lesson['id']])==1
  code=lesson['bncc'];grade=lesson['grade']
  if grade<10:
   assert code.startswith('EF');span=code[2:4];assert grade in ({6,7} if span=='67' else {8,9} if span=='89' else {int(span)})
  else:assert code.startswith('EM13') and 'sugerida' in lesson['alignment_note'] or p.stem=='espanhol'
  if p.stem=='espanhol':assert lesson['alignment']=='complementar' and code.startswith('EM13LGG')
 for q in c['questions']:
  assert q['id'] not in ids;ids.add(q['id'])
  for key in ['options','reviewOptions']:assert len(q[key])==4 and len(set(q[key]))==4
  assert q['question']!=q['reviewQuestion'] and q['explanation']
  assert 0<=q['answer']<4 and 0<=q['reviewAnswer']<4
  if 'lesson_id' in q:
   lesson=lessons[q['lesson_id']];assert q['bncc']==lesson['bncc'] and q['grade']==lesson['grade'];assert q['reviewExplanation']
assert lesson_count==42
print('Catálogo BNCC: 42 miniaulas + 42 exercícios + 42 revisões, em 12 matérias; 36 IDs antigos preservados')
code=(src/'EducationDb.java').read_text()
# Real schema statements, exercised using desktop SQLite (not Android lifecycle APIs).
create_statements=re.findall(r'd\.execSQL\("((?:CREATE TABLE|INSERT INTO profiles)[^"]+)"\)',code)
lesson_schema=next(q for q in create_statements if 'lesson_views' in q)
def fresh():
 d=sqlite3.connect(':memory:')
 for sql in create_statements:d.execute(sql)
 return d
db=fresh()
for profile in ['a','b']:db.execute('INSERT INTO profiles VALUES(?,?)',(profile,profile))
# Fetch the actual parameterized SELECTs used in the production reward code.
due_query=re.search(r'rawQuery\("(SELECT id,qid,created FROM attempts[^"]+)"',code).group(1)
review_query=re.search(r'rawQuery\("(SELECT context,action,policy,strategy_seen FROM attempts[^"]+)"',code).group(1)
insert="INSERT INTO attempts(profile,qid,skill,context,action,prob,policy,correct,assisted,created,lesson_id,strategy_seen) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)"
db.execute(insert,('a','q1','skill','skill|iniciar',1,.91,'adaptativo',1,1,100,'lesson1',1))
assert db.execute("SELECT COUNT(*) FROM attempts WHERE profile='b'").fetchone()[0]==0
assert not db.execute(due_query,('a',99,99)).fetchall()
assert db.execute(due_query,('a',100,100)).fetchall()
db.execute('INSERT INTO lesson_views VALUES(?,?,?)',('a','lesson1',150))
assert not db.execute(due_query,('a',100,100)).fetchall() # opening the lesson delays unaided review
assert not db.execute(review_query,('a',1,100,100)).fetchall()
assert db.execute(review_query,('a',1,200,200)).fetchall() # window elapsed
# Another profile's lesson opening must not delay the student's review.
db.execute('INSERT INTO lesson_views VALUES(?,?,?)',('b','lesson1',500))
assert db.execute(review_query,('a',1,200,200)).fetchall()
for _ in range(2):
 row=db.execute(review_query,('a',1,200,200)).fetchone()
 if row:
  ctx,action,policy,strategy_seen=row
  db.execute('UPDATE attempts SET reviewed=1,review_correct=?,review_time=? WHERE profile=? AND id=?',(1,1800000,'a',1))
  if policy=='adaptativo' and strategy_seen:
   db.execute('INSERT OR IGNORE INTO strategies VALUES(?,?,?,0,0)',('a',ctx,action))
   db.execute('UPDATE strategies SET n=n+1,total=total+? WHERE profile=? AND context=? AND action=?',(1,'a',ctx,action))
assert db.execute('SELECT n,total FROM strategies').fetchone()==(1,1.0)
# Reading a lesson is support but is not exposure to a selected strategy.
db.execute(insert,('a','q2','other','other|iniciar',2,.03,'adaptativo',1,1,100,'lesson2',0))
assert db.execute(review_query,('a',2,200,200)).fetchone()[3]==0
for _ in range(2):db.execute('INSERT OR IGNORE INTO rewards VALUES(?,?,?)',('a','quest',25))
assert db.execute('SELECT SUM(xp) FROM rewards').fetchone()[0]==25
# Upgrade the exact v1 fixture with the actual ALTER/UPDATE statements from onUpgrade.
legacy=sqlite3.connect(':memory:');legacy.executescript((base/'tests/education_v1.sql').read_text())
legacy.execute("INSERT INTO attempts(profile,qid,skill,context,action,prob,policy,correct,assisted,created) VALUES('default','matematica_0','frações','ctx',0,1,'adaptativo',1,1,100)")
legacy.execute("INSERT INTO knowledge(profile,question,text,status,created,updated) VALUES('default','Pergunta antiga','Texto aprovado preservado','aprovado',10,20)")
legacy.execute("INSERT INTO rewards VALUES('default','missao_antiga',25)")
upgrade=code[code.index('public void onUpgrade'):code.index('public synchronized void openLesson')]
for sql in re.findall(r'd\.execSQL\("([^"]+)"\)',upgrade):legacy.execute(sql)
legacy.execute(lesson_schema)
assert legacy.execute('SELECT qid,assisted,strategy_seen FROM attempts').fetchone()==('matematica_0',1,1)
assert legacy.execute('SELECT text,status FROM knowledge').fetchone()==('Texto aprovado preservado','aprovado')
assert legacy.execute('SELECT xp FROM rewards').fetchone()[0]==25
legacy.execute(insert,('default','new','new','new',0,1,'fixo',1,0,500,'newlesson',0))
assert len(list(legacy.execute('SELECT * FROM attempts')))==2
print('SQLite: migração v1→v2 sem perda de registros; perfil, apoio recente, revisão atrasada e recompensa única OK')
with tempfile.TemporaryDirectory() as out:
 files=[src/(n+'.java') for n in ['Brain','TextTools','InteractionLogic','PetState','MissionLab','PedagogyPolicy','BuddyArt']]
 tests=[base/'tests'/(n+'.java') for n in ['CoreTest','InteractionTest','MissionsTest','PolicyTest','BuddyArtTest']]
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*map(str,files+tests)],check=True)
 for test in tests:subprocess.run(['java','-cp',out,test.stem],check=True,cwd=out)
