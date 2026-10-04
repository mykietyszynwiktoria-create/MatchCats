const {chromium}=require('playwright');
const assert=require('assert/strict');
const fs=require('fs'),path=require('path'),http=require('http');

(async()=>{
 const root=path.resolve(__dirname,'..');
 const files=new Set(['index.html','styles.css','sky-garden.css','live.css','i18n.js','api.js','safety.js','error-pages.js','legal.js','plans.js','live.js','app.js','assets/sky-hero.jpg','assets/sky-portraits.jpg']);
 const server=http.createServer((req,res)=>{
  const file=new URL(req.url,'http://localhost').pathname.slice(1)||'index.html';
  if(!files.has(file)){res.writeHead(404);res.end();return;}
  const mime={'.html':'text/html','.css':'text/css','.js':'application/javascript','.jpg':'image/jpeg'};
  res.setHeader('Content-Type',mime[path.extname(file)]);res.end(fs.readFileSync(path.join(root,file)));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
  const page=await browser.newPage();const errors=[];let signedIn=true,pendingPlan;
  page.on('pageerror',error=>errors.push(error.message));
  await page.addInitScript(()=>{
   const original=window.fetch;
   window.fetch=async(...args)=>{
    const response=await original(...args);
    if(args[0]==='/billing/me'){
     const originalText=response.text.bind(response);
     response.text=async()=>{
      const text=await originalText();
      setTimeout(()=>window.completedPlanResponses=(window.completedPlanResponses||0)+1,0);
      return text;
     };
    }
    return response;
   };
  });
  await page.route('**/*',async route=>{
   const url=new URL(route.request().url());let body;
   if(url.pathname==='/billing/me'){pendingPlan=route;return;}
   if(url.pathname==='/users/me'){
    if(!signedIn){await route.fulfill({status:401,json:{status:401,code:'UNAUTHENTICATED'}});return;}
    body={id:1,username:'plantest',firstName:'Test',surname:'Breeder',email:'test@example.test',emailVerified:false};
   }else if(url.pathname==='/owners/me')body={id:1,kennel:'Test cattery',city:'Test',country:'PL',bio:''};
   else if(url.pathname==='/safety/capabilities')body={moderator:false,moderationConfigured:false};
   else if(url.pathname==='/chats/unread')body={unreadMessages:0,unreadConversations:0};
   else if(url.pathname==='/auth/csrf')body={headerName:'X-CSRF-TOKEN',token:'test'};
   else if(url.pathname==='/auth/logout'){signedIn=false;await route.fulfill({status:204});return;}
   else return route.continue();
   await route.fulfill({json:body});
  });
  await page.goto('http://127.0.0.1:'+server.address().port+'/#plans');
  await page.locator('.plans-page h1').waitFor();
  assert.ok(pendingPlan,'Account plan request must be pending');
  await page.locator('#navigation a[href="#settings"]').click();
  await page.locator('#account-form').waitFor();
  await page.locator('[data-logout]').click();
  await page.locator('#login-form').waitFor();
  await page.locator('.auth-panel a[href="#plans"]').click();
  await page.locator('.plans-page h1').waitFor();
  const visitorText=await page.locator('[data-current-plan]').textContent();
  assert.equal(visitorText,'Zaloguj się, aby zobaczyć plan konta.');
  await pendingPlan.fulfill({json:{planId:'FREE',active:true,status:'ACTIVE'}});
  await page.waitForFunction(()=>window.completedPlanResponses===1);
  assert.equal(await page.locator('[data-current-plan]').textContent(),visitorText,'A response from the logged-out account must not overwrite the visitor page');
  assert.deepEqual(errors,[]);
  console.log('PASS: delayed account-plan response cannot change the plans page after logout.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
