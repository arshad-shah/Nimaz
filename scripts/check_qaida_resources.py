"""Validate Qaida locale parity and format placeholders, and that the feature ships no raster art."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / 'feature/content/src/main/res'

def strings(locale):
    entries = list(ET.parse(root / locale / 'qaida_journey.xml').getroot())
    result = {e.attrib['name']: ''.join(e.itertext()) for e in entries}
    assert len(entries) == len(result), f'Duplicate keys: {locale}'
    return result

base = strings('values')
for locale in ['values-tr', 'values-in', 'values-ms', 'values-fr', 'values-de']:
    translated = strings(locale)
    assert translated.keys() == base.keys(), f'Missing/extra keys: {locale}'
    for name, value in translated.items():
        assert value.strip(), f'Empty translation: {locale}/{name}'
        pattern = r'%\d+\$[ds]'
        assert sorted(re.findall(pattern, value)) == sorted(re.findall(pattern, base[name])), f'Placeholder mismatch: {locale}/{name}'
# Qaida is text- and audio-only: its illustrations were removed, so none may come back unnoticed.
rasters = sorted(p.relative_to(root) for p in root.glob('drawable*/qaida_*'))
assert not rasters, f'Qaida raster art is not shipped any more: {rasters}'
print(f'Qaida: {len(base)} keys in all six locales; placeholders valid; no raster art.')
