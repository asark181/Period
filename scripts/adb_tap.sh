#!/bin/bash
# tap.sh "<text>" : taps the first UI node whose text/content-desc equals the text
export PATH=~/Library/Android/sdk/platform-tools:$PATH
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml > ${TMPDIR:-/tmp}/ui_nuanyue.xml
python3 - "$1" <<'PY'
import re,sys,subprocess
t=sys.argv[1]; x=open('${TMPDIR:-/tmp}/ui_nuanyue.xml').read()
for m in re.finditer(r'<node [^>]*>', x):
    n=m.group(0)
    tx=re.search(r' text="([^"]*)"',n).group(1); cd=re.search(r'content-desc="([^"]*)"',n).group(1)
    if t in (tx,cd):
        a,b,c,d=map(int,re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',n).groups())
        subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)]); print('tapped',t); break
else: print('NOT FOUND',t)
PY
