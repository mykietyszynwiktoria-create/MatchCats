const {spawn}=require('child_process');
const fs=require('fs'),path=require('path'),os=require('os');
const {request}=require('playwright');
const {run}=require('./safety.cjs');
const url='http://127.0.0.1:8085';
const dbURL=process.env.DB_URL||'';
if(!/^jdbc:postgresql:\/\/(localhost|127\.0\.0\.1):\d+\/matchcats_test$/.test(dbURL))throw Error('Safety tests require a local, dedicated matchcats_test database.');
const root=path.resolve(__dirname,'../..');
const jars=fs.readdirSync(path.join(root,'build/libs')).filter(n=>n.endsWith('-SNAPSHOT.jar')&&!n.includes('-plain'));
if(jars.length!==1)throw Error('Build exactly one executable application JAR first.');
const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin',process.platform==='win32'?'java.exe':'java'):'java';
const username='safetyMod'+Date.now().toString(36),password='TemporarySafetyPassword!';
let child,account,client;
let output='';
const log=path.join(os.tmpdir(),'matchcats-safety-server-'+Date.now()+'.log');
async function stop(){if(!child)return;const target=child;child=null;if(target.exitCode===null){target.kill();await new Promise(resolve=>target.once('exit',resolve));}}
async function start(moderatorId=''){
 child=spawn(java,['-Xms64m','-Xmx256m','-XX:MaxMetaspaceSize=192m','-jar',path.join(root,'build/libs',jars[0]),'--server.port=8085'],{windowsHide:true,env:{...process.env,SERVER_ADDRESS:'127.0.0.1',MAIL_ENABLED:'false',MODERATOR_ACCOUNT_IDS:String(moderatorId)}});
 child.on('error',e=>{output+=String(e);});
 child.stdout.on('data',b=>{output+=b;});child.stderr.on('data',b=>{output+=b;});
 for(let i=0;i<90;i++){
  if(child.exitCode!==null)throw Error('Safety server stopped: '+output.slice(-6000));
  try{const response=await fetch(url+'/health',{signal:AbortSignal.timeout(500)});if(response.ok)return;}catch{}
  await new Promise(resolve=>setTimeout(resolve,500));
 }
 throw Error('Safety server did not start: '+output.slice(-6000));
}
async function mutation(method,path,data){const token=await (await client.get(url+'/auth/csrf')).json();const response=await client[method](url+path,{headers:{[token.headerName]:token.token},data});if(!response.ok())throw Error('Test setup/cleanup failed: '+response.status()+' '+await response.text());return response;}
(async()=>{
 try{
  try{const r=await fetch(url+'/health',{signal:AbortSignal.timeout(500)});if(r.ok())throw Error('Port 8085 is already in use. Stop the test server first.');}catch(e){if(e.message.includes('already in use'))throw e;}
  await start();client=await request.newContext();
  account=await (await mutation('post','/users',{username,password,email:username+'@example.test',firstName:'Test',surname:'Moderator'})).json();
  await stop();await client.dispose();client=null;
  await start(account.id);await run({url,moderator:username,password});
 }finally{
  try{
   if(account && child){client=await request.newContext();await mutation('post','/auth/login',{username,password});await mutation('delete','/users/me',{currentPassword:password});}
  }finally{if(client)await client.dispose();await stop();fs.writeFileSync(log,output);console.log('Safety server log: '+log);}
 }
})().catch(e=>{console.error(e);process.exitCode=1;});
