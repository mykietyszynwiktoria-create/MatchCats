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
  for(const signedIn of [false,true]){
   const context=await browser.newContext();const page=await context.newPage();const errors=[];
   page.on('pageerror',error=>errors.push(error.message));
   await page.route('**/*',async route=>{
    const url=new URL(route.request().url());let body;
    if(url.pathname==='/users/me'){
     if(!signedIn){await route.fulfill({status:401,json:{status:401,code:'UNAUTHENTICATED'}});return;}
     body={id:1,username:'legaltest',firstName:'Test',surname:'Breeder',email:'test@example.test',emailVerified:false};
    }else if(url.pathname==='/owners/me')body={id:1,kennel:'Test cattery',city:'Test',country:'PL',bio:''};
    else if(url.pathname==='/safety/capabilities')body={moderator:false,moderationConfigured:false};
    else if(url.pathname==='/chats/unread')body={unreadMessages:0,unreadConversations:0};
    else if(url.pathname==='/billing/me')body={planId:'FREE',active:true,status:'ACTIVE'};
    else return route.continue();
    await route.fulfill({json:body});
   });
   if(!signedIn)for(const language of ['pl','en'])for(const mode of ['login','register']){
    await page.goto('http://127.0.0.1:'+server.address().port+'/#'+mode);
    await page.locator('#'+mode+'-form').waitFor();
    await page.locator('[data-language="'+language+'"]').click();
    const link=page.locator('.auth-panel a[href="#plans"]');
    assert.equal(await link.count(),1,'Authentication panel must have one plans link');
    assert.equal(await link.textContent(),language==='pl'?'Plany':'Plans');
    await link.click();
    await page.locator('.plans-page h1').waitFor();
    assert.equal(await page.locator('.plans-page h1').textContent(),language==='pl'?'Plany MatchCats':'MatchCats plans');
   }
   for(const language of ['pl','en'])for(const kind of ['privacy','terms','plans']){
    const section=kind==='plans'?'.plans-page':'.legal-page';
    await page.goto('http://127.0.0.1:'+server.address().port+'/#'+kind);
    await page.locator(section+' h1').waitFor();
    await page.locator('[data-language="'+language+'"]').click();
    const heading=kind==='privacy'?(language==='pl'?'Polityka prywatności':'Privacy policy'):kind==='terms'?(language==='pl'?'Regulamin MatchCats':'MatchCats terms of use'):(language==='pl'?'Plany MatchCats':'MatchCats plans');
    await page.waitForFunction(({section,heading})=>document.querySelector(section+' h1')?.textContent===heading,{section,heading});
    for(const width of [320,390,768,1440]){
     await page.setViewportSize({width,height:1000});
     assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Legal page overflow '+kind+' '+language+' '+width);
    }
    const back=page.locator(section+' .live-tools a');
    assert.equal(await back.getAttribute('href'),signedIn?'#settings':'#login','Return link must preserve the current session');
    await back.click();
    await page.locator(signedIn?'#account-form':'#login-form').waitFor();
    if(signedIn)assert.equal(await page.evaluate(()=>document.body.classList.contains('signed-out')),false);
   }
   assert.deepEqual(errors,[]);await context.close();
  }
  console.log('PASS: privacy, terms and plans pages, PL/EN, mobile layouts and return navigation with and without a session.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
