#!/usr/bin/env python3
"""PalmaVision 2.5.6: capturas y navegación táctil en móvil landscape. Sin depender de streams externos."""
import subprocess,time,re,struct,pathlib,xml.etree.ElementTree as ET,hashlib,json
APP="com.epalma.tvespanolplus"
OUT=pathlib.Path("screenshots");OUT.mkdir(exist_ok=True)
results=[]
def adb(*args,check=True,timeout=45):
    p=subprocess.run(["adb",*args],capture_output=True,timeout=timeout)
    if check and p.returncode:raise RuntimeError("adb "+repr(args)+" "+p.stderr.decode(errors="replace")[:600])
    return p.stdout
def display_size():
    png=adb("exec-out","screencap","-p",timeout=45)
    if png[:8]!=b"\x89PNG\r\n\x1a\n":raise AssertionError("Screenshot invalid")
    w,h=struct.unpack(">II",png[16:24])
    return png,w,h
def shot(label):
    data,w,h=display_size()
    if w<=h:raise AssertionError("App must be landscape: %dx%d"%(w,h))
    (OUT/(label+".png")).write_bytes(data)
    results.append({"screen":label,"width":w,"height":h,"bytes":len(data),"sha":hashlib.sha256(data).hexdigest()})
    print("CAPTURE",label,w,h,len(data),flush=True)
def ui_nodes():
    for _ in range(2):
        adb("shell","uiautomator","dump","/sdcard/pv-qa.xml",check=False,timeout=60)
        raw=adb("shell","cat","/sdcard/pv-qa.xml",check=False).decode(errors="replace")
        start=raw.find("<?xml")
        if start>=0:
            try:return list(ET.fromstring(raw[start:]).iter("node"))
            except ET.ParseError:pass
        time.sleep(2)
    return []
def tap_node(n):
    m=re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.get("bounds",""))
    if not m:return False
    x1,y1,x2,y2=map(int,m.groups());adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
    return True
def dismiss_system():
    for attempt in range(5):
        nodes=ui_nodes()
        page=" ".join((n.get("text","")+" "+n.get("content-desc","")) for n in nodes)
        if "isn't responding" in page or "no responde" in page:
            shot("warning_app_unresponsive_"+str(attempt))
            print("WARNING possible app not responding during emulator startup",flush=True)
            wait=next((n for n in nodes if n.get("text","") in ("Wait","Esperar")),None)
            if wait:tap_node(wait);time.sleep(5);continue
        got=next((n for n in nodes if n.get("text","") in ("Got it","Entendido","Aceptar")),None)
        if got:tap_node(got);time.sleep(3);continue
        return
def nav(label,i):
    nodes=ui_nodes()
    match=[n for n in nodes if n.get("text","").strip()==label and n.get("clickable")=="true"]
    if match and tap_node(match[-1]):print("NAV text",label,flush=True)
    else:
        _,w,h=display_size()
        adb("shell","input","tap",str(int(w*(i+.5)/6)),str(h-55))
        print("NAV fallback",label,flush=True)
    time.sleep(3)
    shot({0:"01_tv_en_vivo",1:"02_peliculas",2:"03_series",3:"04_musica",4:"05_mi_espacio"}[i])
def touch_access(name,token,x_fraction,y_fraction):
    nodes=ui_nodes()
    match=[n for n in nodes if (token.lower() in (n.get("content-desc","")+" "+n.get("text","")).lower()) and n.get("clickable")=="true"]
    if match:tap_node(match[-1])
    else:
        _,w,h=display_size()
        adb("shell","input","tap",str(int(w*x_fraction)),str(int(h*y_fraction)))
    time.sleep(3)
    shot(name)
adb("shell","settings","put","secure","immersive_mode_confirmations","confirmed",check=False)
adb("shell","pm","clear",APP,check=False)
adb("shell","am","start","-W","-n",APP+"/.MainActivity",timeout=120)
time.sleep(12)
dismiss_system()
if not adb("shell","pidof",APP,check=False).strip():raise AssertionError("App process missing after launch")
shot("00_inicio")
for index,label in enumerate(["TV en vivo","Películas","Series","Música","Mi espacio"]):
    nav(label,index)
touch_access("06_configuracion","Configuración",.96,.07)
touch_access("07_analisis_red","Analizar internet",.90,.07)
touch_access("08_busqueda","Buscar",.84,.07)
pathlib.Path("screenshots/revision.json").write_text(json.dumps(results,indent=2))
print("SAVED",len(results),"screens; note screenshots need manual comparison with approved UI",flush=True)
