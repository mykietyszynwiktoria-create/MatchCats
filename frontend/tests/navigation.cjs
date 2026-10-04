const {chromium}=require('playwright');
const assert=require('assert/strict');
const fs=require('fs'),path=require('path'),http=require('http');

(async()=>{
 const root=path.resolve(__dirname,'..');
 const files=new Set(['index.html','styles.css','sky-garden.css','live.css','i18n.js','api.js','safety.js','error-pages.js','live.js','app.js','assets/sky-hero.jpg','assets/sky-portraits.jpg']);
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
  const page=await browser.newPage();const errors=[];
  page.on('pageerror',error=>errors.push(error.message));
  const chat=id=>({id,firstOwnerId:1,secondOwnerId:id+1,contactBlocked:false,unreadMessages:0});
  await page.route('**/*',async route=>{
   const url=new URL(route.request().url());let body;
   if(url.pathname==='/users/me')body={id:1,username:'navigation',firstName:'Test',surname:'Breeder',email:'test@example.test'};
   else if(url.pathname.startsWith('/owners/'))body={id:1,kennel:'Test cattery',city:'Test',country:'PL',bio:''};
   else if(url.pathname==='/safety/capabilities')body={moderator:false,moderationConfigured:false};
   else if(url.pathname.startsWith('/safety/contact/'))body={contactBlocked:false,blockedByYou:false};
   else if(url.pathname==='/chats/unread')body={unreadMessages:0,unreadConversations:0};
   else if(url.pathname==='/chats'){
    const index=Number(url.searchParams.get('page')||0);
    body={items:index===0?Array.from({length:20},(_,i)=>chat(i+1)):[chat(21)],page:index,size:20,total:21};
   }else if(/^\/chats\/\d+\/messages$/.test(url.pathname))body={items:[],page:0,size:50,total:0};
   else if(/^\/chats\/\d+$/.test(url.pathname))body=chat(Number(url.pathname.split('/')[2]));
   else return route.continue();
   await route.fulfill({json:body});
  });
  await page.goto('http://127.0.0.1:'+server.address().port+'/#messages');
  await page.locator('.conversation-row').first().waitFor();
  await page.locator('[data-page-type="cats"][data-page="1"]').click();
  await page.locator('.conversation-row[href="#messages/21"]').waitFor();
  await page.locator('.conversation-row[href="#messages/21"]').click();
  await page.waitForURL('**#messages/21');
  await page.locator('#live-message-form[data-chat-id="21"]').waitFor();
  assert.equal(await page.locator('.conversation-row').count(),1,'Selecting a conversation must preserve the list page');
  assert.equal(await page.locator('.conversation-row.selected').getAttribute('href'),'#messages/21');
  assert.equal(await page.locator('.chat-header strong').textContent(),'Test cattery');
  assert.deepEqual(errors,[]);
  console.log('PASS: conversation selection preserves the second list page and its breeder label.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
