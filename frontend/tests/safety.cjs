const {chromium}=require('playwright');
const assert=require('assert/strict');
const fs=require('fs'),path=require('path');
async function run({url,moderator,password}){
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
 const suffix=Date.now().toString(36),users=['safety'+suffix+'a','safety'+suffix+'b'];
 const contexts=[],errors=[];
 const go=async(page,route)=>{const target=url+'/#'+route;if(page.url()===target)await page.reload();else await page.goto(target);await page.locator('.live-loading').waitFor({state:'hidden'});};
 const fill=async(page,form,data)=>{for(const [name,value] of Object.entries(data))await page.locator(form+' [name="'+name+'"]').fill(value);};
 const change=async(context,method,route,data)=>{const token=await(await context.request.get(url+'/auth/csrf')).json();return context.request[method](url+route,{headers:{[token.headerName]:token.token},data});};
 try{
  for(const name of users){
   const context=await browser.newContext();contexts.push(context);const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept());
   assert.equal((await change(context,'post','/users',{username:name,password,email:name+'@example.test',firstName:'Test',surname:'Breeder'})).status(),201);
   await go(page,'login');await fill(page,'#login-form',{username:name,password});await page.locator('#login-form [type=submit]').click();await page.locator('#breeder-form').waitFor();
   await fill(page,'#breeder-form',{kennel:'Safety Test '+name.slice(-1),city:'Warsaw',country:'Poland'});await page.locator('#breeder-form [type=submit]').click();await page.locator('#breeder-form [type=submit]:enabled').waitFor();
  }
  const alice=contexts[0].pages()[0],bob=contexts[1].pages()[0];
  const cat=(await(await change(contexts[1],'post','/cats',{name:'Safety Test Cat',breed:'Maine Coon',sex:'MALE',health:'HEALTHY',birthDate:'2022-01-01',city:'Warsaw',country:'Poland',description:'<img src=x onerror=alert(1)> Reported profile',available:true})).json()).id;
  await go(alice,'cat/'+cat);await alice.locator('[data-contact]').click();await alice.locator('#live-message-form').waitFor();const chat=alice.url().split('/').at(-1);
  assert.equal((await change(contexts[1],'post','/chats/'+chat+'/messages',{text:'<img src=x onerror=alert(1)> Test abusive message'})).status(),201);
  await alice.locator('[data-refresh]').click();await alice.locator('.bubble a[href^="#report/MESSAGE:"]').click();
  await alice.locator('#safety-report-form').waitFor();await alice.locator('[name=details]').fill('Please review the abusive message in this test.');await alice.locator('#safety-report-form [type=submit]').click();
  await alice.locator('.safety-report').waitFor();assert.equal(await alice.locator('.safety-report img').count(),0);
  await go(alice,'messages/'+chat);await alice.locator('[data-block-owner]').click();await alice.locator('[data-unblock-owner]').waitFor();assert.equal(await alice.locator('#live-message-form [name=text]').isDisabled(),true);
  await go(bob,'messages/'+chat);assert.equal(await bob.locator('#live-message-form [name=text]').isDisabled(),true);
  await go(alice,'safety');await alice.locator('[data-unblock-owner]').click();await alice.locator('[data-unblock-owner]').waitFor({state:'hidden'});
  await go(bob,'messages/'+chat);assert.equal(await bob.locator('#live-message-form [name=text]').isEnabled(),true);
  await go(alice,'moderation');await alice.getByRole('alert').waitFor();assert.equal(await alice.locator('.safety-evidence').count(),0);
  const adminContext=await browser.newContext();contexts.push(adminContext);const admin=await adminContext.newPage();admin.on('dialog',d=>d.accept());admin.on('pageerror',e=>errors.push(e.message));
  await go(admin,'login');await fill(admin,'#login-form',{username:moderator,password});await admin.locator('#login-form [type=submit]').click();await admin.locator('#breeder-form').waitFor();
  const csrf=await(await contexts[1].request.get(url+'/auth/csrf')).json();
  const uploaded=await contexts[1].request.post(url+'/cats/'+cat+'/documents',{headers:{[csrf.headerName]:csrf.token},multipart:{kind:'PEDIGREE',file:{name:'review-test.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.7\nReview signature fixture')}}});
  assert.equal(uploaded.status(),201);const document=(await uploaded.json()).id;
  await go(bob,'cat/'+cat);let requested=bob.waitForResponse(r=>r.url().endsWith('/documents/'+document+'/verification-request')&&r.request().method()==='POST');await bob.locator('[data-request-verification="'+document+'"]').click();assert.equal((await requested).status(),200);
  await bob.locator('[data-request-verification="'+document+'"]:disabled').waitFor();
  await go(admin,'moderation/documents');await admin.locator('.document-review-decision').waitFor();
  for(const language of ['pl','en']){
   await admin.locator('[data-language="'+language+'"]').click();await admin.locator('.document-review-decision').waitFor();
   for(const width of [320,390,768,1440]){
    await admin.setViewportSize({width,height:1000});
    assert.equal(await admin.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'Document review overflow '+width);
   }
  }
  const review=admin.locator('.document-review-decision');await review.locator('[name=note]').fill('Please upload a readable scan.');await review.locator('[type=submit]').click();await review.waitFor({state:'hidden'});
  await go(bob,'cat/'+cat);assert.match(await bob.locator('.live-file-row').innerText(),/Please upload a readable scan/);
  requested=bob.waitForResponse(r=>r.url().endsWith('/documents/'+document+'/verification-request')&&r.request().method()==='POST');await bob.locator('[data-request-verification="'+document+'"]').click();assert.equal((await requested).status(),200);await bob.locator('[data-request-verification="'+document+'"]:disabled').waitFor();
  await go(admin,'moderation/documents');await review.waitFor();await review.locator('[name=status]').selectOption('VERIFIED');await review.locator('[name=note]').fill('Checked with the issuing registry in this test.');await review.locator('[type=submit]').click();await review.waitFor({state:'hidden'});
  await go(bob,'cat/'+cat);assert.match(await bob.locator('.live-file-row').innerText(),/Checked with the issuing registry/);assert.equal(await bob.locator('[data-request-verification="'+document+'"]').isDisabled(),true);
  console.log('PASS: document owner request, moderator rejection, owner explanation, resubmission, approval, PL/EN and responsive review screens.');
  await go(admin,'moderation');await admin.locator('.moderation-decision').first().waitFor();assert.equal(await admin.locator('.safety-evidence img').count(),0);
  if(process.env.MATCHCATS_SAFETY_SCREENSHOTS){fs.mkdirSync(process.env.MATCHCATS_SAFETY_SCREENSHOTS,{recursive:true});await admin.screenshot({path:path.join(process.env.MATCHCATS_SAFETY_SCREENSHOTS,'MatchCats-panel-moderatora.png'),fullPage:true});}
  for(const language of ['pl','en']){
   await admin.locator('[data-language="'+language+'"]').click();await alice.locator('[data-language="'+language+'"]').click();
   for(const width of [320,390,768,1440]){
    await admin.setViewportSize({width,height:1000});await alice.setViewportSize({width,height:1000});
    for(const route of ['safety','report/CAT:'+cat]){await go(alice,route);assert.ok(await alice.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Safety overflow '+route+' '+width);}
    await go(admin,'moderation');assert.ok(await admin.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Moderator overflow '+width);
   }
  }
  await admin.setViewportSize({width:1440,height:1000});await admin.locator('[data-language=en]').click();await go(admin,'moderation');
  const decision=admin.locator('.moderation-decision').first();await decision.locator('[name=decision]').selectOption('RESOLVED:SUSPEND_ACCOUNT');await decision.locator('[name=note]').fill('Repeated harassment confirmed in isolated test.');await decision.locator('[type=submit]').click();await admin.locator('.moderation-reinstate').waitFor();
  await go(bob,'cats');await bob.locator('#login-form').waitFor();
  await go(alice,'cat/'+cat);await alice.getByRole('alert').waitFor();
  await admin.locator('.moderation-reinstate [name=note]').fill('Test appeal accepted.');await admin.locator('.moderation-reinstate [type=submit]').click();await admin.locator('.moderation-reinstate').waitFor({state:'hidden'});
  await go(bob,'login');await fill(bob,'#login-form',{username:users[1],password});await bob.locator('#login-form [type=submit]').click();await bob.locator('#navigation a').first().waitFor();
  await go(alice,'cat/'+cat);await alice.locator('.detail-box').waitFor();
  assert.deepEqual(errors,[]);console.log('PASS: real report/privacy, bidirectional blocks, unblock, moderator access, escaped evidence, suspension/session revocation, reinstatement, PL/EN and responsive safety screens.');
 }finally{
  try{
   for(let i=0;i<users.length;i++)if(contexts[i]){await change(contexts[i],'post','/auth/login',{username:users[i],password});const response=await change(contexts[i],'delete','/users/me',{currentPassword:password});if(response.status()!==204)console.error('Temporary test account cleanup status:',response.status());}
  }finally{await browser.close();}
 }
}
module.exports={run};
