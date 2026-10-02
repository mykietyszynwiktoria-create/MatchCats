const { chromium } = require('playwright');
const path=require('path');
const assert=require('assert/strict');
const expectCount=async(locator,expected)=>{
 const deadline=Date.now()+10000;
 while(await locator.count()!==expected && Date.now()<deadline)
  await new Promise(resolve=>setTimeout(resolve,50));
 assert.equal(await locator.count(),expected);
};
(async()=>{
 const fs=require('fs');
 const http=require('http');
 const root=path.resolve(__dirname,'..');
 const allowed={'/':'index.html','/index.html':'index.html','/styles.css':'styles.css','/sky-garden.css':'sky-garden.css','/app.js':'app.js','/api.js':'api.js','/live.js':'live.js','/safety.js':'safety.js','/live.css':'live.css','/i18n.js':'i18n.js','/assets/sky-hero.jpg':'assets/sky-hero.jpg','/assets/sky-portraits.jpg':'assets/sky-portraits.jpg'};
 const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.jpg':'image/jpeg'};
 const server=http.createServer((req,res)=>{
  const file=allowed[new URL(req.url,'http://localhost').pathname];
  if(!file){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',mime[path.extname(file)]);
  res.end(fs.readFileSync(path.join(root,file)));
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const url='http://127.0.0.1:'+server.address().port+'/?demo=1';
 let browser;
 try {
 browser=await chromium.launch({...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{}),headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1100}});
 const errors=[];page.on('pageerror',error=>errors.push(error.message));
 await page.goto(url);
 await page.locator('#dashboard-search select[name="breed"]').selectOption('Maine Coon');
 await page.locator('#dashboard-search select[name="sex"]').selectOption('MALE');
 await page.locator('#dashboard-search input[name="city"]').fill('Kraków');
 await page.locator('#dashboard-search button').click();
 await page.waitForURL('**#search');
 assert.equal(await page.locator('#filters input[name="city"]').inputValue(),'Kraków');
 await expectCount(page.locator('#search-results .cat-card'),1);
 await page.goto(url+'#dashboard');
 await expectCount(page.locator('.quick-action'),3);
 await page.locator('.quick-action[href="#cats"]').click();
 await expectCount(page.locator('.cat-card'),2);
 await page.locator('a[href="#dashboard"]').first().click();
 await page.locator('.quick-action[href="#messages"]').click();
 await page.locator('.chat-pane').waitFor({state:'visible'});
 await expectCount(page.locator('.chat-pane'),1);
 await page.locator('a[href="#dashboard"]').first().click();
 await page.locator('.quick-action[href="#search"]').click();
 await page.locator('#filters select[name="breed"]').selectOption('Maine Coon');
 await page.locator('#filters select[name="sex"]').selectOption('MALE');
 await expectCount(page.locator('#search-results .cat-card'),1);
 await page.locator('#filters input[name="city"]').fill('Gdańsk');
 await expectCount(page.locator('#search-results .cat-card'),0);
 await page.getByRole('button',{name:'Wyczyść filtry'}).click();
 await expectCount(page.locator('#search-results .cat-card'),3);
 await page.locator('[data-cat="2"]').click();
 await page.locator('[data-contact="2"]').click();
 await page.locator('input[name="message"]').fill('<img src=x onerror=alert(1)> Wiadomość próbna');
 await page.locator('#message-form button').click();
 await expectCount(page.locator('.bubble.mine'),1);
 await expectCount(page.locator('.bubble.mine img'),0);
 await page.reload();
 await expectCount(page.locator('.bubble.mine'),1);
 await page.locator('a[href="#cats"]').first().click();
 await page.locator('[data-add]').click();
 await page.locator('#cat-form input[name="name"]').fill('Testowa Luna');
 await page.locator('#cat-form input[name="birth"]').fill('2023-01-01');
 await page.locator('#cat-form input[name="city"]').fill('Łódź');
 await page.locator('#cat-form button[type="submit"]').click();
 await expectCount(page.locator('.cat-card'),3);
 await page.reload();
 await expectCount(page.locator('.cat-card'),3);
 await page.locator('.cat-card').last().getByRole('button').click();
 await page.locator('[data-toggle]').click();
 await page.locator('[data-toggle]').filter({hasText:'Włącz'}).waitFor();
 assert.match(await page.locator('[data-toggle]').textContent(),/Włącz/);
 await page.locator('#top-profile').click();
 await page.locator('#profile-form input[name="kennel"]').fill('Hodowla Testowa');
 await page.locator('#profile-form button[type="submit"]').click();
 await page.locator('.workspace strong').filter({hasText:'Hodowla Testowa'}).waitFor({state:'attached'});
 assert.equal(await page.locator('.workspace strong').textContent(),'Hodowla Testowa');
 page.on('dialog',dialog=>dialog.accept());
 await page.locator('#reset-demo').click();
 await page.locator('a[href="#dashboard"]').first().click();
 await page.setViewportSize({width:390,height:844});
 for(const route of ['dashboard','cats','search','messages','documents','settings','cat/2']){
  await page.goto(url+'#'+route);
  const width=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,viewport:innerWidth}));
  assert.ok(width.scroll<=width.viewport,`Horizontal overflow ${route}: ${JSON.stringify(width)}`);
 }
 assert.deepEqual(errors,[]);
 await page.locator('[data-language="en"]').click();
 assert.equal(await page.locator('html').getAttribute('lang'),'en');
 assert.equal(await page.locator('[data-contact="2"]').textContent(),'Contact the breeder');
 await page.locator('a[href="#search"]').first().click();
 await page.locator('#filters select[name="breed"]').selectOption('Brytyjski krótkowłosy');
 await expectCount(page.locator('#search-results .cat-card'),1);
 assert.match(await page.locator('#search-results .cat-meta').textContent(),/British Shorthair/);
 await page.reload();
 assert.equal(await page.locator('html').getAttribute('lang'),'en');
 await page.locator('a[href="#cats"]').first().click();
 await page.locator('[data-add]').click();
 assert.equal(await page.locator('#dialog-title').textContent(),'Add a cat profile');
 await page.locator('#close-dialog').click();
 await page.locator('[data-language="pl"]').click();
 await page.locator('[data-add]').click();
 assert.equal(await page.locator('#dialog-title').textContent(),'Dodaj profil kota');
 await page.locator('#close-dialog').click();
 for(const language of ['pl','en']){
  await page.locator('[data-language="'+language+'"]').click();
  for(const viewport of [{width:320,height:760},{width:390,height:844},{width:768,height:1024},{width:1440,height:1000}]){
   await page.setViewportSize(viewport);
   for(const route of ['dashboard','cats','search','messages','documents','settings','cat/2']){
    await page.goto(url+'#'+route);
    const size=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,viewport:innerWidth}));
    assert.ok(size.scroll<=size.viewport,'Overflow '+language+' '+route+' '+viewport.width+': '+JSON.stringify(size));
   }
  }
 }
 assert.deepEqual(errors,[]);
 console.log('PASS: filtry, brak wyników, reset filtrów, profil, rozmowa, bezpieczne wyświetlanie tekstu, trwałość danych, dodanie kota, dostępność, ustawienia, reset demonstracji, 7 ekranów w PL/EN przy 320, 390, 768 i 1440 px bez poziomego przewijania, brak błędów JS.');

 } finally { if(browser)await browser.close();await new Promise(resolve=>server.close(resolve)); }
})().catch(error=>{console.error(error);process.exit(1)});
