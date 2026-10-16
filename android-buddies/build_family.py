#!/usr/bin/env python3
"""Build 12 independent offline debug APKs, compiling shared Java/Dex only once."""
import argparse,hashlib,json,os,shutil,subprocess,zipfile
from pathlib import Path

def run(*args):subprocess.run([str(x) for x in args],check=True)
def main():
 p=argparse.ArgumentParser();p.add_argument('--sdk',type=Path,default=Path(os.environ.get('ANDROID_HOME','android-sdk')));args=p.parse_args();sdk=args.sdk.resolve();base=Path(__file__).resolve().parent
 tools=sorted((sdk/'build-tools').glob('*/aapt2*'));assert tools,'Instale Android SDK build-tools';preferred=sdk/'build-tools/35.0.0';tool=preferred if preferred.exists() else tools[-1].parent;jar=sdk/'platforms/android-35/android.jar';assert jar.exists(),'Instale a plataforma android-35'
 suffix='.exe' if os.name=='nt' else '';build=base/'build-family';build.mkdir(exist_ok=True);out=base/'dist';out.mkdir(exist_ok=True)
 for d in ['gen','classes','dex','art-classes']:
  path=build/d
  if path.exists():shutil.rmtree(path)
  path.mkdir()
 for n in ['aapt2','zipalign']:
  if os.name!='nt' and not os.access(tool/n,os.X_OK):(tool/n).chmod(0o755)
 # Generate the launcher icons from exactly the same geometry as the animated mascots.
 run('java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',build/'art-classes',base/'app/src/main/java/com/brittech/minibuddy/BuddyArt.java',base/'tools/BuddyArtExport.java')
 run('java','-cp',build/'art-classes','BuddyArtExport',build/'art')
 key=base/'signing/debug.keystore';key.parent.mkdir(exist_ok=True)
 if not key.exists():run('keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=MiniBuddies Debug,O=Brittech,C=BR','-noprompt')
 checks=[];icon_hashes=set()
 for i,cat in enumerate(sorted((base/'catalogs').glob('*.json'))):
  c=json.loads(cat.read_text());slug=c['id'];folder=build/slug;assets=folder/'assets';assets.mkdir(parents=True,exist_ok=True)
  res=folder/'res';shutil.copytree(base/'app/src/main/res',res,dirs_exist_ok=True)
  shutil.copyfile(build/'art'/f'{slug}.xml',res/'drawable/ic_robot.xml')
  run(tool/('aapt2'+suffix),'compile','--dir',res,'-o',folder/'resources.zip')
  (assets/'buddy_config.json').write_text(json.dumps(c,ensure_ascii=False),encoding='utf-8')
  (assets/'pack_subject.json').write_text(json.dumps({'version':1,'notes':[{'title':q['skill'],'text':q['explanation']} for q in c['questions']]},ensure_ascii=False),encoding='utf-8')
  manifest=(base/'app/src/main/AndroidManifest.xml').read_text().replace('package="com.brittech.minibuddy"',f'package="com.brittech.minibuddies.{slug}"').replace('android:label="Thigas MiniBuddy"',f'android:label="{c["nickname"]} • {c["name"]}"')
  (folder/'AndroidManifest.xml').write_text(manifest,encoding='utf-8')
  run(tool/('aapt2'+suffix),'link','-I',jar,'--manifest',folder/'AndroidManifest.xml','--custom-package','com.brittech.minibuddy','--java',build/'gen','-A',assets,'--min-sdk-version','26','--target-sdk-version','35','-o',folder/'res.apk',folder/'resources.zip')
  if i==0:
   sources=list((base/'app/src/main/java').rglob('*.java'))+list((build/'gen').rglob('*.java'))
   compiler=['-encoding','UTF-8','-source','8','-target','8','-classpath',str(jar),'-d',str(build/'classes')]+[str(s) for s in sources]
   argfile=build/'javac.args';argfile.write_text('\n'.join('"'+a.replace('\\','\\\\').replace('"','\\"')+'"' for a in compiler))
   run('java','-m','jdk.compiler/com.sun.tools.javac.Main','@'+str(argfile))
   run('java','-cp',tool/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',jar,'--output',build/'dex',*list((build/'classes').rglob('*.class')))
  unsigned=folder/'unsigned.apk';shutil.copyfile(folder/'res.apk',unsigned)
  with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:
   for dex in (build/'dex').glob('*.dex'):z.write(dex,dex.name)
  run(tool/('zipalign'+suffix),'-f','4',unsigned,folder/'aligned.apk');apk=out/f'MiniBuddy_{slug}.apk'
  run('java','-jar',tool/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','pass:android','--key-pass','pass:android','--ks-key-alias','androiddebugkey','--out',apk,folder/'aligned.apk')
  run('java','-jar',tool/'lib/apksigner.jar','verify',apk)
  badging=subprocess.check_output([str(tool/('aapt2'+suffix)),'dump','badging',str(apk)],text=True);assert f"name='com.brittech.minibuddies.{slug}'" in badging;assert 'android.permission.INTERNET' not in badging
  with zipfile.ZipFile(apk) as z:
   assert json.loads(z.read('assets/buddy_config.json'))['id']==slug
   icon=hashlib.sha256(z.read('res/drawable/ic_robot.xml')).hexdigest()
   assert icon not in icon_hashes,'Ícone duplicado';icon_hashes.add(icon)
  assert "versionCode='2'" in badging and "versionName='0.2.0'" in badging
  checks.append(hashlib.sha256(apk.read_bytes()).hexdigest()+'  '+apk.name);print('OK',apk.name,flush=True)
 (out/'SHA256.txt').write_text('\n'.join(checks)+'\n');print('12 APKs verificados em',out)
if __name__=='__main__':main()
