#!/usr/bin/env python3
"""Prueba humo de interfaz móvil-landscape IPTV; no necesita streams externos."""
import subprocess, time, re, struct, pathlib, xml.etree.ElementTree as ET
APP="com.epalma.tvespanolplus"
DEST=pathlib.Path("screenshots");DEST.mkdir(exist_ok=True)
def adb(*args,check=True):
 p=subprocess.run(["adb",*args],stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=False)
 if p.returncode and check: raise RuntimeError(str(args)+": "+p.stderr.decode(errors="replace"))
 return p.stdout
def hierarchy():
 for i in range(5):
  adb("shell","uiautomator","dump","/sdcard/pv.xml",check=False)
  raw=adb("shell","cat","/sdcard/pv.xml",check=False).decode(errors="replace")
  pos=raw.find("<?xml")
  if pos>=0:
   try:return ET.fromstring(raw[pos:])
   except ET.ParseError:pass
  time.sleep(1)
 raise AssertionError("UIAutomator XML no disponible")
def screenshot(name):
 png=adb("exec-out","screencap","-p")
 assert png[:8]==b"\x89PNG\r\n\x1a\n","Captura PNG inválida"
 width,height=struct.unpack(">II",png[16:24])
 assert width>height,f"Orientación incorrecta {width}x{height}"
 (DEST/(name+".png")).write_bytes(png)
 print("PASS captura horizontal",name,width,height,flush=True)
def click_nav(label):
 root=hierarchy()
 matches=[n for n in root.iter("node") if n.get("text","").strip()==label and n.get("clickable")=="true"]
 if not matches:
  raise AssertionError("Botón de menú principal ausente: "+label)
 rect=matches[-1].get("bounds","")
 m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",rect)
 assert m is not None,rect
 x1,y1,x2,y2=map(int,m.groups())
 adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
 time.sleep(2)
 root=hierarchy()
 labels=" ".join(n.get("text","") for n in root.iter("node"))
 assert "Sin anuncios" not in labels and "Ver planes" not in labels, "Publicidad/planes prohibidos"
 screenshot({"TV en vivo":"tv","Películas":"peliculas","Series":"series","Música":"musica","Mi espacio":"mi_espacio"}[label])
adb("shell","pm","clear",APP,check=False)
adb("shell","am","start","-W","-n",APP+"/.MainActivity")
time.sleep(12)
assert adb("shell","pidof",APP,check=False).strip(), "La aplicación no inició"
screenshot("inicio")
for label in ["TV en vivo","Películas","Series","Música","Mi espacio"]:
 click_nav(label)
print("QA INTERFAZ 2.5.5: 6 pantallas abrieron en horizontal",flush=True)
