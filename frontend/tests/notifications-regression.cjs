const {chromium}=require('playwright');
const assert=require('assert/strict');
const fs=require('fs'),path=require('path'),http=require('http');

(async()=>{
 const root=path.resolve(__dirname,'..');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,new URL(req.url,'http://localhost').pathname.slice(1)||'index.html');
  if(!file.startsWith(root+path.sep)||!fs.existsSync(file)||!fs.statSync(file).isFile()){res.writeHead(404);res.end();return;}
  const mime={'.html':'text/html','.css':'text/css','.js':'application/javascript','.jpg':'image/jpeg'};
  res.setHeader('Content-Type',mime[path.extname(file)]||'application/octet-stream');res.end(fs.readFileSync(file));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 let countFails=false;
 try{
  browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
  const page=await browser.newPage();const errors=[];let read=false,failRead=true,empty=false;
  page.on('pageerror',error=>errors.push(error.message));
  await page.route('**/*',async route=>{
   const url=new URL(route.request().url());let body;
   if(url.pathname==='/users/me')body={id:1,username:'notifications',firstName:'Test',surname:'Breeder',email:'test@example.test'};
   else if(url.pathname==='/owners/me')body={id:1,kennel:'Test cattery',city:'Test',country:'PL',bio:''};
   else if(url.pathname==='/safety/capabilities')body={moderator:false,moderationConfigured:false};
   else if(url.pathname==='/chats/unread')body={unreadMessages:0,unreadConversations:0};
   else if(url.pathname==='/auth/csrf')body={headerName:'X-CSRF-TOKEN',token:'test'};
   else if(url.pathname==='/notifications'){
    assert.equal(url.searchParams.get('limit'),'50');
    body=empty?[]:[{id:1,kind:'MESSAGE',title:'New message',body:'You received a new MatchCats message.',createdAt:'2026-10-04T10:00:00Z',readAt:read?'2026-10-04T11:00:00Z':null},{id:2,kind:'OTHER',title:'<img src=x onerror=alert(1)>',body:'<script>throw Error("unsafe")</script>',createdAt:'2026-10-04T09:00:00Z',readAt:'2026-10-04T11:00:00Z'}];
   }else if(url.pathname==='/notifications/unread-count'){await new Promise(resolve=>setTimeout(resolve,100));body={count:read?0:1};}
   else if(url.pathname==='/notifications/1/read'){
    assert.equal(route.request().method(),'POST');assert.equal(route.request().headers()['x-csrf-token'],'test');
    if(failRead){await route.fulfill({status:500,json:{code:'HTTP_ERROR'}});return;}
    read=true;await route.fulfill({status:204});return;
   }else return route.continue();
   await route.fulfill({json:body});
  });
  await page.goto('http://127.0.0.1:'+server.address().port+'/#notifications');
  await page.locator('.notification-card').first().waitFor();
  await page.locator('[data-notification-total]').waitFor();
  assert.equal(await page.locator('[data-notification-total]').textContent(),'1');
  for(const language of ['pl','en']){
   await page.locator('[data-language="'+language+'"]').click();
   const heading=language==='pl'?'Powiadomienia':'Notifications';
   await page.waitForFunction(heading=>document.querySelector('#main h1')?.textContent===heading,heading);
   assert.equal(await page.locator('.notification-card h2').first().textContent(),language==='pl'?'Nowa wiadomość':'New message');
   assert.equal(await page.locator('.notification-card > p').first().textContent(),language==='pl'?'Otrzymano nową wiadomość w MatchCats.':'You received a new MatchCats message.');
   assert.equal(await page.locator('.notification-card').nth(1).locator('h2').textContent(),'<img src=x onerror=alert(1)>');
   assert.equal(await page.locator('.notification-card img, .notification-card script').count(),0);
   assert.equal(await page.locator('.notification-unread').count(),1);
   for(const width of [320,390,768,1440]){
    await page.setViewportSize({width,height:1000});
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Notification layout overflow: '+language+' '+width);
   }
  }
  const action=page.locator('[data-notification-read="1"]');
  await action.click();await page.locator('.live-error').waitFor();
  assert.equal(await action.isEnabled(),true);assert.equal(await page.locator('.notification-unread').count(),1);
  failRead=false;await action.click();await page.waitForFunction(()=>!document.querySelector('[data-notification-read]'));
  assert.equal(await page.locator('.notification-unread').count(),0);
  await page.waitForFunction(()=>document.querySelector('[data-notification-total]')?.textContent==='0');
  assert.equal(await page.locator('[data-notification-total]').textContent(),'0');
  countFails=true;await page.reload();await page.locator('.notification-card').first().waitFor();
  assert.equal(await page.locator('.notification-card').count(),2);
  empty=true;await page.reload();await page.getByText('You have no notifications yet.',{exact:true}).waitFor();
  assert.deepEqual(errors,[]);
  console.log('PASS: notifications PL/EN, escaping, mobile layout, failed and successful read, empty state.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
