"""Promote a complete fresh Roborazzi run into the Qaida preview documents."""
import argparse, hashlib, json, shutil, struct
from pathlib import Path

FLOWS = ['journey','chapters','review','review-empty','audio','audio-empty','clear-audio','reset','settings','intro','focus','repeat','practise','self-check','all-cards','due-review','reward','letters','letter-detail','loading','downloading','audio-unavailable','audio-error','playback-error','lesson-empty']
LOCALES = {'en':'English','tr':'Türkçe','id':'Bahasa Indonesia','ms':'Bahasa Melayu','fr':'Français','de':'Deutsch'}
ROOT = Path(__file__).resolve().parents[1]

def filename(flow, locale='en', dark=False):
    return flow + ('' if locale == 'en' else '-'+locale) + ('-dark' if dark else '') + '.png'

def publish(source, source_sha):
    names = [filename(f,l,d) for f in FLOWS for l in LOCALES for d in [False,True]]
    names += [filename(f'articulation-{i:02}',dark=d) for i in range(1,30) for d in [False,True]]
    records = []
    # Validate the complete incoming set before touching any existing document or screenshot.
    for name in names:
        p = source/name
        data = p.read_bytes()
        if data[:8] != b'\x89PNG\r\n\x1a\n' or len(data) < 1024:
            raise ValueError(f'Invalid capture: {p}')
        width,height = struct.unpack('>II',data[16:24])
        if width < 300 or height < 500:
            raise ValueError(f'Unexpected viewport: {p}: {width}×{height}')
        records.append({'file':name,'sha256':hashlib.sha256(data).hexdigest(),'width':width,'height':height})
    doc = ROOT/'docs/specs/qaida-journey'
    target = doc/'previews';target.mkdir(exist_ok=True)
    for name in names: shutil.copy2(source/name,target/name)
    header = f'# Qaida screen previews\n\nNative Compose screens captured with Roborazzi on Robolectric.\nSource commit: `{source_sha}`. 358 captures: 25 flows in six languages and both themes, plus 29 letter details in both themes.\n\nProgress and audio availability are test fixtures; these are actual app layouts, not browser reconstructions.\n\n'
    for locale,label in LOCALES.items():
        header += f'## {label}\n\n| Flow | Light | Dark |\n| --- | --- | --- |\n'
        for flow in FLOWS:
            header += f'| {flow.replace("-"," ").title()} | ![Light](previews/{filename(flow,locale)}) | ![Dark](previews/{filename(flow,locale,True)}) |\n'
        header += '\n'
    header += '## Regeneration\n\nRun `bash scripts/record_qaida_previews.sh` with JDK 21 and the project Android SDK available. Only a complete successful capture run replaces these documents.\n\n[All 29 letter details](ARTICULATION.md)\n'
    (doc/'PREVIEWS.md').write_text(header)
    anatomy = (doc/'ARTICULATION.md').read_text().split('## Historical native captures')[0].split('## Roborazzi captures')[0]
    anatomy += f'## Roborazzi captures\n\nSource commit: `{source_sha}`.\n\n| Letter | Light | Dark |\n| --- | --- | --- |\n'
    for i,glyph in enumerate('ابتثجحخدذرزسشصضطظعغفقكلمنهويء',1):
        f=f'articulation-{i:02}'
        anatomy += f'| {glyph} | ![Light](previews/{filename(f)}) | ![Dark](previews/{filename(f,dark=True)}) |\n'
    (doc/'ARTICULATION.md').write_text(anatomy)
    (doc/'previews/capture-manifest.json').write_text(json.dumps({'renderer':'Roborazzi 1.50.0 / Robolectric','source_sha':source_sha,'captures':records},indent=2)+'\n')
    print(f'Updated both preview documents with {len(names)} native captures.')

if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('source',type=Path);parser.add_argument('--source-sha',required=True)
    args=parser.parse_args();publish(args.source,args.source_sha)
