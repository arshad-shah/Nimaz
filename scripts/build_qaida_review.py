"""Build a standalone browser design review from the committed resources (not native captures)."""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image
from io import BytesIO
import json,base64
root=Path(__file__).resolve().parents[1]
res=root/'feature/content/src/main/res'
data={}
for locale,folder in [('en','values'),('tr','values-tr'),('id','values-in'),('ms','values-ms'),('fr','values-fr'),('de','values-de')]:
 data[locale]={e.attrib['name']:''.join(e.itertext()) for e in ET.parse(res/folder/'qaida_journey.xml').getroot()}
art={}
for p in (res/'drawable-nodpi').glob('qaida_*'):
 im=Image.open(p);im.load();im.thumbnail((768,512));buf=BytesIO();im.save(buf,format='WEBP',quality=82)
 art[p.stem.removeprefix('qaida_')]='data:image/webp;base64,'+base64.b64encode(buf.getvalue()).decode()
template=(root/'docs/specs/qaida-journey/review-template.html').read_text()
result=template.replace('__DATA__',json.dumps(data,ensure_ascii=False)).replace('__ART__',json.dumps(art))
out=root/'docs/specs/qaida-journey/qaida-review.html';out.write_text(result)
print(out, out.stat().st_size)
