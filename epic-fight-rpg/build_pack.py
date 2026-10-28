import json
from pathlib import Path

BASE_FILES = [
    (1567921,8833284),(1094533,5909525),(280510,7612291),(1482989,8393709),
    (644704,8400008),(1271749,7844812),(1429793,9013630),(1540142,8152400),
    (1031927,5438104),(1542044,8560699),(1128235,7193486),(1184379,7679941),
    (405076,8049910),(1432746,9326774),(426558,5698791),(361276,7263076),
    (872712,7686018),(1434276,8681446),(250498,7815705),(561625,5125710),
    (972044,5597594),(1090102,9326135),(1207952,6245509),(1164949,7046903),
    (1268823,8849430),(331936,7476570),(388172,8285794),(1035265,6410303),
    (871522,7501597),(855414,8680180),(1571005,8655335),(348521,5729105),
    (1001614,8095554),(499980,9354258),(1272352,7144759),(1380365,8682091),
    (1158628,6589093),(227639,5468648),(1485770,8393909),(1729231,9104020),
    (1676741,9329694),(1314084,8123166),(1435106,9264596),(536254,7985699),
    (1111833,6729020),(1377795,9317571),(1239499,7008014),(1423319,9026711),
    (240630,5057220),(981139,8982671),(686437,7917933),(324717,8479276),
    (944035,9301655),(1151542,6931390),(667299,6336646),(1531111,9043675),
    (1339577,9024529),(362479,9339180),(255308,6134920),(806044,8357966),
    (916747,6274623),(817709,9151117),(428151,8753894),(1743441,9345399),
    (1036425,8078907),(421850,5769971),(1063296,9245915),(1690092,8999698),
    (1169738,7656913),(650221,8308946),(1203668,8925506),(521480,9243644),
    (531188,9346774),(1231016,7409314),(535489,7032235),(661376,7830334),
    (1348970,7141536),(1281310,9298017),(1079687,9343358),(918614,8632596),
    (235577,8019497),(926263,8414201),(852665,7232503),(1492763,9328058),
    (1336105,8594666),(1469082,8710903),(1445154,7529944),(900642,7416260),
    (438447,8592325),(520110,5838149),(1289060,6819930),(1365932,8701601),
    (933001,9334355),(1307070,7004921),(1612974,8895434),(542360,5777945),
    (658587,4587214),(238222,8778011),(605375,7787515),(1584283,8539656),
    (1593587,8347709),(1106143,9121251),(901344,5193815),(985226,5586323),
    (551586,8246047),(243121,6427817),(580689,6640968),(815548,6095052),
    (955605,4992038),(1622496,8504891),(1358098,9076919),(328085,7178761),
    (1291754,9154271),(968868,7335229),(896746,8657397),(990013,8687886),
    (412082,9140345),(1721866,9033933),(1663674,8706652),(308989,5281700),
    (1167801,7955163),(952176,5214748),(309927,6418456),(1399324,7817167),
    (1435127,9248898),(1141465,7053846),(245755,8849482),(495476,9130247),
    (1587709,8960793),(1109064,7424091),(1307848,9015395),(1354901,7702162),
    (1635301,9346742),(1623162,8626439),(924854,5848216),(1188180,6964563),
    (941573,5057880),(1445144,7619585),(404183,5598990),(1001151,9333805),
    (959460,8288379),(491794,7892927),(289240,5046069),(1454101,7644948),
    (626676,8830518),(944203,5586345),(1645653,9332331),(638417,7171906),
    (630254,7396304),(1483383,7742473),(532127,4662781),(1435097,7486522),
    (988533,6415373),(693313,9113598),(638111,4650325),(618298,9341400),
    (1741041,9342053),(1477036,9342806),(422301,9341433),(574409,5019620),
    (570073,5659871),(659011,5906086),(419699,5137938),(1225584,6471144),
    (1416297,7532641),(474817,4703671),(531761,8969708),(1355708,8424016),
    (714059,6231176),(665625,8430532),(316582,7018272),(817651,5193465),
    (663658,5326142),
]

