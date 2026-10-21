#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Creates a SQLite database in exactly the format of Room schema version 1 (as written by the V1 app) and fills it with a few
records, so that CI can test an UPDATE from V1 to the newest build.   usage: make_v1_database.py <1.json> <out.db>"""
import json, os, sqlite3, sys

schema_file, out = sys.argv[1], sys.argv[2]
db = json.load(open(schema_file, encoding="utf-8"))["database"]
if os.path.exists(out): os.remove(out)
con = sqlite3.connect(out)
for e in db["entities"]:
    con.execute(e["createSql"].replace("${TABLE_NAME}", e["tableName"]))
for q in db["setupQueries"]:
    con.execute(q)
con.execute("PRAGMA user_version = %d" % db["version"])

def insert(table, **values):
    ent = next(e for e in db["entities"] if e["tableName"] == table)
    row = {}
    for f in ent["fields"]:
        name, typ = f["columnName"], f["affinity"]
        row[name] = values.get(name, 0 if typ == "INTEGER" else "")
    cols = ",".join('"%s"' % c for c in row)
    con.execute('INSERT INTO "%s" (%s) VALUES (%s)' % (table, cols, ",".join("?" * len(row))), list(row.values()))

NOW = 1760000000000
insert("projects", id=1, kind="PHD", title="Upgrade survivor — Nirukta thesis", createdAt=NOW, updatedAt=NOW)
insert("projects", id=2, kind="BOOK", title="Upgrade survivor — Book", createdAt=NOW, updatedAt=NOW)
insert("chapters", id=1, projectId=1, title="Upgrade chapter one")
insert("points", id=1, chapterId=1, title="Upgrade point")
insert("point_description_chunks", pointId=1, seq=0, text="Description text that must survive the update")
insert("manuscripts", id=1, scope="EDIT", title="Upgrade survivor manuscript", stakeholder="ORI", place="Mysuru")
insert("planner_books", id=1, listType="TRANS", title="Upgrade survivor translation book", author="A. Author")
insert("profile", id=1, prefix="Dr.", name="Upgrade Survivor", place="Mysuru")
con.commit(); con.close()
print("wrote", out)
