import AdmZip from "adm-zip";
import fs from "node:fs/promises";
import path from "node:path";
import { MAX_UPLOAD_MB,appendLog,sourceDir,writeText } from "./util.mjs";

const FREE_AI_URL=process.env.FREE_AI_URL||"https://text.pollinations.ai/openai";
const FREE_AI_MODEL=process.env.FREE_AI_MODEL||"qwen-coder-large";

function xml(s){return String(s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&apos;")}
function jesc(s){return String(s).replace(/\\/g,"\\\\").replace(/"/g,'\\"').replace(/\r?\n/g," ")}
function cleanHtml(html){
 let out=String(html||"").replace(/```(?:html)?/gi,"").replace(/```/g,"");
 const low=out.toLowerCase(),p=low.indexOf("<!doctype")>=0?low.indexOf("<!doctype"):low.indexOf("<html");
 if(p>0)out=out.slice(p);
 const e=out.toLowerCase().lastIndexOf("</html>");if(e>=0)out=out.slice(0,e+7);
 out=out.replace(/<script[^>]+src\s*=\s*["'][^"']+["'][^>]*><\/script>/gi,"");
 out=out.replace(/<(iframe|object|embed)[\s\S]*?<\/\1>/gi,"").replace(/<(iframe|object|embed)[^>]*\/?>/gi,"");
 return out;
}
function fallbackHtml(appName,prompt){
 const p=String(prompt||"").toLowerCase();
 const kind=/ماشین.?حساب|calculator/.test(p)?"calc":/لایتنر|فلش.?کارت|flash.?card|واژگان|لغت/.test(p)?"flash":/مطب|کلینیک|نوبت|بیمار|appointment|clinic/.test(p)?"clinic":/کارها|وظیفه|تسک|todo|task|یادداشت|note/.test(p)?"tasks":"records";
 const title=xml(appName);
 return `<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title><style>*{box-sizing:border-box}body{font-family:system-ui;margin:0;background:#0c0c10;color:#f5f1fb}header{padding:22px;background:#18151f;position:sticky;top:0}main{padding:18px;max-width:760px;margin:auto}.card{background:#1d1925;border:1px solid #332b40;border-radius:18px;padding:16px;margin:12px 0}input,textarea,button{width:100%;padding:14px;border-radius:14px;border:1px solid #443854;background:#121016;color:#fff;margin:6px 0;font-size:16px}button{background:#8755f7;border:0;font-weight:700}.row{display:grid;grid-template-columns:1fr 1fr;gap:8px}.item{padding:12px;border-bottom:1px solid #332c3e}.muted{color:#bdb5ca;font-size:13px}</style></head><body><header><h2>${title}</h2><div class="muted">نسخه رایگان آفلاین</div></header><main><div id="app"></div></main><script>const kind=${JSON.stringify(kind)},key="aif:"+location.pathname+":data";let data=JSON.parse(localStorage.getItem(key)||"[]");const $=s=>document.querySelector(s),esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));function save(){localStorage.setItem(key,JSON.stringify(data));render()}function del(i){data.splice(i,1);save()}function render(){const r=$("#app");if(kind==="calc"){r.innerHTML='<div class="card"><input id="expr" placeholder="مثلاً 12*3+4"><button onclick="calc()">محاسبه</button><div id="out" class="card">نتیجه</div></div>';return}r.innerHTML='<div class="card"><input id="title" placeholder="'+(kind==="clinic"?"نام بیمار / عنوان نوبت":kind==="flash"?"روی کارت":kind==="tasks"?"کار یا یادداشت":"عنوان")+'"><textarea id="body" rows="4" placeholder="جزئیات"></textarea><button onclick="add()">ذخیره</button></div><div class="card"><input id="q" placeholder="جستجو" oninput="draw()"><div id="list"></div></div>';draw()}function add(){const t=$("#title").value.trim(),b=$("#body").value.trim();if(!t)return;data.unshift({t,b,done:false,at:new Date().toLocaleString("fa-IR")});save()}function draw(){const q=($("#q")?.value||"").toLowerCase(),rows=data.map((x,i)=>({...x,i})).filter(x=>(x.t+" "+x.b).toLowerCase().includes(q));$("#list").innerHTML=rows.map(x=>'<div class="item"><b>'+esc(x.t)+'</b><div>'+esc(x.b)+'</div><div class="muted">'+esc(x.at)+'</div><div class="row"><button onclick="toggle('+x.i+')">'+(x.done?"انجام نشده":"انجام شد")+'</button><button onclick="del('+x.i+')">حذف</button></div></div>').join("")||'<div class="muted">هنوز موردی ثبت نشده.</div>'}function toggle(i){data[i].done=!data[i].done;save()}function calc(){try{const e=$("#expr").value;if(!/^[0-9+\\-*/(). %]+$/.test(e))throw 0;$("#out").textContent=Function("return ("+e+")")()}catch{$("#out").textContent="عبارت نامعتبر است"}}render();</script></body></html>`;
}
async function aiHtml(appName,prompt){
 const c=new AbortController(),timer=setTimeout(()=>c.abort(),140000);
 const system="Generate a complete single-file offline mobile web app embedded in Android WebView. Return ONLY HTML. Use Persian RTL if appropriate. Inline all CSS/JS. No CDN, external scripts, remote images, iframe, fetch, XMLHttpRequest, WebSocket, eval, Function constructor, service worker, analytics, trackers, credentials, or network calls. Use localStorage when useful. Make it polished and actually usable.";
 try{
  const res=await fetch(FREE_AI_URL,{method:"POST",signal:c.signal,headers:{"content-type":"application/json"},body:JSON.stringify({model:FREE_AI_MODEL,private:true,messages:[{role:"system",content:system},{role:"user",content:"App name: "+appName+"\nRequirements:\n"+prompt}]})});
  if(!res.ok)throw new Error("free AI HTTP "+res.status);
  const j=await res.json(),text=j?.choices?.[0]?.message?.content;if(!text||text.length<200)throw new Error("free AI returned no usable HTML");return cleanHtml(text);
 }finally{clearTimeout(timer)}
}
async function extractZip(buffer,outDir){
 const z=new AdmZip(buffer),es=z.getEntries();if(es.length>1200)throw new Error("ZIP بیش از حد فایل دارد.");
 let total=0,has=false;for(const e of es){if(e.isDirectory)continue;const rel=e.entryName.replace(/\\/g,"/").replace(/^\/+/, "");if(!rel||rel.split("/").includes(".."))throw new Error("ZIP path نامعتبر است.");const d=e.getData();total+=d.length;if(total>MAX_UPLOAD_MB*1024*1024*3)throw new Error("ZIP پس از استخراج بیش از حد بزرگ است.");const full=path.resolve(outDir,rel);if(!full.startsWith(path.resolve(outDir)+path.sep))throw new Error("ZIP path از محدوده خارج شد.");await fs.mkdir(path.dirname(full),{recursive:true});await fs.writeFile(full,d);if(rel.toLowerCase()==="index.html")has=true}if(!has)throw new Error("index.html در ریشه ZIP پیدا نشد.");
}

export async function generateAndroidSource(p,zipBuffer){
 const root=sourceDir(p.id);await fs.rm(root,{recursive:true,force:true});await fs.mkdir(root,{recursive:true});
 const pkg=p.spec.packageName,pkgPath=pkg.replace(/\./g,"/");let load="file:///android_asset/www/index.html",internet=false;
 if(p.spec.mode==="url"){load=p.spec.url;internet=true}
 if(p.spec.mode==="prompt"){let html;try{await appendLog(p.id,"[ai] تلاش برای موتور AI رایگان");html=await aiHtml(p.spec.appName,p.spec.prompt);await appendLog(p.id,"[ai] خروجی AI رایگان دریافت شد")}catch(e){await appendLog(p.id,"[ai] fallback داخلی فعال شد: "+e.message);html=fallbackHtml(p.spec.appName,p.spec.prompt)}await writeText(path.join(root,"app/src/main/assets/www/index.html"),html)}
 else if(p.spec.mode==="webzip")await extractZip(zipBuffer,path.join(root,"app/src/main/assets/www"));
 const settings=`pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name='GeneratedApp'\ninclude ':app'\n`;
 const rg="plugins { id 'com.android.application' version '8.13.2' apply false }\n";
 const ag=`plugins { id 'com.android.application' }\nandroid { namespace '${pkg}'; compileSdk 36\n defaultConfig { applicationId '${pkg}'; minSdk 26; targetSdk 36; versionCode 1; versionName '1.0' }\n signingConfigs { release { storeFile file(System.getenv('SIGNING_STORE_FILE')); storePassword System.getenv('SIGNING_STORE_PASSWORD'); keyAlias System.getenv('SIGNING_KEY_ALIAS'); keyPassword System.getenv('SIGNING_STORE_PASSWORD') } }\n buildTypes { release { minifyEnabled false; signingConfig signingConfigs.release } }\n}\n`;
 const main=`package ${pkg};\nimport android.app.Activity;import android.os.Bundle;import android.webkit.WebChromeClient;import android.webkit.WebView;import android.webkit.WebViewClient;public class MainActivity extends Activity{@Override public void onCreate(Bundle b){super.onCreate(b);WebView w=new WebView(this);setContentView(w);w.getSettings().setJavaScriptEnabled(true);w.getSettings().setDomStorageEnabled(true);w.getSettings().setAllowFileAccess(true);w.setWebViewClient(new WebViewClient());w.setWebChromeClient(new WebChromeClient());w.loadUrl("${jesc(load)}");}}\n`;
 const clear=p.spec.mode==="url"&&/^http:\/\//i.test(load);
 const man=`<?xml version="1.0" encoding="utf-8"?><manifest xmlns:android="http://schemas.android.com/apk/res/android">${internet?'<uses-permission android:name="android.permission.INTERNET"/>':""}<application android:theme="@style/AppTheme" android:label="${xml(p.spec.appName)}" android:usesCleartextTraffic="${clear?"true":"false"}"><activity android:name=".MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>`;
 const styles='<?xml version="1.0" encoding="utf-8"?><resources><style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar"><item name="android:fontFamily">sans</item><item name="android:colorAccent">#7C4DFF</item><item name="android:navigationBarColor">#0C0C10</item><item name="android:statusBarColor">#0C0C10</item><item name="android:windowLightStatusBar">false</item></style></resources>';
 await writeText(path.join(root,"settings.gradle"),settings);await writeText(path.join(root,"build.gradle"),rg);await writeText(path.join(root,"gradle.properties"),"org.gradle.daemon=false\norg.gradle.parallel=false\norg.gradle.workers.max=1\nandroid.useAndroidX=false\n");await writeText(path.join(root,"app/build.gradle"),ag);await writeText(path.join(root,"app/src/main/java",pkgPath,"MainActivity.java"),main);await writeText(path.join(root,"app/src/main/AndroidManifest.xml"),man);await writeText(path.join(root,"app/src/main/res/values/styles.xml"),styles);
}
