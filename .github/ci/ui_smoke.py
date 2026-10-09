#!/usr/bin/env python3
import subprocess, time, re, sys, xml.etree.ElementTree as ET

APP="com.epalma.tvespanolplus"
ACT=f"{APP}/.MainActivity"

def sh(*args, check=True):
    p=subprocess.run(list(args),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    if check and p.returncode:
        print(p.stdout)
        raise RuntimeError("command failed: "+" ".join(args))
    return p.stdout

def adb(*args, check=True):
    return sh("adb",*args,check=check)

def dump():
    adb("shell","uiautomator","dump","/sdcard/pv-ui.xml")
    adb("pull","/sdcard/pv-ui.xml","pv-ui.xml")
    return ET.parse("pv-ui.xml").getroot()

def text_nodes(needle):
    root=dump()
    out=[]
    for n in root.iter("node"):
        txt=n.attrib.get("text","")
        desc=n.attrib.get("content-desc","")
        if needle.lower() in (txt+" "+desc).lower():
            out.append(n)
    return out

def wait_text(needle, timeout=12):
    end=time.time()+timeout
    while time.time()<end:
        nodes=text_nodes(needle)
        if nodes:
            print("OK text:",needle)
            return nodes
        time.sleep(.8)
    raise AssertionError("Missing text: "+needle)

def assert_no_text(needle):
    if text_nodes(needle):
        raise AssertionError("Unexpected visible text: "+needle)
    print("OK hidden:",needle)

def center(bounds):
    m=re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",bounds or "")
    if not m: raise AssertionError("No bounds")
    x1,y1,x2,y2=map(int,m.groups())
    return (x1+x2)//2,(y1+y2)//2

def tap_text(needle, index=0, timeout=12):
    nodes=wait_text(needle,timeout)
    if index>=len(nodes):
        raise AssertionError(f"Need occurrence {index} of {needle}; only {len(nodes)}")
    x,y=center(nodes[index].attrib.get("bounds"))
    adb("shell","input","tap",str(x),str(y))
    time.sleep(1.2)
    print("TAP",needle,index,x,y)

def edit_text(index, value):
    root=dump()
    edits=[n for n in root.iter("node") if "EditText" in n.attrib.get("class","")]
    if index>=len(edits): raise AssertionError(f"Missing EditText {index}; found {len(edits)}")
    x,y=center(edits[index].attrib.get("bounds"))
    adb("shell","input","tap",str(x),str(y))
    adb("shell","input","text",value)
    time.sleep(.4)

def back():
    adb("shell","input","keyevent","4")
    time.sleep(1)

def resumed():
    return adb("shell","dumpsys","activity","activities")

# clean launch
adb("shell","pm","clear",APP,check=False)
adb("shell","am","start","-n",ACT)
time.sleep(10)
wait_text("Canales en vivo")
wait_text("Vista Doble")
wait_text("Enviar a TV")
wait_text("Salir")

# Vista Doble -> choose Canal 1 -> full screen must show only one channel
tap_text("Vista Doble")
wait_text("Vista Doble")
pcs=wait_text("Pantalla completa")
if len(pcs)<2: raise AssertionError("Vista Doble must expose two Pantalla completa buttons")
tap_text("Canal 1")
wait_text("Seleccionar Canal 1")
tap_text("Red Bull TV",0,15)
wait_text("Vista Doble")
wait_text("Red Bull TV")
tap_text("Pantalla completa",0)
wait_text("Volver")
wait_text("Red Bull TV")
assert_no_text("Canal 2 ·")
tap_text("Volver")
wait_text("Vista Doble")
if len(wait_text("Pantalla completa"))<2: raise AssertionError("Dual view not restored after fullscreen")

# Parental: create PIN, open multi-select, select and persist one channel
back()
wait_text("Configuración")
tap_text("Configuración")
wait_text("Control parental")
tap_text("Control parental")
wait_text("Seleccionar canales a bloquear")
tap_text("Seleccionar canales a bloquear")
wait_text("Crear PIN parental")
edit_text(0,"1234")
edit_text(1,"1234")
back()  # hide keyboard
tap_text("Guardar")
wait_text("Seleccionar canales")
wait_text("Marcar visibles")
wait_text("Desmarcar visibles")
tap_text("Red Bull TV",0,15)
tap_text("Guardar bloqueos")
wait_text("Control parental")
wait_text("1 canales bloqueados")

# Cast: screen must be functional, chooser must actually open
back()
wait_text("Enviar a TV")
tap_text("Enviar a TV")
wait_text("Buscar dispositivos Google Cast")
wait_text("Conectar / cambiar Wi")
wait_text("Duplicar pantalla / Miracast")
tap_text("Buscar dispositivos Google Cast")
wait_text("Dispositivos Google Cast")
back()
wait_text("Enviar a TV")

# Wi-Fi network chooser must launch Android Settings/Panel
tap_text("Conectar / cambiar Wi")
time.sleep(2)
state=resumed()
if "com.android.settings" not in state:
    print(state[-5000:])
    raise AssertionError("Wi-Fi system selector did not open")
print("OK Wi-Fi selector opened")

back()
time.sleep(1)
adb("logcat","-d","-t","2500")
print("PALMAVISION UI SMOKE: PASS")
