import express from "express";
import cors from "cors";
import multer from "multer";
import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";
import { generateAndroidSource } from "./generator.mjs";
import { ACCESS_TOKEN,MAX_UPLOAD_MB,SIGNING_ALIAS,SIGNING_PASS,projects,now,publicProject,persist,appendLog,loadExisting,ensureSigningKey,sourceDir,artifactDir,logFile,runCommand,sha256,signingFile,safePackage } from "./util.mjs";

const PORT=Number(process.env.PORT||8080);
const app=express();
const upload=multer({storage:multer.memoryStorage(),limits:{fileSize:MAX_UPLOAD_MB*1024*1024,files:1}});
const queue=[];let busy=false;\nconst buildRate=new Map();

app.disable("x-powered-by");app.set("trust proxy",1);app.use(cors());app.use(express.json({limit:"1mb"}));
app.use((req,res,next)=>{res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");res.setHeader("Referrer-Policy","no-referrer");if(ACCESS_TOKEN&&req.path!=="/api/health"){const t=(req.headers.authorization||"").replace(/^Bearer\s+/i,"");if(t!==ACCESS_TOKEN)return res.status(401).json({error:"Unauthorized"})}next()});

async function executeBuild(p){
 p.status="generating";p.progress=15;p.error=null;p.attempts=(p.attempts||0)+1;await persist(p);await appendLog(p.id,"[pipeline] ساخت پروژه شروع شد");
 await generateAndroidSource(p,p._zipBuffer||null);
 p.status="building";p.progress=45;await persist(p);await appendLog(p.id,"[builder] Gradle release build started");
 const code=await runCommand(p.id,"gradle",[":app:assembleRelease","--no-daemon","--stacktrace","--max-workers=1"],sourceDir(p.id),{
  SIGNING_STORE_FILE:signingFile,SIGNING_STORE_PASSWORD:SIGNING_PASS,SIGNING_KEY_ALIAS:SIGNING_ALIAS,
  GRADLE_OPTS:"-Dorg.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"
 });
 if(code!==0)throw new Error("Gradle build failed with exit code "+code);
 const built=path.join(sourceDir(p.id),"app/build/outputs/apk/release/app-release.apk");await fs.access(built);await fs.mkdir(artifactDir(p.id),{recursive:true});
 const dest=path.join(artifactDir(p.id),"app-release.apk");await fs.copyFile(built,dest);
 p.artifact="app-release.apk";p.artifactSha256=await sha256(dest);p.status="succeeded";p.progress=100;p.error=null;await persist(p);await appendLog(p.id,"[done] APK آماده است. SHA-256: "+p.artifactSha256);
}
async function worker(){if(busy)return;busy=true;try{while(queue.length){const id=queue.shift(),p=projects.get(id);if(!p)continue;try{await executeBuild(p)}catch(e){p.status="failed";p.progress=100;p.error=e?.message||String(e);await persist(p);await appendLog(p.id,"[error] "+p.error)}}}finally{busy=false}}
function enqueue(id){if(!queue.includes(id))queue.push(id);setImmediate(worker)}

app.get("/api/health",(_req,res)=>res.json({ok:true,aiConfigured:true,builderImage:"railway-free-cloud",toolchain:{android:"API 36",gradle:"8.13",agp:"8.13.2",java:"17",generator:"free-ai-with-offline-fallback"}}));
app.get("/api/projects",(_req,res)=>res.json([...projects.values()].sort((a,b)=>String(b.createdAt).localeCompare(String(a.createdAt))).map(publicProject)));
app.get("/api/projects/:id",(req,res)=>{const p=projects.get(req.params.id);if(!p)return res.status(404).json({error:"Not found"});res.json(publicProject(p))});
app.get("/api/projects/:id/logs",async(req,res)=>{try{res.type("text/plain; charset=utf-8").send(await fs.readFile(logFile(req.params.id),"utf8"))}catch{res.type("text/plain").send("")}});
app.get("/api/projects/:id/artifact",async(req,res)=>{const p=projects.get(req.params.id);if(!p?.artifact)return res.status(404).json({error:"Artifact not ready"});const f=path.join(artifactDir(p.id),p.artifact);res.setHeader("X-APK-SHA256",p.artifactSha256||"");res.download(f,(p.spec.appName.replace(/[^a-zA-Z0-9_-]+/g,"_").slice(0,40)||"app")+"-release.apk")});
app.post("/api/projects",upload.single("webzip"),async(req,res,next)=>{try{
 const appName=String(req.body.appName||"").trim();if(appName.length<2)throw new Error("نام برنامه کوتاه است.");
 const mode=["prompt","url","webzip"].includes(req.body.mode)?req.body.mode:"prompt",pkg=safePackage(req.body.packageName,appName);
 if(mode==="prompt"&&!String(req.body.prompt||"").trim())throw new Error("Prompt لازم است.");
 if(mode==="url"&&!/^https?:\/\//i.test(String(req.body.url||"")))throw new Error("URL نامعتبر است.");
 if(mode==="webzip"&&!req.file)throw new Error("ZIP لازم است.");
 const id=crypto.randomUUID(),p={id,ownerClientId:req.clientId,createdAt:now(),updatedAt:now(),status:"created",progress:2,spec:{appName,packageName:pkg,mode,prompt:mode==="prompt"?String(req.body.prompt||""):undefined,url:mode==="url"?String(req.body.url||""):undefined,autoBuild:String(req.body.autoBuild??"true")!=="false"},artifact:null,artifactSha256:null,error:null,attempts:0,_zipBuffer:req.file?.buffer||null};
 projects.set(id,p);await persist(p);await appendLog(id,"[project] Created "+pkg);if(p.spec.autoBuild)enqueue(id);res.status(201).json(publicProject(p));
 }catch(e){next(e)}});
app.post("/api/projects/:id/run",allowBuild,async(req,res)=>{const p=owned(req,req.params.id);if(!p)return res.status(404).json({error:"Not found"});p.status="created";p.progress=2;p.error=null;await persist(p);enqueue(p.id);res.status(202).json({ok:true})});
app.use((err,_req,res,_next)=>{console.error(err);res.status(err?.code==="LIMIT_FILE_SIZE"?413:400).json({error:err?.message||"Request failed"})});

await loadExisting();await ensureSigningKey();
app.listen(PORT,"0.0.0.0",()=>console.log("AI Android Factory free cloud listening on :"+PORT));
