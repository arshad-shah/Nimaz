"""Validate Qaida locale parity, format placeholders and bundled raster integrity."""
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
for asset in (root / 'drawable-nodpi').glob('qaida_*.webp'):
    data = asset.read_bytes()
    assert len(data) > 1024 and data[:4] == b'RIFF' and data[8:12] == b'WEBP', f'Invalid raster: {asset}'
print(f'Qaida: {len(base)} keys in all six locales; placeholders and raster headers valid.')
