import json
from pathlib import Path

manifest_path = Path('build/pack/manifest.json')
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))

# Keep Epic ACG itself. Instead of downgrading Epic Fight from 20.14.17,
# add the current GPLv3 compatibility bridge tested against Epic Fight 20.14.17.
# Relics is pinned to 0.8.0.13 because it is compatible with OctoLib 0.5.0.1,
# while Relics 0.6.5.2 still references the removed OctoConfig class.
REQUIRED_FIXES = [
    (1286233, 8021301, "Dungeons And Combat x Iron's Spells 1.5.4 (dacxirons)"),
    (445274, 7708970, "Relics 0.8.0.13"),
    (474114, 8465155, "Huge Structure Blocks Forge 1.1.10"),
    (326652, 8889041, "Cupboard 4.2"),
    (1737786, 9206668, "Epic ACG Compatibility Fix 2.2.28 for Epic Fight 20.14.17"),
]

files = list(manifest['files'])
by_project = {f['projectID']: f for f in files}

for project_id, file_id, _name in REQUIRED_FIXES:
    by_project[project_id] = {
        'projectID': project_id,
        'fileID': file_id,
        'required': True,
        'isLocked': False,
    }

# Preserve original order, then append newly required projects.
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
manifest['version'] = '1.0.4-relics-octolib-fix'
manifest['name'] = 'RPG EPIC FIGHT - FULL COMPAT + MAX OPT - FIX4 ACG'
manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

notes_path = Path('build/pack/OPTIMIZATION_NOTES.txt')
notes = notes_path.read_text(encoding='utf-8') if notes_path.exists() else ''
notes += '\nFIX4 - RELICS / OCTOLIB COMPATIBILITY:\n'
notes += '- KEPT: Epic ACG 20.12.5.0 original (project 665625, file 8430532).\n'
notes += '- KEPT: Epic Fight 20.14.17 + Epic ACG Compatibility Fix 2.2.28.\n'
for project_id, file_id, name in REQUIRED_FIXES:
    notes += f'- PINNED/ADDED: {name} (project {project_id}, file {file_id})\n'
notes += '- Relics upgraded from 0.6.5.2 to 0.8.0.13 to match OctoLib 0.5.0.1 and remove the missing OctoConfig crash.\n'
notes_path.write_text(notes, encoding='utf-8')

added_html = Path('build/pack/ADDED_MODS.html')
html = added_html.read_text(encoding='utf-8') if added_html.exists() else '<ul>\n</ul>\n'
insert = ''.join(f'<li>{name}</li>\n' for _, _, name in REQUIRED_FIXES)
if '</ul>' in html:
    html = html.replace('</ul>', insert + '</ul>')
else:
    html += insert
added_html.write_text(html, encoding='utf-8')

assert len(manifest['files']) == 193, len(manifest['files'])
projects = {f['projectID']: f['fileID'] for f in manifest['files']}
assert projects[665625] == 8430532
assert projects[1737786] == 9206668
for project_id, file_id, _ in REQUIRED_FIXES:
    assert projects[project_id] == file_id

print('FIX4 Relics/OctoLib compatibility applied. Final entries:', len(manifest['files']))
