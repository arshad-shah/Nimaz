"""Build a standalone browser design review from the committed resources (not native captures)."""
from pathlib import Path
import xml.etree.ElementTree as ET
import json
root=Path(__file__).resolve().parents[1]
res=root/'feature/content/src/main/res'
data={}
for locale,folder in [('en','values'),('tr','values-tr'),('id','values-in'),('ms','values-ms'),('fr','values-fr'),('de','values-de')]:
 data[locale]={e.attrib['name']:''.join(e.itertext()) for e in ET.parse(res/folder/'qaida_journey.xml').getroot()}
template=(root/'docs/specs/qaida-journey/review-template.html').read_text()
result=template.replace('__DATA__',json.dumps(data,ensure_ascii=False))
out=root/'docs/specs/qaida-journey/qaida-review.html';out.write_text(result)
print(out, out.stat().st_size)
