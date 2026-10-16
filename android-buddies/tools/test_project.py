#!/usr/bin/env python3
"""Headless tests for the policy, neural core, catalogs and actual SQL statements.
Does not emulate Android lifecycle, UI, audio or permissions.
"""
import json,re,sqlite3,subprocess,tempfile
from pathlib import Path
base=Path(__file__).resolve().parents[1];src=base/'app/src/main/java/com/brittech/minibuddy'
catalogs=list((base/'catalogs').glob('*.json'));assert len(catalogs)==12
ids=set()
for p in catalogs:
 c=json.loads(p.read_text());assert c['id']==p.stem
 assert len(c['questions'])==3
 for q in c['questions']:
  assert q['id'] not in ids;ids.add(q['id'])
  for key in ['options','reviewOptions']:assert len(q[key])==4 and len(set(q[key]))==4
  assert q['question']!=q['reviewQuestion'] and q['explanation']
  assert 0<=q['answer']<4 and 0<=q['reviewAnswer']<4
print('Catálogos: 12 matérias, 36 atividades e 36 revisões estruturalmente válidas')
# Execute the real schema and reward statements using SQLite, without Android wrappers.
code=(src/'EducationDb.java').read_text();db=sqlite3.connect(':memory:')
for sql in re.findall(r'd\.execSQL\("((?:CREATE TABLE|INSERT INTO profiles)[^"]+)"\)',code):db.execute(sql)
for profile in ['a','b']:db.execute('INSERT INTO profiles VALUES(?,?)',(profile,profile))
db.execute("INSERT INTO attempts(profile,qid,skill,context,action,prob,policy,correct,assisted,created) VALUES('a','q','skill','skill|iniciar',1,.91,'adaptativo',1,1,0)")
assert db.execute("SELECT COUNT(*) FROM attempts WHERE profile='b'").fetchone()[0]==0
# Review reward once (same condition used by EducationDb.review).
for _ in range(2):
 row=db.execute("SELECT context,action,policy,assisted FROM attempts WHERE profile=? AND id=? AND reviewed=0 AND created<=?",('a',1,1)).fetchone()
 if row:
  ctx,action,policy,assisted=row
  db.execute('UPDATE attempts SET reviewed=1,review_correct=?,review_time=? WHERE profile=? AND id=?',(1,1800000,'a',1))
  db.execute('INSERT OR IGNORE INTO strategies VALUES(?,?,?,0,0)',('a',ctx,action))
  db.execute('UPDATE strategies SET n=n+1,total=total+? WHERE profile=? AND context=? AND action=?',(1,'a',ctx,action))
assert db.execute('SELECT n,total FROM strategies').fetchone()==(1,1.0)
for _ in range(2):db.execute('INSERT OR IGNORE INTO rewards VALUES(?,?,?)',('a','quest',25))
assert db.execute('SELECT SUM(xp) FROM rewards').fetchone()[0]==25
assert '&&assisted==1' in code
print('SQLite: separação entre perfis e proteção contra dupla recompensa OK (sem executar APIs Android)')
with tempfile.TemporaryDirectory() as out:
 files=[src/(n+'.java') for n in ['Brain','TextTools','InteractionLogic','PetState','MissionLab','PedagogyPolicy','BuddyArt']]
 tests=[base/'tests'/(n+'.java') for n in ['CoreTest','InteractionTest','MissionsTest','PolicyTest','BuddyArtTest']]
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*map(str,files+tests)],check=True)
 for test in tests:subprocess.run(['java','-cp',out,test.stem],check=True,cwd=out)
