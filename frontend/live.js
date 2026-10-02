'use strict';
if (new URLSearchParams(location.search).get('demo') !== '1') (() => {
  const api = MatchCatsAPI.request;
  const main = document.querySelector('#main');
  let user = null, breeder = null, language = 'pl', revision = 0, flash = '', editingCat = null;
  let searchFilters = {}, listPage = 0, chatPage = 0;
  try { language = localStorage.getItem('matchcats-language') === 'en' ? 'en' : 'pl'; } catch {}
  const t = (pl, en) => language === 'en' ? en : pl;
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const button = (label, href, style='primary') => `<a class="button ${style}" href="#${href}">${label}</a>`;
  const title = (heading, description='') => `<div class="heading"><div><h1>${esc(heading)}</h1><p class="muted">${esc(description)}</p></div></div>`;
  const field = (name, label, type='text', value='', attrs='') => `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${attrs}></label>`;
  const formError = '<div class="live-error" role="alert" id="live-error" hidden></div>';
  function message(error) {
    const messages = {
      NETWORK:t('Nie można połączyć się z serwerem. Sprawdź połączenie i spróbuj ponownie.','Cannot reach the server. Check your connection and try again.'),
      INVALID_CREDENTIALS:t('Nieprawidłowy login lub hasło.','Invalid username or password.'),
      ACCOUNT_EXISTS:t('Login lub adres e-mail jest już zajęty.','Username or email is already registered.'),
      VALIDATION_FAILED:t('Sprawdź wymagane pola i ich poprawność.','Check the required fields and their values.'),
      INVALID_REQUEST:t('Dane nie spełniają wymagań. Sprawdź formularz.','The submitted data does not meet the requirements. Check the form.'),
      FORBIDDEN:t('Nie masz dostępu do tej operacji. Odśwież stronę i spróbuj ponownie.','You cannot perform this action. Refresh the page and try again.'),
      STALE_VERSION:t('Profil został zmieniony. Odśwież go przed kolejnym zapisem.','The profile has changed. Refresh it before saving again.'),
      CONFLICT:t('Dane są już używane lub zmieniły się podczas zapisu. Odśwież stronę.','The data is already used or changed while saving. Refresh the page.'),
      FILE_TOO_LARGE:t('Plik jest za duży. Maksymalny rozmiar to 5 MB.','The file is too large. Maximum size is 5 MB.'),
      NOT_FOUND:t('Nie znaleziono tego elementu.','This item was not found.')
    };
    if (error.status === 429) return t('Zbyt wiele prób. Poczekaj chwilę i spróbuj ponownie.','Too many attempts. Wait a moment and try again.');
    return messages[error.code] || (error.status===404?messages.NOT_FOUND:t('Operacja nie powiodła się. Spróbuj ponownie później.','The operation failed. Please try again later.'));
  }
  function showError(error) {
    if(error.status===401 && user) { user=null;breeder=null;location.hash='login';render(t('Sesja wygasła. Zaloguj się ponownie.','Your session expired. Sign in again.'));return; }
    let box=document.querySelector('#live-error');
    if(!box) {box=document.createElement('div');box.id='live-error';box.className='live-error';box.setAttribute('role','alert');main.prepend(box);}
    box.hidden=false;box.textContent=typeof error==='string'?error:message(error);
    box.tabIndex=-1;box.focus();
    if(error.fields) for(const name of Object.keys(error.fields)) {
      const input=main.querySelector(`[name="${CSS.escape(name.replace('profile.',''))}"]`);
      if(input) input.setAttribute('aria-invalid','true');
    }
  }
  function shell(route) {
    document.body.classList.toggle('signed-out',!user);
    document.documentElement.lang=language;
    document.querySelectorAll('[data-language]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.language===language)));
    document.querySelector('.skip').textContent=t('Przejdź do treści','Skip to content');
    document.querySelector('.demo-chip').textContent=t('Dane na serwerze','Server data');
    document.querySelector('.topbar-label').textContent=t('PANEL HODOWCY','BREEDER PANEL');
    document.querySelector('footer').innerHTML=`MatchCats · ${t('Łączymy hodowców, z troską o koty.','Connecting breeders with care for cats.')}<span>${t('Prywatne konto · Dane w bazie','Private account · Database storage')}</span>`;
    const nav=[['dashboard',t('Start','Home')],['cats',t('Moje koty','My cats')],['search',t('Szukaj kota','Find a cat')],['messages',t('Rozmowy','Conversations')],['documents',t('Dokumenty','Documents')],['settings',t('Ustawienia','Settings')]];
    const current=nav.find(x=>x[0]===route)?.[1]||t('Profil','Profile');
    document.querySelector('#breadcrumb').textContent=current;
    document.title=`MatchCats · ${user?current:t('Twoje konto','Your account')}`;
    document.querySelector('#navigation').innerHTML=user?nav.map(([key,label])=>`<a href="#${key}" class="nav-item ${key===route?'active':''}" ${key===route?'aria-current="page"':''}><span class="nav-icon" aria-hidden="true">${{dashboard:'⌂',cats:'♧',search:'⌕',messages:'✉',documents:'▤',settings:'⚙'}[key]}</span><span class="nav-text">${label}</span></a>`).join(''):'';
    document.querySelector('#top-profile').textContent=user?.firstName?.slice(0,1)||'•';
    document.querySelector('#top-profile').setAttribute('aria-label',t('Ustawienia konta','Account settings'));
  }
  function auth(mode, notice='') {
    const register=mode==='register';
    return `<div class="auth-layout"><section class="auth-intro"><h1>${t('Poznaj koty.<br>Poznaj ich hodowców.','Meet cats.<br>Meet their breeders.')}</h1><p class="muted">${t('Twoje koty, dokumenty i rozmowy w jednej przestrzeni.','Your cats, documents and conversations in one place.')}</p><img src="assets/sky-hero.jpg" alt=""><p class="live-caption">${t('Dekoracyjne zdjęcie wygenerowane dla MatchCats.','Decorative image generated for MatchCats.')}</p></section><section class="auth-panel"><h1>${register?t('Utwórz konto','Create an account'):t('Zaloguj się','Sign in')}</h1><p class="muted">${register?t('Dołącz do hodowców MatchCats.','Join MatchCats breeders.'):t('Witaj ponownie w swojej hodowli.','Welcome back to your cattery.')}</p>${notice?`<p class="live-success" role="status">${esc(notice)}</p>`:''}<form id="${register?'register':'login'}-form" novalidate>${formError}${field('username',t('Login','Username'),'text','','required minlength="3" maxlength="40" autocomplete="username" autocapitalize="none" spellcheck="false"')}${register?field('email',t('Adres e-mail','Email address'),'email','','required maxlength="254" autocomplete="email"')+field('firstName',t('Imię','First name'),'text','','required maxlength="80" autocomplete="given-name"')+field('surname',t('Nazwisko','Surname'),'text','','required maxlength="80" autocomplete="family-name"'):''}${field('password',t('Hasło','Password'),'password','','required '+(register?'minlength="8" maxlength="72" autocomplete="new-password"':'autocomplete="current-password"'))}${register?`<p class="muted live-caption">${t('Minimum 8 znaków. Maksymalnie 72 bajty UTF-8. Login: 3-40 liter, cyfr lub znaków . _ -','At least 8 characters. Maximum 72 UTF-8 bytes. Username: 3-40 letters, digits or . _ -') }</p>`+field('confirm',t('Powtórz hasło','Repeat password'),'password','','required autocomplete="new-password"'):''}<label class="check"><input type="checkbox" data-show-password>${t('Pokaż hasło','Show password')}</label><button class="button primary" type="submit">${register?t('Utwórz konto','Create account'):t('Zaloguj się','Sign in')}</button></form><a class="auth-link" href="#${register?'login':'register'}">${register?t('Mam już konto - zaloguj się','Already have an account? Sign in'):t('Nie masz konta? Zarejestruj się','No account? Register')}</a><a class="auth-link" href="/?demo=1">${t('Zobacz demonstrację bez konta','Explore the demo without an account')}</a></section></div>`;
  }
  async function render(notice='') {
    if(notice) flash=notice;
    const ownRevision=++revision;
    const [route='dashboard',id]=location.hash.slice(1).split('/');shell(route);
    if(!user){ main.innerHTML=auth(route==='register'?'register':'login',flash);return; }
    main.innerHTML=`<div class="live-loading" role="status">${t('Wczytywanie…','Loading…')}</div>`;
    try {
      const result=await view(route,id);
      if(ownRevision===revision) main.innerHTML=result;
    } catch(error) { if(ownRevision===revision){main.innerHTML=button(t('Spróbuj ponownie','Try again'),route);showError(error);} }
  }
  async function view(route,id) {
    if(!breeder && route!=='settings') return title(t('Uzupełnij swoją hodowlę','Complete your cattery'))+`<section class="panel"><p>${t('Zapisz informacje o hodowli, zanim dodasz kota lub skontaktujesz się z innym hodowcą.','Save your cattery information before adding a cat or contacting another breeder.')}</p>${button(t('Uzupełnij profil','Complete profile'),'settings')}</section>`;
    if(route==='settings') return settings();
    if(route==='cats' || route==='search' || route==='dashboard') return catList(route);
    if(route==='cat') return catDetail(id);
    if(route==='new-cat' || route==='edit-cat') return catForm(route==='edit-cat'?await api('/cats/'+id):null);
    if(route==='messages') return conversations(id);
    if(route==='documents') return documentPage(id);
    if(route==='candidates') {
      const result=await api('/cats/'+id+'/candidates?page='+listPage+'&size=20');
      const saved=await api('/cats/'+id+'/matches?size=100');
      return title(t('Kandydaci do rozmowy','Candidates for discussion'),t('Ta sama rasa i przeciwna płeć. To nie jest ocena genetyczna.','Same breed and opposite sex. This is not a genetic assessment.'))+`<div class="cat-grid">${result.items.map(c=>`<section>${card(c)}<button type="button" class="button primary" data-pair-source="${Number(id)}" data-pair-candidate="${c.id}">${t('Zapisz propozycję i otwórz rozmowę','Save proposal and open conversation')}</button></section>`).join('')||t('Brak dostępnych kandydatów.','No available candidates.')}</div>`+pager(result,'cats')+`<section class="panel"><h2>${t('Zapisane propozycje','Saved proposals')}</h2>${saved.items.map(p=>`<p>${t('Para','Pair')} #${p.firstCatId} / #${p.secondCatId} ${button(t('Otwórz rozmowę','Open conversation'),'messages/'+p.conversationId,'secondary')}</p>`).join('')||t('Brak zapisanych propozycji.','No saved proposals.')}</section>`;
    }
    return title(t('Nie znaleziono strony','Page not found'))+button(t('Wróć na start','Back home'),'dashboard');
  }
  const sex = value => value==='MALE'?t('Kocur','Male'):t('Kotka','Female');
  const health = value => ({UNKNOWN:t('Niepodane','Unknown'),HEALTHY:t('Zdrowy według właściciela','Owner-declared healthy'),SICK:t('Chory według właściciela','Owner-declared sick')})[value];
  function card(cat) {
    return `<article class="cat-card"><div class="cat-picture live-photo-empty" aria-label="${t('Brak zdjęcia','No photo')}">♧</div><div class="cat-body"><div class="cat-title"><h3>${esc(cat.name)}</h3><span>${sex(cat.sex)}</span></div><p class="cat-meta">${esc(cat.breed)} · ${esc(cat.city)}</p><p>${cat.available?t('Dostępny do kontaktu','Available for contact'):t('Obecnie niedostępny','Currently unavailable')}</p><div class="cat-bottom">${button(t('Zobacz profil','View profile'),'cat/'+cat.id,'secondary')}</div></div></article>`;
  }
  function pager(result, type) {
    return `<div class="live-pages"><button class="button secondary" data-page="${Math.max(0,result.page-1)}" data-page-type="${type}" ${result.page===0?'disabled':''}>${t('Poprzednie','Previous')}</button><span>${t('Strona','Page')} ${result.page+1} · ${t('Wyników','Results')}: ${result.total}</span><button class="button secondary" data-page="${result.page+1}" data-page-type="${type}" ${(result.page+1)*result.size>=result.total?'disabled':''}>${t('Następne','Next')}</button></div>`;
  }
  async function catList(route) {
    const own=route==='cats';const home=route==='dashboard';
    const params=new URLSearchParams({page:String(home?0:listPage),size:'20'});
    if(!own)for(const [key,value] of Object.entries(searchFilters))if(value)params.set(key,value);
    const result=await api((own?'/owners/me/cats':'/cats')+'?'+params);
    const intro=home?`<section class="hero"><div class="hero-copy"><h2>${t('Poznaj koty.<br>Poznaj ich hodowców.','Meet cats.<br>Meet their breeders.')}</h2><p>${t('Odkrywaj profile i rozmawiaj z hodowcami.','Explore profiles and talk to breeders.')}</p><div class="hero-actions">${button(t('Szukaj kota','Find a cat'),'search')}${button(t('Dodaj kota','Add a cat'),'new-cat','secondary')}</div></div><div class="hero-art"><img src="assets/sky-hero.jpg" alt=""></div></section>`:'';
    const filters=route==='search'?`<form id="live-search" class="filters">${field('breed',t('Rasa','Breed'),'text',searchFilters.breed)}<label>${t('Płeć','Sex')}<select name="sex"><option value="">${t('Dowolna','Any')}</option><option value="MALE" ${searchFilters.sex==='MALE'?'selected':''}>${sex('MALE')}</option><option value="FEMALE" ${searchFilters.sex==='FEMALE'?'selected':''}>${sex('FEMALE')}</option></select></label>${field('city',t('Miasto','City'),'text',searchFilters.city)}<label class="check"><input type="checkbox" name="available" ${searchFilters.available?'checked':''}>${t('Tylko dostępne','Available only')}</label><button class="button primary" type="submit">${t('Szukaj','Search')}</button><button class="button secondary" type="button" data-clear-search>${t('Wyczyść filtry','Clear filters')}</button></form>`:'';
    return title(home?t('Witaj, ','Welcome, ')+user.firstName:own?t('Moje koty','My cats'):t('Szukaj kota','Find a cat'),breeder.kennel)+intro+filters+(own?`<div class="live-tools">${button(t('Dodaj kota','Add a cat'),'new-cat')}</div>`:'')+`<p class="live-caption">${t('Profile i stan zdrowia są deklarowane przez właścicieli. Sprawdź dokumenty przed planowaniem krycia.','Profiles and health are owner-declared. Review documents before planning a mating.')}</p><div class="cat-grid live-page">${result.items.map(card).join('')||`<section class="empty"><h2>${t('Brak profili','No profiles yet')}</h2><p>${t('Dodaj kota lub zmień filtry wyszukiwania.','Add a cat or adjust your search filters.')}</p></section>`}</div>`+pager(result,'cats');
  }
  function catForm(cat) {
    if(cat && cat.ownerId!==user.id)throw new MatchCatsAPI.APIError(403,'FORBIDDEN');
    editingCat=cat;
    const options=(values,selected,labels)=>values.map(v=>`<option value="${v}" ${v===selected?'selected':''}>${esc(labels(v))}</option>`).join('');
    return title(cat?t('Edytuj kota','Edit cat'):t('Dodaj kota','Add a cat'))+`<form id="live-cat-form" class="panel live-form">${formError}${field('name',t('Imię kota','Cat name'),'text',cat?.name,'required maxlength="100"')}${field('breed',t('Rasa','Breed'),'text',cat?.breed,'required maxlength="100"')}<label>${t('Płeć','Sex')}<select name="sex">${options(['FEMALE','MALE'],cat?.sex,sex)}</select></label><label>${t('Stan zdrowia zadeklarowany przez właściciela','Owner-declared health')}<select name="health">${options(['UNKNOWN','HEALTHY','SICK'],cat?.health,health)}</select></label>${field('birthDate',t('Data urodzenia','Date of birth'),'date',cat?.birthDate,'required max="'+new Date().toLocaleDateString('sv-SE')+'"')}${field('city',t('Miasto','City'),'text',cat?.city||breeder.city,'required maxlength="100"')}${field('country',t('Kraj','Country'),'text',cat?.country||breeder.country,'required maxlength="100"')}<label>${t('Opis','Description')}<textarea name="description" maxlength="2000">${esc(cat?.description)}</textarea></label><label class="check"><input name="available" type="checkbox" ${cat?.available?'checked':''}>${t('Dostępny do kontaktu (wymaga deklaracji zdrowia)','Available for contact (requires healthy declaration)')}</label><div class="live-tools"><button class="button primary" type="submit">${t('Zapisz kota','Save cat')}</button>${button(t('Anuluj','Cancel'),'cats','secondary')}</div></form>`;
  }
  async function catDetail(id) {
    const cat=await api('/cats/'+id);const owner=await api('/owners/'+cat.ownerId);const docs=await api('/cats/'+id+'/documents');
    const own=cat.ownerId===user.id;
    return title(cat.name,cat.breed)+`<div class="detail-layout"><section class="panel"><div class="cat-picture live-photo-empty">♧</div><p>${t('Nie dodano zdjęcia.','No photo has been added.')}</p><h2>${t('Dokumenty','Documents')}</h2>${documentsHTML(docs,own)}${own?button(t('Zarządzaj dokumentami','Manage documents'),'documents/'+id,'secondary'):''}</section><section class="detail-box"><h2>${esc(owner.kennel)}</h2><p>${esc(cat.city)} · ${esc(cat.country)}</p><p>${sex(cat.sex)} · ${esc(cat.birthDate)}</p><p>${health(cat.health)}</p><p>${esc(cat.description)}</p><p class="notice">${t('Profil i załączniki nie oznaczają niezależnej weryfikacji zdrowia, pochodzenia ani zgodności genetycznej.','A profile and attachments do not mean independent verification of health, ancestry or genetic compatibility.')}</p><div class="live-tools">${own?button(t('Edytuj profil','Edit profile'),'edit-cat/'+id,'secondary')+button(t('Znajdź kandydatów','Find candidates'),'candidates/'+id)+`<button type="button" class="button secondary danger" data-delete-cat="${cat.id}">${t('Usuń kota','Delete cat')}</button>`:`<button type="button" class="button primary" data-contact="${cat.id}" ${!cat.available?'disabled':''}>${t('Napisz do hodowcy','Contact breeder')}</button>`}</div></section></div>`;
  }
  const docKind=kind=>({PEDIGREE:t('Rodowód','Pedigree'),GENETIC_TEST:t('Badanie genetyczne','Genetic test'),AWARD:t('Osiągnięcie','Award'),HEALTH:t('Zdrowie','Health'),OTHER:t('Inny','Other')})[kind];
  function documentsHTML(docs,own) {
    return docs.map(d=>`<div class="live-file-row"><strong>${esc(d.filename)}<small>${docKind(d.kind)} · ${d.visibility==='PRIVATE'?t('Prywatny','Private'):t('Widoczny dla hodowców','Visible to breeders')}</small></strong><button type="button" class="button secondary" data-download="${d.id}">${t('Pobierz','Download')}</button>${own?`<button type="button" class="button secondary" data-share="${d.id}" data-visibility="${d.visibility==='PRIVATE'?'BREEDERS':'PRIVATE'}">${d.visibility==='PRIVATE'?t('Udostępnij hodowcom','Share with breeders'):t('Ustaw prywatny','Make private')}</button><button type="button" class="button secondary danger" data-delete-document="${d.id}">${t('Usuń','Delete')}</button>`:''}</div>`).join('')||`<p>${t('Brak widocznych dokumentów.','No visible documents.')}</p>`;
  }
  async function documentPage(id) {
    if(!id){const cats=await api('/owners/me/cats?page='+listPage+'&size=20');return title(t('Dokumenty kotów','Cat documents'))+`<div class="cat-grid">${cats.items.map(c=>`<section class="panel"><h2>${esc(c.name)}</h2>${button(t('Zarządzaj plikami','Manage files'),'documents/'+c.id)}</section>`).join('')||t('Najpierw dodaj kota.','Add a cat first.')}</div>`+pager(cats,'cats');}
    const cat=await api('/cats/'+id);if(cat.ownerId!==user.id)throw new MatchCatsAPI.APIError(403,'FORBIDDEN');
    const docs=await api('/cats/'+id+'/documents');
    return title(t('Dokumenty: ','Documents: ')+cat.name)+`<section class="panel">${formError}${documentsHTML(docs,true)}<form id="document-upload" data-cat-id="${cat.id}"><label>${t('Rodzaj dokumentu','Document type')}<select name="kind">${['PEDIGREE','GENETIC_TEST','AWARD','HEALTH','OTHER'].map(k=>`<option value="${k}">${docKind(k)}</option>`).join('')}</select></label><label>${t('Plik PDF, PNG lub JPEG (do 5 MB)','PDF, PNG or JPEG file (up to 5 MB)')}<input type="file" name="file" accept="application/pdf,image/png,image/jpeg" required></label><p>${t('Nowy dokument jest prywatny. Dodanie pliku nie potwierdza jego autentyczności.','New documents are private. Uploading a file does not verify its authenticity.')}</p><button class="button primary" type="submit">${t('Dodaj dokument','Upload document')}</button></form></section>`;
  }
  async function conversations(id) {
    const list=await api('/chats?page='+listPage+'&size=20');
    const chat=id?await api('/chats/'+id):list.items[0];
    const names=new Map();
    await Promise.all(list.items.map(async c=>{const ownerId=c.firstOwnerId===user.id?c.secondOwnerId:c.firstOwnerId;try{names.set(c.id,(await api('/owners/'+ownerId)).kennel);}catch(e){if(e.status!==404)throw e;names.set(c.id,t('Hodowca ','Breeder ')+ownerId);}}));
    const history=chat?await api('/chats/'+chat.id+'/messages?page='+chatPage+'&size=50'):null;
    return title(t('Rozmowy','Conversations'),t('Wiadomości zapisują się na serwerze. Odśwież rozmowę, aby zobaczyć nowe odpowiedzi.','Messages are stored on the server. Refresh the conversation to see new replies.'))+`<section class="chat-layout"><div class="chat-list">${list.items.map(c=>`<a class="conversation-row ${c.id===chat?.id?'selected':''}" href="#messages/${c.id}"><span class="initials">${esc(names.get(c.id)?.slice(0,1))}</span><strong>${esc(names.get(c.id))}</strong></a>`).join('')||t('Brak rozmów. Otwórz profil kota, aby skontaktować się z hodowcą.','No conversations. Open a cat profile to contact a breeder.')}</div><div class="chat-pane">${chat?`<div class="chat-header"><strong>${esc(names.get(chat.id)||t('Rozmowa','Conversation'))}</strong><button type="button" class="button secondary" data-refresh>${t('Odśwież','Refresh')}</button></div><div class="messages" role="log">${history.items.map(m=>`<div class="bubble ${m.authorId===user.id?'mine':''}">${esc(m.text)}<small>${esc(new Date(m.createdAt).toLocaleString(language==='pl'?'pl-PL':'en-GB'))}</small></div>`).join('')||t('Przywitaj się z hodowcą.','Say hello to the breeder.')}</div><form id="live-message-form" class="chat-compose" data-chat-id="${chat.id}"><input name="text" required maxlength="4000" aria-label="${t('Treść wiadomości','Message text')}" placeholder="${t('Napisz wiadomość…','Write a message…')}"><button class="button primary" type="submit">${t('Wyślij','Send')}</button></form>`:''}</div></section>`+(history?pager(history,'messages'):'')+pager(list,'cats');
  }
  function settings() {
    return title(t('Twoje konto i hodowla','Your account and cattery'))+`<form id="breeder-form" class="panel live-form"><h2>${t('Profil hodowli','Cattery profile')}</h2>${formError}${field('kennel',t('Nazwa hodowli','Cattery name'),'text',breeder?.kennel||'','required maxlength="120"')}${field('city',t('Miasto','City'),'text',breeder?.city||'','required maxlength="100"')}${field('country',t('Kraj','Country'),'text',breeder?.country||'','required maxlength="100"')}<label>${t('Opis hodowli','Cattery description')}<textarea name="bio" maxlength="2000">${esc(breeder?.bio)}</textarea></label><button type="submit" class="button primary">${t('Zapisz hodowlę','Save cattery')}</button></form><form id="account-form" class="panel live-form live-page"><h2>${t('Dane konta','Account details')}</h2>${field('firstName',t('Imię','First name'),'text',user.firstName,'required maxlength="80"')}${field('surname',t('Nazwisko','Surname'),'text',user.surname,'required maxlength="80"')}${field('email',t('Adres e-mail','Email address'),'email',user.email,'required maxlength="254"')}<button class="button primary" type="submit">${t('Zapisz dane konta','Save account details')}</button></form><div class="live-tools"><button type="button" class="button secondary" data-logout>${t('Wyloguj się','Sign out')}</button></div>`;
  }
  async function loadBreeder(){try {breeder=await api('/owners/me');}catch(e){if(e.status!==404)throw e;breeder=null;}}
  document.addEventListener('click',async e=>{
    const el=e.target.closest('button');if(!el)return;
    if(el.dataset.language){ language=el.dataset.language;try{localStorage.setItem('matchcats-language',language);}catch{}render(); }
    if(el.id==='top-profile')location.hash='settings';
    if(el.hasAttribute('data-logout'))try {el.disabled=true;await api('/auth/logout',{method:'POST'});MatchCatsAPI.resetCSRF();user=null;breeder=null;location.hash='login';await render();}catch(error){showError(error);el.disabled=false;}
    if(el.hasAttribute('data-page')){if(el.dataset.pageType==='messages')chatPage=Number(el.dataset.page);else listPage=Number(el.dataset.page);render();}
    if(el.hasAttribute('data-refresh'))render();
    if(el.hasAttribute('data-clear-search')){searchFilters={};listPage=0;render();}
    try {
      if(el.dataset.contact){el.disabled=true;const chat=await api('/cats/'+el.dataset.contact+'/contact',{method:'POST'});chatPage=0;location.hash='messages/'+chat.id;}
      if(el.dataset.pairSource){el.disabled=true;const pair=await api('/cats/'+el.dataset.pairSource+'/matches',{method:'POST',body:{candidateId:Number(el.dataset.pairCandidate)}});location.hash='messages/'+pair.conversationId;}
      if(el.dataset.deleteCat && confirm(t('Usunąć kota i jego dokumenty?','Delete this cat and its documents?'))){await api('/cats/'+el.dataset.deleteCat,{method:'DELETE'});location.hash='cats';}
      if(el.dataset.download){const blob=await api('/documents/'+el.dataset.download+'/download',{binary:true});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='document-'+el.dataset.download+({ 'application/pdf':'.pdf','image/png':'.png','image/jpeg':'.jpg'}[blob.type]||'');a.click();setTimeout(()=>URL.revokeObjectURL(url),10000);}
      if(el.dataset.share){await api('/documents/'+el.dataset.share+'/visibility',{method:'PUT',body:{visibility:el.dataset.visibility}});await render();}
      if(el.dataset.deleteDocument && confirm(t('Usunąć ten dokument?','Delete this document?'))){await api('/documents/'+el.dataset.deleteDocument,{method:'DELETE'});await render();}
    }catch(error){showError(error);el.disabled=false;}
  });
  document.addEventListener('change',e=>{if(e.target.hasAttribute('data-show-password')) main.querySelectorAll('input[name="password"],input[name="confirm"]').forEach(input=>input.type=e.target.checked?'text':'password');});
  document.addEventListener('submit',async e=>{
    const form=e.target;if(!['login-form','register-form','breeder-form','account-form','live-cat-form','live-search','live-message-form','document-upload'].includes(form.id))return;
    e.preventDefault();
    if(!form.checkValidity()){form.reportValidity();return;}
    const data=Object.fromEntries(new FormData(form));const submit=form.querySelector('[type="submit"]');
    flash='';
    if(form.id==='register-form') {
      if(!/^[a-zA-Z0-9._-]{3,40}$/.test(data.username)){showError(t('Login może zawierać 3-40 liter, cyfr i znaków . _ -','Username must contain 3-40 letters, digits or . _ -'));return;}
      if(new TextEncoder().encode(data.password).length>72){showError(t('Hasło przekracza 72 bajty. Skróć je.','Password exceeds 72 bytes. Shorten it.'));return;}
      if(data.password!==data.confirm){showError(t('Hasła nie są takie same.','Passwords do not match.'));return;}delete data.confirm;
    }
    submit.disabled=true;form.setAttribute('aria-busy','true');
    try {
      if(form.id==='register-form'){await api('/users',{method:'POST',body:data});location.hash='login';await render(t('Konto zostało utworzone. Możesz się zalogować.','Your account has been created. You can sign in.'));}
      else if(form.id==='login-form'){user=await api('/auth/login',{method:'POST',body:data});MatchCatsAPI.resetCSRF();await loadBreeder();location.hash=breeder?'dashboard':'settings';await render();}
      else if(form.id==='breeder-form'){breeder=await api('/owners/me',{method:'PUT',body:data});await render();}
      else if(form.id==='account-form'){user=await api('/users/me',{method:'PUT',body:data});await render();}
      else if(form.id==='live-search'){searchFilters={breed:data.breed.trim(),sex:data.sex,city:data.city.trim(),available:data.available?'true':''};listPage=0;await render();}
      else if(form.id==='live-cat-form') {
        data.available=!!data.available;
        if(data.available && data.health!=='HEALTHY'){showError(t('Dostępność wymaga zadeklarowania stanu zdrowia jako zdrowy.','Availability requires owner-declared healthy status.'));return;}
        const cat=editingCat?await api('/cats/'+editingCat.id,{method:'PUT',body:{version:editingCat.version,profile:data}}):await api('/cats',{method:'POST',body:data});
        editingCat=null;location.hash='cat/'+cat.id;
      }
      else if(form.id==='live-message-form'){if(!data.text.trim()){showError(t('Wpisz treść wiadomości.','Enter a message.'));return;}await api('/chats/'+form.dataset.chatId+'/messages',{method:'POST',body:{text:data.text.trim()}});await render();}
      else if(form.id==='document-upload'){if(data.file.size>5*1024*1024){showError(message({code:'FILE_TOO_LARGE'}));return;}await api('/cats/'+form.dataset.catId+'/documents',{method:'POST',body:new FormData(form)});await render();}
    }catch(error){showError(error);}finally{submit.disabled=false;form.removeAttribute('aria-busy');}
  });
  window.addEventListener('hashchange',()=>{listPage=0;chatPage=0;render();window.scrollTo(0,0);});
  async function start() {
    shell('login');main.innerHTML=`<div class="live-loading" role="status">${t('Sprawdzanie sesji…','Checking your session…')}</div>`;
    try {user=await api('/users/me');await loadBreeder();await render();}
    catch(error){user=null;await render();if(error.status!==401)showError(error);}
  }
  start();
})();
