CREATE TABLE profiles(id TEXT PRIMARY KEY,name TEXT NOT NULL);
INSERT INTO profiles VALUES('default','Estudante');
CREATE TABLE attempts(id INTEGER PRIMARY KEY,profile TEXT,qid TEXT,skill TEXT,context TEXT,action INTEGER,prob REAL,policy TEXT,correct INTEGER,assisted INTEGER,created INTEGER,reviewed INTEGER DEFAULT 0,review_correct INTEGER,review_time INTEGER);
CREATE TABLE strategies(profile TEXT,context TEXT,action INTEGER,n INTEGER,total REAL,PRIMARY KEY(profile,context,action));
CREATE TABLE knowledge(id INTEGER PRIMARY KEY,profile TEXT,question TEXT,text TEXT,status TEXT,created INTEGER,updated INTEGER);
CREATE TABLE revisions(id INTEGER PRIMARY KEY,knowledge_id INTEGER,profile TEXT,text TEXT,status TEXT,created INTEGER);
CREATE TABLE rewards(profile TEXT,mission TEXT,xp INTEGER,PRIMARY KEY(profile,mission));
