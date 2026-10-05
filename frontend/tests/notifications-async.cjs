const {chromium}=require('playwright'),assert=require('assert/strict');
const fs=require('fs'),path=require('path'),http=require('http');
(async()=>{
 const root=path.resolve(__dirname,'..');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,new URL(req.url,'http://localhost').pathname.slice(1)||'index.html');
  if(!file.startsWith(root+path.sep)||!fs.existsSync(file)||!fs.statSync(file).isFile()){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',({'.html':'text/html','.css':'text/css','.js':'application/javascript','.jpg':'image/jpeg'})[path.extname(file)]||'application/octet-stream');res.end(fs.readFileSync(file));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
  for(const oldStatus of [200,401]){
   const context=await browser.newContext({serviceWorkers:'block'}),page=await context.newPage();let account=1,pending,queued;let newerRequests=0;const errors=[];
   const held=new Promise(resolve=>queued=resolve);
   page.on('pageerror',e=>errors.push(e.message));
   await page.addInitScript(()=>{
    const original=window.fetch;
    window.fetch=async(...args)=>{
     const response=await original(...args);
     if(args[0]==='/notifications/unread-count')for(const method of ['text','json']){
      const read=response[method].bind(response);response[method]=async()=>{const value=await read();setTimeout(()=>window.completedCounts=(window.completedCounts||0)+1,0);return value;};
     }
     return response;
    };
   });
   const user=()=>({id:account,username:'account'+account,firstName:'Test',surname:'Breeder',email:'test@example.test'});
   await page.route('**/*',async route=>{
    const url=new URL(route.request().url());let body;
    if(url.pathname==='/notifications/unread-count'){
     if(account===1){pending=route;queued();return;}
     newerRequests++;body={count:0};
    }else if(url.pathname==='/users/me')body=user();
    else if(url.pathname==='/owners/me')body={id:account,kennel:'Test',city:'Test',country:'PL',bio:''};
    else if(url.pathname==='/safety/capabilities')body={moderator:false,moderationConfigured:false};
    else if(url.pathname==='/chats/unread')body={unreadMessages:0,unreadConversations:0};
    else if(url.pathname==='/auth/csrf')body={headerName:'X-CSRF-TOKEN',token:'test'};
    else if(url.pathname==='/auth/logout'){await route.fulfill({status:204});return;}
    else if(url.pathname==='/auth/login'){account=2;body=user();}
    else if(url.pathname==='/cats')body={items:[],page:0,size:20,total:0};
    else return route.continue();
    await route.fulfill({json:body});
   });
   await page.goto('http://127.0.0.1:'+server.address().port+'/#settings');await held;
   await page.locator('[data-logout]').click();await page.locator('#login-form').waitFor();
   await page.locator('#login-form [name="username"]').fill('account2');await page.locator('#login-form [name="password"]').fill('StrongPassword!');await page.locator('#login-form button[type="submit"]').click();
   await page.waitForURL('**#dashboard');await page.locator('#main .hero').waitFor();
   await pending.fulfill({status:oldStatus,json:oldStatus===200?{count:99}:{code:'UNAUTHENTICATED'}});
   await page.waitForFunction(()=>window.completedCounts>=1);
   assert.equal(await page.locator('#login-form').count(),0,'Old response '+oldStatus+' must not log out account 2');
   assert.equal(await page.locator('[data-notification-total]').isHidden(),true,'Old account count must not appear in the new account ('+oldStatus+')');
   await page.waitForFunction(()=>window.completedCounts>=2);
   assert.ok(newerRequests>0,'The current account count must be refreshed after the old request ends');assert.deepEqual(errors,[]);await context.close();
  }
  console.log('PASS: delayed notification count or 401 cannot affect a different account; current count refreshes.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exitCode=1;});
