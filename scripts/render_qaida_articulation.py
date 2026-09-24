#!/usr/bin/env python3
"""Render original, deterministic teaching schematics directly to WebP (no SVG).

Geometry is deliberately schematic, not an anatomical scan or motion simulation.
See ARTICULATION.md for scope, sources and specialist review requirement.
Requires matplotlib, numpy and Pillow; rerun after changing any pose.
"""
from io import BytesIO
from pathlib import Path
import json
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.path import Path as MPath
from matplotlib.patches import PathPatch, Ellipse, FancyArrowPatch
from matplotlib.colors import to_rgba
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'feature/content/src/main/res/drawable-nodpi'
POSES = {
    'cavity': 'ا', 'bilabial': 'ب', 'dental_stop': 'تد', 'interdental': 'ثذ',
    'postalveolar_stop': 'ج', 'pharyngeal': 'حع', 'uvular_fricative': 'خغ',
    'tap': 'ر', 'sibilant': 'زس', 'postalveolar_fricative': 'ش',
    'emphatic_sibilant': 'ص', 'lateral_emphatic': 'ض', 'emphatic_stop': 'ط',
    'emphatic_interdental': 'ظ', 'labiodental': 'ف', 'uvular_stop': 'ق',
    'velar_stop': 'ك', 'lateral': 'ل', 'bilabial_nasal': 'م', 'alveolar_nasal': 'ن',
    'glottal_open': 'ه', 'labial_velar': 'و', 'palatal': 'ي', 'glottal_closed': 'ءأإؤئ',
}
INK = '#8b6260'
GOLD = '#c18816'
TEAL = '#128c83'

def path(start, curves):
    points = [start]; codes = [MPath.MOVETO]
    for curve in curves:
        points.extend(curve); codes.extend([MPath.CURVE4] * 3)
    points.append(start); codes.append(MPath.CLOSEPOLY)
    return MPath(points, codes)

def shape(ax, start, curves, light, dark, edge=INK, z=2):
    patch = PathPatch(path(start, curves), facecolor='none', edgecolor=edge, linewidth=1.7, zorder=z+0.1)
    ax.add_patch(patch)
    a,b = np.array(to_rgba(light)), np.array(to_rgba(dark))
    g = np.linspace(0,1,660)[:,None,None]
    rgb = a[None,None,:]*(1-g)+b[None,None,:]*g
    im = ax.imshow(np.repeat(rgb, 8, axis=1), extent=(0,1000,660,0), aspect='auto', zorder=z)
    im.set_clip_path(patch)

def line(ax, points, color, width=3, z=8, alpha=1):
    verts=[points[0]];codes=[MPath.MOVETO]
    for curve in points[1:]:
        verts.extend(curve);codes.extend([MPath.CURVE4]*3)
    ax.add_patch(PathPatch(MPath(verts,codes),facecolor='none',edgecolor=color,
        linewidth=width,capstyle='round',joinstyle='round',zorder=z,alpha=alpha))

def highlight(ax, x, y, width=54, height=44):
    ax.add_patch(Ellipse((x,y),width+22,height+22,facecolor=GOLD,edgecolor='none',alpha=.12,zorder=15))
    ax.add_patch(Ellipse((x,y),width,height,facecolor='none',edgecolor=GOLD,linewidth=3,zorder=16))
    ax.add_patch(Ellipse((x,y),7,7,facecolor=GOLD,edgecolor='none',zorder=17))

def arrow(ax, start, end, rad=0):
    ax.add_patch(FancyArrowPatch(start,end,connectionstyle=f'arc3,rad={rad}',
        arrowstyle='-|>',mutation_scale=17,linewidth=3,color=TEAL,zorder=12))