# Additions selected for this exact 1.20.1 Forge + Epic Fight pack.
ADDITIONS = [
    # Performance / memory / rendering
    (790626,9309378,"ModernFix 5.27.91"),
    (429235,4810975,"FerriteCore 6.0.1"),
    (908741,5681725,"Embeddium 0.3.31"),
    (448233,9100267,"Entity Culling 1.11.3"),
    (686911,7313094,"ImmediatelyFast 1.5.3"),
    (949555,7338200,"BadOptimizations 2.4.1"),
    (256717,5278538,"Clumps 12.0.0.4"),
    (475117,7527943,"FastSuite 5.1.2"),
    (283644,6274231,"Placebo 8.6.3 (FastSuite dependency)"),

    # Epic Fight compatibility
    (1338443,7725196,"P1nero's Epic Bow"),
    (1555371,9264762,"Epic Fight x Twilight Forest Compat 1.1.8 Fix 1"),
    (1389133,9296365,"Epic Fight x Curios Compat 4.0"),

    # Loot Integrations addons matching installed structure/boss mods
    (1134260,7920425,"Loot Integrations: L_Ender's Cataclysm"),
    (1149272,8922518,"Loot Integrations: Integrated Dungeons/Villages/Strongholds/Cataclysm"),
    (1146846,8922248,"Loot Integrations: The Graveyard"),
]

assert len(BASE_FILES) == 173, f"Expected 173 original files, got {len(BASE_FILES)}"
existing_projects = {p for p, _ in BASE_FILES}
for p, f, name in ADDITIONS:
    if p in existing_projects:
        raise RuntimeError(f"Addition already present in base manifest: {name} ({p})")

all_files = BASE_FILES + [(p, f) for p, f, _ in ADDITIONS]
assert len(all_files) == 188

manifest = {
    "minecraft": {
        "version": "1.20.1",
        "modLoaders": [{"id": "forge-47.4.28", "primary": True}],
    },
    "manifestType": "minecraftModpack",
    "manifestVersion": 1,
    "name": "RPG EPIC FIGHT - FULL COMPAT + MAX OPT",
    "version": "1.0.1-fullcompat-maxopt",
    "author": "CastroSafio",
    "overrides": "overrides",
    "files": [
        {"projectID": p, "fileID": f, "required": True, "isLocked": False}
        for p, f in all_files
    ],
}

out = Path("build/pack")
out.mkdir(parents=True, exist_ok=True)
(out / "overrides").mkdir(exist_ok=True)
(out / "overrides" / ".keep").write_text("", encoding="utf-8")
(out / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

notes = [
    "RPG EPIC FIGHT - FULL COMPAT + MAX OPT",
    "Minecraft 1.20.1 | Forge 47.4.28",
    "",
    f"Original CurseForge entries preserved: {len(BASE_FILES)}",
    f"New entries added: {len(ADDITIONS)}",
    f"Total entries: {len(all_files)}",
    "",
    "ADDED:",
]
notes.extend(f"- {name} (project {p}, file {f})" for p, f, name in ADDITIONS)
notes += [
    "",
    "NOT AUTO-ADDED ON PURPOSE:",
    "- Old Epic Fight datapack compats that explicitly require Epic Fight 20.5.17.",
    "- Extra Cataclysm weapon-overhaul addons that can overlap existing Epic Fight Mod Compat / WoM / Nightfall movesets.",
    "- Canary/Radium-style aggressive tick mixin optimizers; omitted to reduce conflict risk in this very large combat pack.",
    "- Oculus/shaders; not added because shader support was not part of this pack request.",
    "",
    "NOTE: Entity Culling is client-side. If a modded spell/effect ever visually disappears while still functioning, whitelist that entity in Entity Culling's config.",
]
(out / "OPTIMIZATION_NOTES.txt").write_text("\n".join(notes) + "\n", encoding="utf-8")

# modlist.html is informational only; manifest.json drives CurseForge import.
html = ["<ul>"]
html += [f"<li>{name}</li>" for _, _, name in ADDITIONS]
html += ["</ul>"]
(out / "ADDED_MODS.html").write_text("\n".join(html) + "\n", encoding="utf-8")

print(f"Generated manifest with {len(all_files)} entries")
