import fs from "node:fs/promises";
import fssync from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { spawn, spawnSync } from "node:child_process";

export const DATA_DIR=path.resolve(process.env.DATA_DIR||"/tmp/aifactory");
export const ACCESS_TOKEN=process.env.ACCESS_TOKEN||"";
export const MAX_UPLOAD_MB=Math.max(1,Math.min(50,Number(process.env.MAX_UPLOAD_MB||20)));
export const SIGNING_PASS=process.env.SIGNING_STORE_PASSWORD||"factory-free-local-only";
export const SIGNING_ALIAS=process.env.SIGNING_KEY_ALIAS||"factory";
export let signingFile="";
export const projects=new Map();
export const now=()=>new Date().toISOString();
export const projectDir=id=>path.join(DATA_DIR,"projects",id);
export const sourceDir=id=>path.join(projectDir(id),"source");
export const artifactDir=id=>path.join(projectDir(id),"artifacts");
export const logFile=id=>path.join(projectDir(id),"build.log");

export function safePackage(input,appName="app"){
  let v=String(input||"").trim();
  if(!v){const slug=String(appName).toLowerCase().replace(/[^a-z0-9]+/g,"").slice(0,24)||"generated";v="com.aifactory."+slug}
  if(!/^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*){1,}$/.test(v))throw new Error("Package Name نامعتبر است.");
  return v.toLowerCase();
}
export function publicProject(p){return{id:p.id,createdAt:p.createdAt,updatedAt:p.updatedAt,status:p.status,progress:p.progress,spec:p.spec,artifact:p.artifact||null,artifactSha256:p.artifactSha256||null,error:p.error||null,attempts:p.attempts||0}}
export async function appendLog(id,line){await fs.mkdir(projectDir(id),{recursive:true});await fs.appendFile(logFile(id),"["+new Date().toLocaleTimeString("en-GB")+"] "+String(line).slice(0,5000)+"\n","utf8")}
export async function persist(p){p.updatedAt=now();projects.set(p.id,p);await fs.mkdir(projectDir(p.id),{recursive:true});await fs.writeFile(path.join(projectDir(p.id),"project.json"),JSON.stringify(publicProject(p),null,2))}
export async function loadExisting(){await fs.mkdir(path.join(DATA_DIR,"projects"),{recursive:true});const ds=await fs.readdir(path.join(DATA_DIR,"projects"),{withFileTypes:true}).catch(()=>[]);for(const d of ds){if(!d.isDirectory())continue;try{const p=JSON.parse(await fs.readFile(path.join(DATA_DIR,"projects",d.name,"project.json"),"utf8"));projects.set(p.id,p)}catch{}}}
export async function ensureSigningKey(){
  const dir=path.join(DATA_DIR,"private");await fs.mkdir(dir,{recursive:true});signingFile=path.join(dir,"factory-release.jks");
  const b64=process.env.SIGNING_KEYSTORE_B64||"";
  if(b64&&process.env.SIGNING_STORE_PASSWORD){await fs.writeFile(signingFile,Buffer.from(b64,"base64"),{mode:0o600});return}
  const r=spawnSync("keytool",["-genkeypair","-noprompt","-keystore",signingFile,"-storepass",SIGNING_PASS,"-keypass",SIGNING_PASS,"-alias",SIGNING_ALIAS,"-keyalg","RSA","-keysize","3072","-validity","3650","-dname","CN=AI Android Factory Free Cloud"],{stdio:"ignore"});
  if(r.status!==0)throw new Error("Could not create signing key");
}
export async function writeText(file,content){await fs.mkdir(path.dirname(file),{recursive:true});await fs.writeFile(file,content,"utf8")}
export function runCommand(id,cmd,args,cwd,extra={}){
 return new Promise((resolve,reject)=>{const child=spawn(cmd,args,{cwd,env:{...process.env,...extra},stdio:["ignore","pipe","pipe"]});const pipe=d=>String(d).split(/\r?\n/).filter(Boolean).forEach(x=>appendLog(id,x));child.stdout.on("data",pipe);child.stderr.on("data",pipe);child.on("error",reject);child.on("close",c=>resolve(c??1))});
}
export async function sha256(file){const h=crypto.createHash("sha256"),s=fssync.createReadStream(file);for await(const c of s)h.update(c);return h.digest("hex")}