def tongue(ax, pose):
    # Each profile specifies the dorsal contour and the tip; the root stays continuous.
    root=(350,480) if pose.startswith('emphatic') or pose=='pharyngeal' else (395,480)
    profiles = {
      'uvular_stop': [((368,397),(380,296),(412,270)),((435,280),(498,329),(681,355))],
      'velar_stop': [((382,370),(408,267),(438,244)),((504,265),(549,332),(681,355))],
      'uvular_fricative': [((369,404),(379,315),(411,295)),((441,295),(523,335),(681,355))],
      'palatal': [((392,391),(455,260),(528,254)),((579,260),(626,325),(690,355))],
      'postalveolar_stop': [((400,368),(496,271),(578,253)),((601,252),(634,266),(670,337))],
      'postalveolar_fricative': [((400,375),(496,285),(586,271)),((612,277),(645,307),(676,347))],
      'dental_stop': [((410,356),(550,342),(651,282)),((672,254),(684,257),(694,274))],
      'emphatic_stop': [((351,365),(483,311),(633,285)),((669,253),(684,257),(694,274))],
      'interdental': [((408,355),(535,345),(645,316)),((685,303),(712,302),(732,310))],
      'emphatic_interdental': [((346,356),(497,304),(645,310)),((685,299),(712,302),(732,310))],
      'sibilant': [((412,345),(547,338),(662,351)),((683,354),(695,363),(702,374))],
      'emphatic_sibilant': [((346,349),(497,309),(651,345)),((683,355),(695,363),(702,374))],
      'lateral': [((410,371),(535,345),(617,287)),((648,248),(661,253),(674,262))],
      'alveolar_nasal': [((410,371),(535,345),(617,287)),((648,248),(661,253),(674,262))],
      'tap': [((410,376),(535,356),(608,303)),((634,273),(639,257),(652,255))],
      'pharyngeal': [((346,371),(440,321),(574,337)),((626,342),(665,351),(682,368))],
      'labial_velar': [((375,373),(411,284),(445,266)),((515,276),(583,333),(679,356))],
      'cavity': [((398,417),(447,378),(566,380)),((613,380),(655,380),(680,388))],
    }
    top=profiles.get(pose,[((395,400),(459,333),(565,334)),((615,335),(658,348),(681,363))])
    tip=top[-1][-1]
    shape(ax,root,top+[
        ((tip[0]+15,tip[1]+22),(680,420),(592,436)),
        ((526,452),(451,443),(448,514)),
        ((432,544),(root[0]+4,533),root),
    ],'#f4b9b1','#c9777e',edge='#a95c68',z=6)
    line(ax,[(root[0]+35,477),((457,427),(541,414),(612,412))],'#bd6e78',1.5,alpha=.6)

