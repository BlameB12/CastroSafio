import json
from pathlib import Path

manifest_path = Path('build/pack/manifest.json')
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))

# Epic ACG 20.12.5 hard-requires Epic Fight >=20.12.5,<20.13.0,
# while this pack intentionally stays on Epic Fight 20.14.17.
REMOVE_PROJECTS = {
    665625: 'Epic ACG 20.12.5.0 (incompatible with Epic Fight 20.14.17)'
}

REQUIRED_FIXES = [
    (1286233, 8021301, "Dungeons And Combat x Iron's Spells 1.5.4 (dacxirons)"),
    (445274, 5435202, "Relics 0.6.5.2"),
    (474114, 8465155, "Huge Structure Blocks Forge 1.1.10"),
    (326652, 8889041, "Cupboard 4.2"),
]

files = [f for f in manifest['files'] if f['projectID'] not in REMOVE_PROJECTS]
by_project = {f['projectID']: f for f in files}

for project_id, file_id, _name in REQUIRED_FIXES:
    by_project[project_id] = {
        'projectID': project_id,
        'fileID': file_id,
        'required': True,
        'isLocked': False,
    }

# Preserve existing order, then append newly required projects.
existing_order = [f['projectID'] for f in files]
fixed_files = []
seen = set()
for project_id in existing_order:
    if project_id in by_project and project_id not in seen:
        fixed_files.append(by_project[project_id])
        seen.add(project_id)
for project_id, _file_id, _name in REQUIRED_FIXES:
    if project_id not in seen:
        fixed_files.append(by_project[project_id])
        seen.add(project_id)

manifest['files'] = fixed_files
manifest['version'] = '1.0.2-dependencyfix'
manifest['name'] = 'RPG EPIC FIGHT - FULL COMPAT + MAX OPT - FIX2'
manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

notes_path = Path('build/pack/OPTIMIZATION_NOTES.txt')
notes = notes_path.read_text(encoding='utf-8') if notes_path.exists() else ''
notes += '\nDEPENDENCY FIX 2:\n'
for project_id, name in REMOVE_PROJECTS.items():
    notes += f'- REMOVED: {name} (project {project_id})\n'
for project_id, file_id, name in REQUIRED_FIXES:
    notes += f'- ADDED: {name} (project {project_id}, file {file_id})\n'
notes += '- Reason: fixes mandatory dependency failures reported by Forge on first launch.\n'
notes_path.write_text(notes, encoding='utf-8')

added_html = Path('build/pack/ADDED_MODS.html')
html = added_html.read_text(encoding='utf-8') if added_html.exists() else '<ul>\n</ul>\n'
insert = ''.join(f'<li>{name}</li>\n' for _, _, name in REQUIRED_FIXES)
if '</ul>' in html:
    html = html.replace('</ul>', insert + '</ul>')
else:
    html += insert
added_html.write_text(html, encoding='utf-8')

assert len(manifest['files']) == 191, len(manifest['files'])
assert 665625 not in {f['projectID'] for f in manifest['files']}
for project_id, file_id, _ in REQUIRED_FIXES:
    matches = [f for f in manifest['files'] if f['projectID'] == project_id]
    assert len(matches) == 1 and matches[0]['fileID'] == file_id

print('Dependency fix applied. Final entries:', len(manifest['files']))
