const {chromium}=require('playwright');
const assert=require('assert/strict');
const fs=require('fs'),path=require('path');
const url=process.env.MATCHCATS_TEST_URL||'http://127.0.0.1:8084';
if(!['localhost','127.0.0.1'].includes(new URL(url).hostname))throw Error('Use an isolated local test server.');
const suffix=Date.now().toString(36), usernames=['uie2e'+suffix+'a','uie2e'+suffix+'b'];
const password='TemporaryTestPassword!';
const passwords=[password,password];
(async()=>{
 let ready=false;
 for(let attempt=0;attempt<60;attempt++){
  try{const response=await fetch(url+'/health',{signal:AbortSignal.timeout(1000)});if(response.ok){ready=true;break;}}catch{}
  await new Promise(resolve=>setTimeout(resolve,500));
 }
 if(!ready)throw Error('The local MatchCats server is not ready. Start the built application first.');
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
 const errors=[],contexts=[];
 const go=async(page,route)=>{const target=url+'/#'+route;if(page.url()===target)await page.reload();else await page.goto(target);await page.locator('.live-loading').waitFor({state:'hidden'});};
 const fill=async(page,form,values)=>{for(const [name,value] of Object.entries(values))await page.locator('#'+form+' [name="'+name+'"]').fill(value);};
 try{
  for(let i=0;i<2;i++){
   const context=await browser.newContext();contexts.push(context);const page=await context.newPage();
   page.on('pageerror',e=>errors.push(e.message));
   await go(page,'register');
   await fill(page,'register-form',{username:usernames[i],email:usernames[i]+'@example.test',firstName:i?'Bob':'Alice',surname:'Test',password,confirm:'Mismatch'});
   await page.locator('#register-form [type="submit"]').click();
   await page.getByRole('alert').filter({hasText:'Hasła nie są takie same'}).waitFor();
   await page.locator('#register-form [name="confirm"]').fill(password);
   await page.locator('#register-form [type="submit"]').click();
   await page.locator('#login-form').waitFor();
   if(i===0){
    await go(page,'register');await fill(page,'register-form',{username:usernames[i],email:usernames[i]+'@example.test',firstName:'Alice',surname:'Test',password,confirm:password});
    await page.locator('#register-form [type="submit"]').click();await page.getByRole('alert').filter({hasText:'jest już zajęty'}).waitFor();await go(page,'login');
   }
   await fill(page,'login-form',{username:usernames[i],password:'WrongPassword!'});
   if(i===0&&process.env.MATCHCATS_AUTH_SCREENSHOT)await page.screenshot({path:process.env.MATCHCATS_AUTH_SCREENSHOT,fullPage:true});
   await page.locator('#login-form [type="submit"]').click();
   await page.getByRole('alert').filter({hasText:'Nieprawidłowy login lub hasło'}).waitFor();
   await page.locator('#login-form [name="password"]').fill(password);
   await page.locator('#login-form [type="submit"]').click();
   await page.locator('#breeder-form').waitFor();
   await fill(page,'breeder-form',{kennel:'UI Test '+i,city:'Warsaw',country:'Poland'});
   await page.locator('#breeder-form [type="submit"]').click();
   await page.locator('#breeder-form [type="submit"]:enabled').waitFor();
   await go(page,'new-cat');
   await fill(page,'live-cat-form',{name:i?'Bob Cat':'Alice Cat',breed:'Maine Coon',birthDate:'2022-01-01'});
   await page.locator('#live-cat-form [name="sex"]').selectOption(i?'MALE':'FEMALE');
   await page.locator('#live-cat-form [name="health"]').selectOption('HEALTHY');
   await page.locator('#live-cat-form [name="available"]').check();
   await page.locator('#live-cat-form [type="submit"]').click();
   await page.waitForURL('**/#cat/*');
   await page.locator('.detail-box').waitFor();
  }
  const alice=contexts[0].pages()[0],bob=contexts[1].pages()[0];
  const aliceCat=alice.url().split('/').at(-1),bobCat=bob.url().split('/').at(-1);
  await alice.locator('#photo-upload [name="file"]').setInputFiles(path.join(__dirname,'../assets/sky-hero.jpg'));
  await alice.locator('#photo-upload [type="submit"]').click();await alice.locator('.live-cat-photo').waitFor();
  assert.ok(await alice.locator('.live-cat-photo').evaluate(img=>img.complete&&img.naturalWidth>0));
  let release;const blocked=new Promise(resolve=>{release=resolve;});
  await alice.route('**/cats/'+aliceCat,async route=>{await blocked;await route.continue();});
  const loading=alice.waitForRequest('**/cats/'+aliceCat);await alice.goto(url+'/#edit-cat/'+aliceCat);await loading;
  await alice.evaluate(()=>{location.hash='new-cat';});await alice.locator('#live-cat-form').waitFor();
  const oldResponse=alice.waitForResponse('**/cats/'+aliceCat);release();await oldResponse;
  await alice.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  await alice.unroute('**/cats/'+aliceCat);
  await fill(alice,'live-cat-form',{name:'Race Test Cat',breed:'Maine Coon',birthDate:'2022-01-01'});
  const created=alice.waitForResponse(r=>r.url().endsWith('/cats')&&r.request().method()==='POST'&&r.status()===201);
  await alice.locator('#live-cat-form [type=submit]').click();
  const createdCat=await (await created).json();
  await alice.waitForURL('**/#cat/'+createdCat.id);await alice.locator('.detail-box').waitFor();
  assert.equal((await alice.request.get(url+'/cats/'+aliceCat).then(r=>r.json())).name,'Alice Cat','A delayed edit response changed the new-cat form');
  await go(alice,'documents/'+aliceCat);
  await alice.locator('[name="file"]').setInputFiles(path.join(__dirname,'../assets/sky-hero.jpg'));
  await alice.locator('#document-upload [type="submit"]').click();
  await alice.locator('[data-download]').waitFor();
  await go(bob,'cat/'+aliceCat);
  assert.equal(await bob.locator('[data-download]').count(),0,'Private document leaked');
  await go(alice,'documents/'+aliceCat);
  await alice.locator('[data-share]').click();
  await alice.locator('[data-visibility="PRIVATE"]').waitFor();
  await go(bob,'cat/'+aliceCat);
  await bob.locator('[data-download]').waitFor();
  const download=await Promise.all([bob.waitForEvent('download'),bob.locator('[data-download]').click()]);
  assert.ok(download[0].suggestedFilename().endsWith('.jpg'));
  await bob.locator('[data-contact]').click();await bob.locator('#live-message-form').waitFor();
  await bob.locator('#live-message-form [name="text"]').fill('<img src=x onerror=alert(1)> Hello Alice');
  await bob.locator('#live-message-form [type="submit"]').click();
  await bob.locator('.bubble.mine').waitFor();assert.equal(await bob.locator('.bubble img').count(),0);
  await go(alice,'dashboard');await alice.locator('[data-unread-total]:not([hidden])').waitFor();
  assert.equal(await alice.locator('[data-unread-total]').innerText(),'1');
  await alice.reload();await alice.locator('[data-unread-total]:not([hidden])').waitFor();
  assert.equal((await alice.request.get(url+'/chats/unread').then(r=>r.json())).unreadMessages,1,'Read state must survive reload');
  await go(alice,'messages');await alice.locator('.bubble').filter({hasText:'Hello Alice'}).waitFor();
  await alice.locator('[data-unread-total][hidden]').waitFor({state:'attached'});
  assert.equal((await alice.request.get(url+'/chats/unread').then(r=>r.json())).unreadMessages,0);
  await go(alice,'candidates/'+aliceCat);await alice.locator('[data-pair-candidate="'+bobCat+'"]').click();
  await alice.locator('#live-message-form').waitFor();
  await go(alice,'proposals');
  const proposal=alice.locator('.proposal-card').filter({hasText:'#'+bobCat});
  await proposal.waitFor();
  const proposalId=await proposal.getAttribute('data-proposal-id');
  assert.equal(await proposal.getAttribute('data-proposal-status'),'PENDING');
  assert.equal(await proposal.locator('[data-proposal-action="ACCEPT"]').count(),0,'Sender must not approve their own request');
  bob.on('dialog',dialog=>dialog.accept());alice.on('dialog',dialog=>dialog.accept());
  await go(bob,'proposals');
  await bob.locator('[data-proposal="'+proposalId+'"][data-proposal-action="ACCEPT"]').click();
  await bob.locator('[data-proposal-id="'+proposalId+'"][data-proposal-status="ACCEPTED"]').waitFor();
  await go(alice,'proposals');
  await alice.locator('[data-proposal="'+proposalId+'"][data-proposal-action="WITHDRAW"]').click();
  await alice.locator('[data-proposal-id="'+proposalId+'"][data-proposal-status="WITHDRAWN"]').waitFor();
  await go(bob,'proposals');
  assert.equal(await bob.locator('[data-proposal="'+proposalId+'"]').count(),0,'Withdrawn proposal must not offer acceptance');
  const csrfToken=await bob.request.get(url+'/auth/csrf').then(r=>r.json());
  const secondResponse=await bob.request.post(url+'/cats',{headers:{[csrfToken.headerName]:csrfToken.token},data:{name:'Second Bob Cat',breed:'Maine Coon',sex:'MALE',health:'HEALTHY',birthDate:'2022-01-01',city:'Warsaw',country:'Poland',description:'',available:true}});
  assert.equal(secondResponse.status(),201);const secondCat=await secondResponse.json();
  await go(alice,'candidates/'+aliceCat);await alice.locator('[data-pair-candidate="'+secondCat.id+'"]').click();await alice.locator('#live-message-form').waitFor();
  await go(bob,'proposals');await bob.locator('[data-proposal-action="DECLINE"]').click();
  await bob.locator('[data-proposal-status="DECLINED"]').waitFor();
  if(process.env.MATCHCATS_PROPOSAL_SCREENSHOT){await bob.evaluate(()=>{document.activeElement?.blur();scrollTo(0,0);});await bob.screenshot({path:process.env.MATCHCATS_PROPOSAL_SCREENSHOT,fullPage:true});}
  for(const language of ['en','pl']) {
   await alice.locator('[data-language="'+language+'"]').click();
   for(const width of [320,390,768,1440]) {
    await alice.setViewportSize({width,height:1000});
    for(const route of ['dashboard','cats','search','messages','documents','settings','proposals','cat/'+aliceCat,'edit-cat/'+aliceCat,'candidates/'+aliceCat]) {
     await go(alice,route);assert.ok(await alice.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Live overflow '+route+' '+language+' '+width);
    }
   }
  }
  await alice.setViewportSize({width:1440,height:1000});
  for(const language of ['en','pl']) {
   await alice.locator('[data-language="'+language+'"]').click();
   for(const width of [320,390,768,1440]) {
    await alice.setViewportSize({width,height:1000});
    await go(alice,'this-page-does-not-exist');await alice.locator('[data-error-kind="404"] .error-cat').waitFor();
    assert.ok(await alice.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'404 overflow '+language+' '+width);
   }
  }
  await alice.setViewportSize({width:1440,height:1000});
  await go(alice,'cat/999999999');await alice.locator('[data-error-kind="404"]').waitFor();
  if(process.env.MATCHCATS_ERROR_SCREENSHOT){await alice.evaluate(()=>{document.activeElement?.blur();scrollTo(0,0);});await alice.screenshot({path:process.env.MATCHCATS_ERROR_SCREENSHOT,fullPage:true});}
  await alice.route('**/cats?*',route=>route.fulfill({status:503,contentType:'application/json',body:'{"code":"SERVICE_UNAVAILABLE"}'}));
  await go(alice,'search');await alice.locator('[data-error-kind="server"]').waitFor();
  await alice.unroute('**/cats?*');await alice.locator('[data-refresh]').click();await alice.locator('#live-search').waitFor();
  await alice.route('**/cats?*',route=>route.abort());await go(alice,'search');await alice.locator('[data-error-kind="network"] .error-cat').waitFor();
  if(process.env.MATCHCATS_NETWORK_SCREENSHOT){await alice.evaluate(()=>{document.activeElement?.blur();scrollTo(0,0);});await alice.screenshot({path:process.env.MATCHCATS_NETWORK_SCREENSHOT,fullPage:true});}
  await alice.unroute('**/cats?*');await alice.locator('[data-refresh]').click();await alice.locator('#live-search').waitFor();
  await go(alice,'cats');await alice.locator('.cat-card').filter({hasText:'Alice Cat'}).waitFor();
  assert.equal(await alice.locator('.cat-card').filter({hasText:'Bob Cat'}).count(),0);
  await go(alice,'edit-cat/'+bobCat);await alice.getByRole('alert').waitFor();
  await go(alice,'settings');await alice.locator('[data-logout]').click();await alice.locator('#login-form').waitFor();
  await fill(alice,'login-form',{username:usernames[0],password});await alice.locator('#login-form [type="submit"]').click();
  await alice.locator('#navigation a').first().waitFor();
  await go(alice,'settings');
  await alice.locator('[data-language=pl]').click();await alice.locator('#email-status').waitFor();
  await alice.locator('#email-status').getByText('Adres e-mail nie jest jeszcze potwierdzony.',{exact:true}).waitFor();
  await alice.locator('#request-email-form [type=submit]').click();
  await alice.locator('#request-email-form [role=alert]').filter({hasText:'Wysyłka e-maili nie jest jeszcze skonfigurowana'}).waitFor();
  await fill(alice,'password-form',{currentPassword:password,newPassword:'NewTemporaryPassword!',confirmPassword:'NewTemporaryPassword!'});
  const changed=alice.waitForResponse(r=>r.url().endsWith('/users/me/password')&&r.status()===204);
  await alice.locator('#password-form [type="submit"]').click();await changed;passwords[0]='NewTemporaryPassword!';
  await alice.locator('#login-form').waitFor();
  await fill(alice,'login-form',{username:usernames[0],password:passwords[0]});await alice.locator('#login-form [type="submit"]').click();await alice.locator('#navigation a').first().waitFor();
  await contexts[0].clearCookies();await alice.locator('#navigation a[href="#cats"]').click();
  await alice.locator('#login-form').waitFor();
  await alice.locator('[data-language="en"]').click();await alice.getByRole('heading',{name:'Sign in',exact:true}).waitFor();
  await go(alice,'forgot');await alice.locator('[name=email]').fill(usernames[0]+'@example.test');await alice.locator('#forgot-password-form [type=submit]').click();
  await alice.getByRole('alert').filter({hasText:'Email delivery is not configured'}).waitFor();
  await go(alice,'reset/'+'A'.repeat(43));await fill(alice,'reset-password-form',{password:'AnotherTestPassword!',confirm:'AnotherTestPassword!'});await alice.locator('#reset-password-form [type=submit]').click();
  await alice.getByRole('alert').filter({hasText:'invalid, already used or expired'}).waitFor();
  for(const width of [320,390,768,1440]){await alice.setViewportSize({width,height:1000});for(const route of ['login','register','forgot','reset/'+'A'.repeat(43)]){await go(alice,route);assert.ok(await alice.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Overflow '+route+' '+width);}}
  for(const language of ['pl','en']) {
   await alice.locator('[data-language="'+language+'"]').click();
   await go(alice,'verify/'+'A'.repeat(43));
   await alice.locator('#verify-email-form [type=submit]').click();
   await alice.getByRole('alert').filter({hasText:language==='pl'?'Link potwierdzający jest nieprawidłowy':'This verification link is invalid'}).waitFor();
   for(const width of [320,390,768,1440]) {
    await alice.setViewportSize({width,height:1000});await go(alice,'verify/'+'A'.repeat(43));
    assert.ok(await alice.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Verification overflow '+language+' '+width);
   }
  }
  await alice.setViewportSize({width:1440,height:1000});
  await alice.locator('[data-language=en]').click();
  let confirms=0;
  await alice.route('**/auth/email/confirm',route=>{confirms++;return route.fulfill({status:204});});
  await go(alice,'verify/'+'A'.repeat(43));await alice.locator('#verify-email-form').waitFor();
  assert.equal(confirms,0,'Opening a link must not confirm automatically');
  await alice.locator('#verify-email-form [type=submit]').click();
  await alice.getByRole('status').filter({hasText:'The account email this link was sent for has been verified.'}).waitFor();
  assert.equal(confirms,1);assert.equal(await alice.locator('#verify-email-form').count(),0);
  if(process.env.MATCHCATS_VERIFY_SCREENSHOT)await alice.screenshot({path:process.env.MATCHCATS_VERIFY_SCREENSHOT,fullPage:true});
  await go(alice,'verify/'+'B'.repeat(43));await alice.locator('#verify-email-form').waitFor();
  assert.equal(confirms,1,'A second link needs its own explicit confirmation');
  await alice.unroute('**/auth/email/confirm');
  await alice.route('**/auth/login',route=>route.abort());
  await go(alice,'login');await fill(alice,'login-form',{username:usernames[0],password:passwords[0]});await alice.locator('#login-form [type="submit"]').click();
  await alice.getByRole('alert').filter({hasText:'Cannot reach the server'}).waitFor();
  assert.deepEqual(errors,[]);
  console.log('PASS: real registration, duplicate accounts, password mismatch, invalid credentials, sessions, profiles, cats/photos, private/shared documents, download, conversations, escaping, proposals, ownership, password change, logout, expiry, recovery and verification errors, mocked confirmation UI, PL/EN, auth layout, network errors and temporary account deletion.');
 }catch(error){
  for(const context of contexts)for(const page of context.pages()){try{console.error('DIAGNOSTIC',page.url(),await page.locator('#main').count()?await page.locator('#main').innerText({timeout:2000}):'Page did not load');}catch(diagnosticError){console.error('DIAGNOSTIC unavailable',page.url());}}
  throw error;
 }finally{
  fs.writeFileSync(process.env.MATCHCATS_TEST_MANIFEST||path.join(require('os').tmpdir(),'matchcats-live-accounts-'+suffix+'.json'),JSON.stringify({usernames}));
  for(let i=0;i<usernames.length;i++){
   const context=await browser.newContext();
   try{
    const request=context.request;
    let token=await (await request.get(url+'/auth/csrf')).json();
    const login=await request.post(url+'/auth/login',{headers:{[token.headerName]:token.token},data:{username:usernames[i],password:passwords[i]}});
    if(login.ok()){
     token=await (await request.get(url+'/auth/csrf')).json();
     const deleted=await request.delete(url+'/users/me',{headers:{[token.headerName]:token.token},data:{currentPassword:passwords[i]}});
     assert.equal(deleted.status(),204,'Temporary account cleanup failed');
    }
   }finally{await context.close();}
  }
  await browser.close();
 }
})().catch(e=>{console.error(e);process.exitCode=1;});