def side(ax, pose):
    nasal=pose in ('bilabial_nasal','alveolar_nasal')
    closed=pose in ('bilabial','bilabial_nasal')
    # Continuous external profile: nose, lips, chin and neck.
    shape(ax,(300,72),[
      ((405,48),(544,46),(625,82)),((668,106),(687,163),(753,197)),
      ((802,225),(786,242),(745,247)),((741,263),(753,276),(779,286)),
      ((805,295),(801,311),(777,321)),((787,343),(797,355),(773,370)),
      ((755,391),(778,422),(746,451)),((711,487),(628,493),(592,520)),
      ((555,550),(554,584),(558,616)),((474,634),(372,632),(300,615)),
      ((290,442),(289,220),(300,72)),
    ],'#fff2df','#e4bfa5',edge='#c39c88',z=1)
    # Nasal airway, separated from the oral cavity by hard and soft palate.
    shape(ax,(335,239),[
      ((330,177),(354,136),(430,126)),((527,98),(622,128),(649,186)),
      ((678,220),(722,225),(753,218)),((740,242),(696,239),(657,221)),
      ((569,194),(478,178),(414,210)),((382,224),(370,239),(335,239)),
    ],'#aa8383','#805f68',edge='#986f72',z=3)
    line(ax,[(433,157),((498,146),(563,160),(616,192))],'#d8a5a3',7,z=4)
    line(ax,[(446,184),((508,170),(559,187),(591,200))],'#d8a5a3',6,z=4)
    shape(ax,(332,265),[
      ((359,249),(385,253),(414,244)),((470,214),(571,216),(632,240)),
      ((662,251),(683,261),(698,282)),((724,303),(751,311),(779,313)),
      ((772,342),(760,372),(724,403)),((668,443),(546,463),(461,492)),
      ((427,526),(422,570),(422,613)),((390,619),(351,619),(330,611)),
      ((326,493),(326,349),(332,265)),
    ],'#76545f','#523e4e',edge='#805563',z=3)
    # Hard palate / alveolar ridge; ivory band above the tongue.
    shape(ax,(414,220),[
      ((483,196),(584,205),(646,234)),((670,244),(683,255),(690,269)),
      ((677,270),(659,256),(639,251)),((571,225),(484,219),(421,244)),
      ((414,237),(410,229),(414,220)),
    ],'#fffdf4','#e0c9ae',edge='#c6a58e',z=5)
    # Soft palate raised to close the nasal route for oral sounds; lowered for nasals.
    if nasal:
        shape(ax,(421,225),[
          ((398,235),(387,260),(404,287)),((415,305),(420,298),(417,282)),
          ((409,260),(424,250),(432,241)),((430,232),(428,226),(421,225)),
        ],'#efb3a7','#c08080',z=5)
    else:
        shape(ax,(421,225),[
          ((395,229),(361,235),(334,239)),((330,248),(335,260),(351,258)),
          ((370,256),(390,262),(393,282)),((399,298),(410,294),(407,278)),
          ((404,258),(423,249),(430,238)),((427,231),(425,228),(421,225)),
        ],'#efb3a7','#c08080',z=5)
    # Upper and lower incisors in section, not a row of duplicated sagittal teeth.
    shape(ax,(685,253),[
      ((696,252),(708,261),(709,273)),((711,288),(711,298),(705,305)),
      ((697,308),(687,301),(686,291)),((682,275),(680,262),(685,253)),
    ],'#ffffff','#e9ddcb',edge='#bbac98',z=7)
    shape(ax,(701,376),[
      ((710,372),(719,379),(720,390)),((723,406),(716,420),(708,423)),
      ((699,419),(693,407),(695,397)),((696,387),(695,380),(701,376)),
    ],'#ffffff','#e4d5bf',edge='#bbac98',z=7)
    tongue(ax,pose)
    # Epiglottis and glottal region are separated, below the tongue root.
    shape(ax,(418,527),[
      ((420,505),(432,493),(438,485)),((445,505),(436,520),(431,539)),
      ((429,554),(422,549),(418,527)),
    ],'#ebb0a2','#b87980',z=8)
    line(ax,[(330,563),((340,558),(349,558),(356,564))],'#e4b4a6',7)
    line(ax,[(420,563),((402,558),(393,558),(382,564))],'#e4b4a6',7)
    # Lips are independently posed; upper teeth touch the lower lip for fa.
    lower_top = 313 if closed else (300 if pose=='labiodental' else (330 if pose=='labial_velar' else 354))
    shape(ax,(749,281),[
      ((770,280),(790,286),(798,299)),((799,307),(788,314),(775,316)),
      ((763,309),(751,304),(742,298)),((740,290),(741,284),(749,281)),
    ],'#e9a299','#bc6c78',z=9)
    shape(ax,(704 if pose=='labiodental' else 754,lower_top),[
      ((730 if pose=='labiodental' else 773,lower_top-4),(791,lower_top+3),(797,lower_top+13)),
      ((791,lower_top+30),(760,lower_top+30),(744,lower_top+22)),
      ((736,lower_top+15),(736,lower_top+7),(704 if pose=='labiodental' else 754,lower_top)),
    ],'#efb0a5','#bf727b',z=10)
    points={
      'cavity':(536,290,126,60),'glottal_open':(369,562,62,42),'glottal_closed':(369,562,62,42),
      'pharyngeal':(345,454,40,82),'uvular_fricative':(403,287,52,48),
      'uvular_stop':(412,269,50,42),'velar_stop':(438,243,54,38),
      'palatal':(528,249,78,35),'postalveolar_stop':(580,248,52,35),
      'postalveolar_fricative':(590,260,52,40),'dental_stop':(683,271,47,39),
      'emphatic_stop':(683,271,47,39),'interdental':(714,309,47,37),
      'emphatic_interdental':(714,309,47,37),'sibilant':(696,366,43,43),
      'emphatic_sibilant':(696,366,43,43),'lateral':(661,258,43,36),
      'alveolar_nasal':(661,258,43,36),'tap':(648,256,43,36),
      'bilabial':(782,314,45,38),'bilabial_nasal':(782,314,45,38),
      'labiodental':(710,304,45,40),'labial_velar':(445,257,54,40),
    }
    if pose=='glottal_closed':
        line(ax,[(355,563),((363,563),(373,563),(384,563))],'#efbaad',6,z=13)
    if pose in points: highlight(ax,*points[pose])
    if pose.startswith('emphatic'): highlight(ax,345,444,38,76)
    if nasal:
        arrow(ax,(355,420),(357,272),-.07)
        arrow(ax,(357,205),(591,179),-.25)
        arrow(ax,(592,181),(742,223),-.10)
    if pose=='cavity':
        arrow(ax,(370,505),(375,308),-.15)
        arrow(ax,(435,299),(758,332),.08)
    if pose=='labial_velar':
        # Front inset makes the rounded aperture visible; sagittal view alone cannot.
        lips(ax,center=(170,240),scale=.7,rounded=True)

