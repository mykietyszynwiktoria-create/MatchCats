const {chromium}=require('playwright'),assert=require('assert/strict'),fs=require('fs'),path=require('path'),http=require('http');
(async()=>{
 const root=path.resolve(__dirname,'..');
 const server=http.createServer((req,res)=>{const file=path.resolve(root,new URL(req.url,'http://localhost').pathname.slice(1)||'index.html');if(!file.startsWith(root+path.sep)||!fs.existsSync(file)||!fs.statSync(file).isFile()){res.writeHead(404);return res.end();}res.setHeader('Content-Type',({'.html':'text/html','.css':'text/css','.js':'application/javascript'})[path.extname(file)]||'application/octet-stream');res.end(fs.readFileSync(file));});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH});
  const account={id:1,username:'longprofile',firstName:'A'.repeat(80),surname:'B'.repeat(80),email:'a'.repeat(64)+'@'+'d'.repeat(60)+'.example.test',emailVerified:false};
  const breeder={id:1,kennel:'K'.repeat(120),city:'C'.repeat(100),country:'P'.repeat(100),bio:'Bio'.repeat(600)};
  const cat={id:7,ownerId:1,name:'N'.repeat(100),breed:'R'.repeat(100),city:breeder.city,country:breeder.country,description:'Description'.repeat(180),birthDate:'2024-01-01',health:'HEALTHY',sex:'FEMALE',available:true,hasPhoto:false};
  for(const width of [320,390,768,1440])for(const language of ['pl','en']){
   const page=await browser.newPage({viewport:{width,height:900}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.route('**/*',async route=>{const url=new URL(route.request().url());let body;
    if(url.pathname==='/users/me')body=account;else if(url.pathname==='/owners/me'||url.pathname==='/owners/1')body=breeder;
    else if(url.pathname==='/safety/capabilities')body={moderator:false,moderationConfigured:false};else if(url.pathname==='/chats/unread')body={unreadMessages:0,unreadConversations:0};else if(url.pathname==='/notifications/unread-count')body={count:0};
    else if(url.pathname==='/cats')body={items:[cat],total:1,page:0,size:20};else if(url.pathname==='/cats/7')body=cat;else if(url.pathname==='/cats/7/documents')body=[];else return route.continue();await route.fulfill({json:body});
   });
   await page.goto('http://127.0.0.1:'+server.address().port+'/#settings');await page.locator('#account-form').waitFor();await page.locator('[data-language="'+language+'"]').click();await page.locator('#account-form').waitFor();
   for(const [route,selector] of [['settings','#account-form'],['dashboard','.cat-card'],['search','#live-search'],['cat/7','#photo-upload']]){
    await page.evaluate(route=>location.hash=route,route);await page.locator(selector).waitFor();
    const overflow=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth,offenders:[...document.querySelectorAll('#main *')].filter(e=>e.getBoundingClientRect().right>innerWidth+1).slice(0,8).map(e=>({tag:e.tagName,class:e.className,right:e.getBoundingClientRect().right}))}));
    if(overflow.scroll>width+1){throw Error(JSON.stringify({route,language,...overflow}));}
   }
   assert.deepEqual(errors,[]);await page.close();
  }
  console.log('PASS: maximum-length unbroken profile/cat names, breed/city/country/bio/email across settings/dashboard/search/cat PL/EN at 320/390/768/1440, no document overflow or JS errors.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});

