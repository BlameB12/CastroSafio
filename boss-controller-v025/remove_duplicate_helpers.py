from pathlib import Path
import sys

root = Path(sys.argv[1])
p = root / 'src/main/java/com/castrosafio/bosscontroller/control/CataclysmBossMovementBridge.java'
s = p.read_text()

def method_spans(src: str, signature: str):
    spans = []
    search_from = 0
    while True:
        start = src.find(signature, search_from)
        if start < 0:
            break
        brace = src.find('{', start)
        depth = 0
        end = None
        for i in range(brace, len(src)):
            ch = src[i]
            if ch == '{': depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    end = i + 1
                    break
        if end is None:
            raise RuntimeError('Unclosed method: ' + signature)
        spans.append((start, end))
        search_from = end
    return spans

# v0.2.3 already contributes these two helpers. v0.2.5's full method replacement also
# included them, so remove only the later duplicate occurrence while preserving one copy.
for signature in [
    '    private static void maledictusChargeImpulse(LivingEntity boss, LivingEntity target)',
    '    private static void maledictusBackstep(LivingEntity boss)'
]:
    spans = method_spans(s, signature)
    if len(spans) < 2:
        continue
    # Remove duplicates from the end backwards so offsets remain valid.
    for start, end in reversed(spans[1:]):
        # Consume trailing blank lines too.
        while end < len(s) and s[end] == '\n':
            end += 1
            if end < len(s) and s[end] != '\n':
                break
        s = s[:start] + s[end:]

p.write_text(s)
print('Removed duplicate Maledictus movement helpers')