def lips(ax,center=(500,330),scale=1,rounded=False):
    # Use a separate inset axes for a clean, readable frontal view.
    x,y=center
    if rounded:
        ax.add_patch(Ellipse((x,y),190*scale,235*scale,facecolor='#dca293',edgecolor=INK,linewidth=1.7,zorder=10))
        ax.add_patch(Ellipse((x,y),106*scale,155*scale,facecolor='#624655',edgecolor='#b67375',linewidth=7*scale,zorder=11))
        highlight(ax,x,y,136*scale,186*scale)

def lateral_top(ax):
    # Superior schematic of tongue alongside the upper dental arch.
    shape(ax,(310,537),[
      ((231,408),(263,189),(387,108)),((449,65),(551,65),(613,108)),
      ((737,189),(769,408),(690,537)),((589,590),(411,590),(310,537)),
    ],'#f6d1bb','#d9a69c',edge='#bc8d84',z=2)
    ax.add_patch(Ellipse((500,343),355,457,facecolor='#765361',edgecolor='none',zorder=3))
    for side_sign in (-1,1):
        for i in range(6):
            y=199+i*52; x=500+side_sign*(111+40*np.sin(i/6*np.pi))
            ax.add_patch(Ellipse((x,y),43,55,angle=side_sign*(-35+i*7),
                facecolor='#fff7e6',edgecolor='#bdac96',linewidth=1.6,zorder=4))
    for x in (449,483,517,551):
        ax.add_patch(Ellipse((x,140+abs(x-500)*.23),32,49,angle=(x-500)*-.6,
            facecolor='#fff9ed',edgecolor='#bdac96',linewidth=1.6,zorder=4))
    shape(ax,(394,525),[
      ((361,446),(346,331),(379,235)),((403,183),(453,166),(497,178)),
      ((562,183),(604,236),(620,335)),((643,409),(626,483),(602,526)),
      ((542,549),(450,549),(394,525)),
    ],'#f4b4aa','#c87b84',edge='#aa6876',z=6)
    line(ax,[(500,228),((503,308),(503,410),(502,472))],'#b46e7d',1.8)
    # One side shown; the learner may use either side as guided by their teacher.
    highlight(ax,366,350,50,150)

def render(name):
    fig=plt.figure(figsize=(10,6.6),dpi=120,facecolor='#fbf8f2')
    ax=fig.add_axes([0,0,1,1]);ax.set_xlim(100,850);ax.set_ylim(660,0);ax.set_aspect('equal');ax.axis('off')
    if name=='lateral_emphatic': lateral_top(ax)
    else: side(ax,name)
    buf=BytesIO();fig.savefig(buf,format='png',dpi=120,facecolor='#fbf8f2');plt.close(fig)
    im=Image.open(buf).convert('RGB');out=BytesIO();im.save(out,format='WEBP',quality=94,method=6)
    dest=OUT/f'qaida_pose_{name}.webp';dest.write_bytes(out.getvalue());return dest

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    files=[render(name) for name in POSES]
    manifest={'renderer':'scripts/render_qaida_articulation.py (Matplotlib Agg; original raster schematics)',
      'review_status':'Specialist tajweed/anatomy review required before release; schematic positions, not a motion simulation.',
      'poses':[{'name':n,'glyphs':g,'path':f'feature/content/src/main/res/drawable-nodpi/qaida_pose_{n}.webp'} for n,g in POSES.items()]}
    (ROOT/'docs/specs/qaida-journey/ARTICULATION_ASSETS.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
    print(f'Rendered {len(files)} raster articulation schematics')
