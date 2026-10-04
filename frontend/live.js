'use strict';
if (new URLSearchParams(location.search).get('demo') !== '1') (() => {
  const api = MatchCatsAPI.request;
  const main = document.querySelector('#main');
  let user = null, breeder = null, language = 'pl', revision = 0, flash = '', flashRoute = '';
  let searchFilters = {}, listPage = 0, chatPage = 0;
  let unreadCount=0, unreadBusy=false, unreadAgain=false;
  const errorPage=error=>MatchCatsErrors.page(error,{t,esc,button});
  let safetyCapabilities={moderator:false,moderationConfigured:false};
  try { language = localStorage.getItem('matchcats-language') === 'en' ? 'en' : 'pl'; } catch {}
  const t = (pl, en) => language === 'en' ? en : pl;
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const button = (label, href, style='primary') => `<a class="button ${style}" href="#${href}">${label}</a>`;
  const title = (heading, description='') => `<div class="heading"><div><h1>${esc(heading)}</h1><p class="muted">${esc(description)}</p></div></div>`;
  const field = (name, label, type='text', value='', attrs='') => `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${attrs}></label>`;
  const formError = '<div class="live-error" role="alert" id="live-error" hidden></div>';
  function icon(name) {
    const shapes={dashboard:'<path d="m3 10 9-7 9 7v10H15v-7H9v7H3Z"/>',cats:'<ellipse cx="12" cy="16" rx="6" ry="4"/><ellipse cx="5" cy="9" rx="2" ry="3"/><ellipse cx="10" cy="6" rx="2" ry="3"/><ellipse cx="16" cy="6" rx="2" ry="3"/><ellipse cx="20" cy="10" rx="2" ry="3"/>',search:'<circle cx="10.5" cy="10.5" r="7"/><path d="m16 16 5 5"/>',messages:'<path d="M21 11a8 8 0 0 1-8 8H8l-5 3V11a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8Z"/><path d="M7 10h10M7 14h6"/>',notifications:'<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9Z"/><path d="M10 21h4"/>',documents:'<path d="M5 3h10l4 4v14H5Z M15 3v5h4M8 12h8M8 16h8"/>',settings:'<circle cx="12" cy="12" r="4"/><path d="M12 2v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2"/>'};
    return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+(shapes[name]||shapes.cats)+'</svg>';
  }
  function message(error) {
    const messages = {
      PROPOSAL_CHANGED:t('Propozycja ma już inną odpowiedź. Odśwież listę.','This proposal already has a different response. Refresh the list.'),
      CONTACT_BLOCKED:t('Kontakt jest zablokowany lub konto jest niedostępne.','Contact is blocked or the account is unavailable.'),
      ACCOUNT_SUSPENDED:t('Dostęp do konta jest zawieszony.','Account access has been suspended.'),
      REPORT_REVIEWED:t('Zgłoszenie zostało już rozpatrzone. Odśwież kolejkę.','This report has already been reviewed. Refresh the queue.'),
      NETWORK:t('Nie można połączyć się z serwerem. Sprawdź połączenie i spróbuj ponownie.','Cannot reach the server. Check your connection and try again.'),
      INVALID_CREDENTIALS:t('Nieprawidłowy login lub hasło.','Invalid username or password.'),
      INVALID_CURRENT_PASSWORD:t('Obecne hasło jest nieprawidłowe.','Your current password is incorrect.'),
      INVALID_RESET_LINK:t('Link jest nieprawidłowy, wykorzystany lub wygasł. Poproś o nowy link.','This link is invalid, already used or expired. Request a new link.'),
      INVALID_VERIFICATION_LINK:t('Link potwierdzający jest nieprawidłowy, wykorzystany lub wygasł. Zaloguj się i wyślij nowy link w ustawieniach.','This verification link is invalid, already used or expired. Sign in and request a new link in Settings.'),
      EMAIL_UNAVAILABLE:t('Wysyłka e-maili nie jest jeszcze skonfigurowana lub jest chwilowo niedostępna. Spróbuj później.','Email delivery is not configured yet or is temporarily unavailable. Please try later.'),
      PAYMENT_NOT_CONFIGURED:t('Płatności Premium nie są jeszcze uruchomione.','Premium payments are not configured yet.'),
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
  function showError(error,target=main) {
    if(!target.isConnected)target=main;
    if(error.status===401 && user) { MatchCatsAPI.resetCSRF();user=null;breeder=null;unreadCount=0;location.hash='login';render(t('Sesja wygasła. Zaloguj się ponownie.','Your session expired. Sign in again.'));return; }
    let box=target.querySelector('.live-error');
    if(!box) {box=document.createElement('div');box.className='live-error';box.setAttribute('role','alert');target.prepend(box);}
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
    const nav=[['dashboard',t('Start','Home')],['cats',t('Moje koty','My cats')],['search',t('Szukaj kota','Find a cat')],['proposals',t('Propozycje','Proposals')],['messages',t('Rozmowy','Conversations')],['notifications',t('Powiadomienia','Notifications')],['documents',t('Dokumenty','Documents')],['settings',t('Ustawienia','Settings')]];
    const current=nav.find(x=>x[0]===route)?.[1]||t('Profil','Profile');
    document.querySelector('#breadcrumb').textContent=current;
    document.title=`MatchCats · ${user?current:t('Twoje konto','Your account')}`;
    document.querySelector('#navigation').innerHTML=user?nav.map(([key,label])=>`<a href="#${key}" class="nav-item ${key===route?'active':''}" ${key===route?'aria-current="page"':''}><span class="nav-icon" aria-hidden="true">${icon(key)}</span><span class="nav-text">${label}</span>${key==='messages'?unreadBadge():''}</a>`).join(''):'';
    document.querySelector('#top-profile').textContent=user?.firstName?.slice(0,1)||'•';
    document.querySelector('#top-profile').setAttribute('aria-label',t('Ustawienia konta','Account settings'));
  }
  function auth(mode, notice='') {
    const register=mode==='register';
    return `<div class="auth-layout"><section class="auth-intro"><h1>${t('Poznaj koty.<br>Poznaj ich hodowców.','Meet cats.<br>Meet their breeders.')}</h1><p class="muted">${t('Twoje koty, dokumenty i rozmowy w jednej przestrzeni.','Your cats, documents and conversations in one place.')}</p><img src="assets/sky-hero.jpg" alt=""><p class="live-caption">${t('Dekoracyjne zdjęcie wygenerowane dla MatchCats.','Decorative image generated for MatchCats.')}</p></section><section class="auth-panel"><h1>${register?t('Utwórz konto','Create an account'):t('Zaloguj się','Sign in')}</h1><p class="muted">${register?t('Dołącz do hodowców MatchCats.','Join MatchCats breeders.'):t('Witaj ponownie w swojej hodowli.','Welcome back to your cattery.')}</p>${notice?`<p class="live-success" role="status">${esc(notice)}</p>`:''}<form id="${register?'register':'login'}-form" novalidate>${formError}${field('username',t('Login','Username'),'text','','required minlength="3" maxlength="40" autocomplete="username" autocapitalize="none" spellcheck="false"')}${register?field('email',t('Adres e-mail','Email address'),'email','','required maxlength="254" autocomplete="email"')+field('firstName',t('Imię','First name'),'text','','required maxlength="80" autocomplete="given-name"')+field('surname',t('Nazwisko','Surname'),'text','','required maxlength="80" autocomplete="family-name"'):''}${field('password',t('Hasło','Password'),'password','','required '+(register?'minlength="8" maxlength="72" autocomplete="new-password"':'autocomplete="current-password"'))}${register?`<p class="muted live-caption">${t('Minimum 8 znaków. Maksymalnie 72 bajty UTF-8. Login: 3-40 liter, cyfr lub znaków . _ -','At least 8 characters. Maximum 72 UTF-8 bytes. Username: 3-40 letters, digits or . _ -') }</p>`+field('confirm',t('Powtórz hasło','Repeat password'),'password','','required autocomplete="new-password"'):''}<label class="check"><input type="checkbox" data-show-password>${t('Pokaż hasło','Show password')}</label><button class="button primary" type="submit">${register?t('Utwórz konto','Create account'):t('Zaloguj się','Sign in')}</button></form><a class="auth-link" href="#${register?'login':'register'}">${register?t('Mam już konto - zaloguj się','Already have an account? Sign in'):t('Nie masz konta? Zarejestruj się','No account? Register')}</a>${register?'':`<a class="auth-link" href="#forgot">${t('Nie pamiętasz hasła?','Forgot your password?')}</a>`}<a class="auth-link" href="/?demo=1">${t('Zobacz demonstrację bez konta','Explore the demo without an account')}</a><p class="live-caption"><a href="#plans">${t('Plany','Plans')}</a> · <a href="#privacy">${t('Prywatność','Privacy')}</a> · <a href="#terms">${t('Regulamin','Terms')}</a></p></section></div>`;
  }
  async function render(notice='') {
    const ownRevision=++revision;
    const [route='dashboard',id]=location.hash.slice(1).split('/');shell(route);
    const flashKey=location.hash.slice(1);
    if(notice){flash=notice;flashRoute=flashKey;}else if(flashRoute!==flashKey)flash='';
    if(route==='privacy'||route==='terms'){document.body.classList.add('signed-out');main.innerHTML=MatchCatsLegal.page(route,language,!!user);activateForms();return;}
    if(route==='plans'){document.body.classList.toggle('signed-out',!user);main.innerHTML=MatchCatsPlans.page(language,undefined,!!user);activateForms();if(user){try{const plan=await api('/billing/me');if(ownRevision!==revision)return;const note=main.querySelector('.plans-page .notice');if(note)note.textContent=t('Twój plan: '+plan.planId,'Your plan: '+plan.planId);}catch{}}return;}
    if(route==='forgot' || route==='reset') {document.body.classList.add('signed-out');main.innerHTML=recoveryForm(route,id);activateForms();return;}
    if(route==='verify') {main.innerHTML=verificationForm(id);activateForms();return;}
    if(route==='404'){main.innerHTML=errorPage({status:404});return;}
    if(!user){ main.innerHTML=auth(route==='register'?'register':'login',flash);activateForms();return; }
    main.innerHTML=`<div class="live-loading" role="status">${t('Wczytywanie…','Loading…')}</div>`;
    try {
      const result=await view(route,id);
      if(ownRevision===revision) {main.innerHTML=(flash?`<p class="live-success" role="status">${esc(flash)}</p>`:'')+result;activateForms();await acknowledgeMessages();await refreshUnread();}
    } catch(error) { if(ownRevision===revision){if([403,404].includes(error.status)||error.status>=500||error.code==='NETWORK'){main.innerHTML=errorPage(error);}else {main.innerHTML=button(t('Spróbuj ponownie','Try again'),route+(id?'/'+id:''));showError(error);}} }
  }
  function activateForms(){main.querySelectorAll('form').forEach(form=>form.noValidate=true);}
  function recoveryForm(route,token) {
    const reset=route==='reset';
    return `<section class="auth-panel live-form" style="margin:30px auto">${title(reset?t('Ustaw nowe hasło','Set a new password'):t('Nie pamiętasz hasła?','Forgot your password?'))}<p>${reset?t('Link działa przez 15 minut i można użyć go tylko raz.','The link is valid for 15 minutes and can only be used once.'):t('Podaj adres e-mail konta. Jeśli konto istnieje, wyślemy link do zmiany hasła.','Enter your account email. If an account exists, we will send a password reset link.')}</p>${flash?`<p class="live-success" role="status">${esc(flash)}</p>`:''}<form id="${reset?'reset-password':'forgot-password'}-form" data-token="${esc(token||'')}">${formError}${reset?field('password',t('Nowe hasło','New password'),'password','','required minlength="8" maxlength="72" autocomplete="new-password"')+field('confirm',t('Powtórz hasło','Repeat password'),'password','','required autocomplete="new-password"'):field('email',t('Adres e-mail','Email address'),'email','','required maxlength="254" autocomplete="email"')}<button class="button primary" type="submit">${reset?t('Zapisz nowe hasło','Save new password'):t('Wyślij link','Send link')}</button></form><a class="auth-link" href="#login">${t('Wróć do logowania','Back to sign-in')}</a></section>`;
  }
  function verificationForm(token) {
    return `<section class="auth-panel live-form" style="margin:30px auto">${title(t('Potwierdź adres e-mail','Verify your email'))}<p>${t('Naciśnij przycisk, aby potwierdzić adres konta, dla którego wysłano ten link. Link działa przez 24 godziny i można użyć go tylko raz.','Press the button to verify the account email this link was sent for. The link is valid for 24 hours and can only be used once.')}</p><p class="muted">${t('To potwierdzenie skrzynki e-mail, a nie hodowli ani dokumentów kota.','This confirms access to the email inbox, not the cattery or cat documents.')}</p>${flash?`<p class="live-success" role="status">${esc(flash)}</p>`:`<form id="verify-email-form" data-token="${esc(token||'')}">${formError}<button class="button primary" type="submit">${t('Potwierdź adres','Confirm email')}</button></form>`}${button(user?t('Wróć do ustawień','Back to Settings'):t('Przejdź do logowania','Go to sign-in'),user?'settings':'login','secondary')}</section>`;
  }
  function emailStatus() {
    return `<section class="panel live-form live-page" id="email-status"><h2>${t('Potwierdzenie adresu e-mail','Email verification')}</h2><p>${esc(user.email)}</p><p>${user.emailVerified?t('Adres e-mail potwierdzony.','Email address verified.'):t('Adres e-mail nie jest jeszcze potwierdzony.','Email address is not verified yet.')}</p><p class="muted">${t('Potwierdzenie dotyczy skrzynki pocztowej. Nie potwierdza hodowli ani dokumentów. Zmiana adresu wymaga nowego potwierdzenia.','Verification confirms access to the inbox. It does not verify the cattery or documents. Changing your email requires verification again.')}</p>${user.emailVerified?'':`<form id="request-email-form">${formError}<button class="button primary" type="submit">${t('Wyślij link potwierdzający','Send verification link')}</button><p class="live-caption">${t('Wymaga działającej usługi e-mail. Nowy link zastępuje poprzedni; ponowna wysyłka najwcześniej po minucie.','Requires a configured email service. A new link replaces the previous one; wait at least one minute before resending.')}</p></form>`}</section>`;
  }
  async function view(route,id) {
    if(['safety','report','moderation'].includes(route))return MatchCatsSafety.render(route,id,{api,t,esc,title,button,capabilities:safetyCapabilities,page:listPage});
    if(!breeder && route!=='settings') return title(t('Uzupełnij swoją hodowlę','Complete your cattery'))+`<section class="panel"><p>${t('Zapisz informacje o hodowli, zanim dodasz kota lub skontaktujesz się z innym hodowcą.','Save your cattery information before adding a cat or contacting another breeder.')}</p>${button(t('Uzupełnij profil','Complete profile'),'settings')}</section>`;
    if(route==='settings') return settings();
    if(route==='notifications') return notifications();
    if(route==='cats' || route==='search' || route==='dashboard') return catList(route);
    if(route==='cat') return catDetail(id);
    if(route==='new-cat' || route==='edit-cat') return catForm(route==='edit-cat'?await api('/cats/'+id):null);
    if(route==='messages') return conversations(id);
    if(route==='proposals') {const result=await api('/matches?page='+listPage+'&size=20');return title(t('Propozycje par','Pair proposals'),t('Zgoda obu hodowców na dalsze planowanie. Każda osoba może ją później wycofać.','Both breeders agree to further planning. Either person may later withdraw consent.'))+`<div class="live-tools"><button class="button secondary" data-refresh>${t('Odśwież','Refresh')}</button></div><p class="live-caption">${t('Akceptacja nie potwierdza bezpieczeństwa genetycznego ani weterynaryjnego. Sprawdź dokumenty i skonsultuj plan z weterynarzem. Odrzucone i wycofane propozycje nie otwierają się ponownie.','Acceptance does not confirm genetic or veterinary suitability. Review documents and consult a veterinarian. Declined and withdrawn proposals do not reopen.')}</p>`+result.items.map(proposalCard).join('')+(result.items.length?'':`<section class="panel">${t('Nie masz jeszcze propozycji. Wyślij pierwszą z listy kandydatów swojego kota.','No proposals yet. Send one from your cat’s candidate list.')}</section>`)+pager(result,'proposals');}
    if(route==='documents') return documentPage(id);
    if(route==='candidates') {
      const result=await api('/cats/'+id+'/candidates?page='+listPage+'&size=20');
      const saved=await api('/cats/'+id+'/matches?size=100');
      return title(t('Kandydaci do rozmowy','Candidates for discussion'),t('Ta sama rasa i przeciwna płeć. To nie jest ocena genetyczna.','Same breed and opposite sex. This is not a genetic assessment.'))+`<div class="cat-grid">${result.items.map(c=>`<section>${card(c)}<button type="button" class="button primary" data-pair-source="${Number(id)}" data-pair-candidate="${c.id}">${t('Zapisz propozycję i otwórz rozmowę','Save proposal and open conversation')}</button></section>`).join('')||t('Brak dostępnych kandydatów.','No available candidates.')}</div>`+pager(result,'cats')+`<section class="panel"><h2>${t('Zapisane propozycje','Saved proposals')}</h2>${button(t('Zobacz wszystkie propozycje i odpowiedzi','View all proposals and responses'),'proposals','secondary')}${saved.items.map(proposalCard).join('')||t('Brak zapisanych propozycji.','No saved proposals.')}</section>`;
    }
    return errorPage({status:404});
  }
  function proposalCard(p) {
    const state=p.proposedBy===null?t('Starsza propozycja — wymaga ponownego wysłania z listy kandydatów','Older proposal — send again from the candidate list'):{PENDING:t('Oczekuje na odpowiedź','Awaiting response'),ACCEPTED:t('Zaakceptowana przez obu hodowców','Accepted by both breeders'),DECLINED:t('Odrzucona','Declined'),WITHDRAWN:t('Wycofana','Withdrawn')}[p.status];
    const action=(value,label)=>`<button class="button ${value==='ACCEPT'?'primary':'secondary'}" data-proposal="${p.id}" data-proposal-action="${value}">${label}</button>`;
    return `<article class="panel proposal-card" data-proposal-id="${p.id}" data-proposal-status="${p.status}"><h2>${t('Propozycja','Proposal')} #${p.id}</h2><p>${button(t('Kot','Cat')+' #'+p.firstCatId,'cat/'+p.firstCatId,'secondary')} ${button(t('Kot','Cat')+' #'+p.secondCatId,'cat/'+p.secondCatId,'secondary')}</p><p class="proposal-status">${esc(state)}</p><p class="muted">${p.proposedBy===null?'':p.proposedBy===user.id?t('Wysłana przez Ciebie','Sent by you'):t('Otrzymana od drugiego hodowcy','Received from the other breeder')}</p><div class="live-tools">${p.canDecide?action('ACCEPT',t('Akceptuję propozycję','Accept proposal'))+action('DECLINE',t('Odrzuć','Decline')):''}${p.canWithdraw?action('WITHDRAW',t('Wycofaj zgodę / propozycję','Withdraw consent / proposal')):''}${button(t('Otwórz rozmowę','Open conversation'),'messages/'+p.conversationId,'secondary')}</div></article>`;
  }
  const sex = value => value==='MALE'?t('Kocur','Male'):t('Kotka','Female');
  const health = value => ({UNKNOWN:t('Niepodane','Unknown'),HEALTHY:t('Zdrowy według właściciela','Owner-declared healthy'),SICK:t('Chory według właściciela','Owner-declared sick')})[value];
  const photo = cat => cat.hasPhoto ? `<div class="cat-picture"><img class="live-cat-photo" src="/cats/${cat.id}/photo" alt="${esc(cat.name)}"></div>` : `<div class="cat-picture live-photo-empty" role="img" aria-label="${t('Brak zdjęcia','No photo')}">${icon('cats')}</div>`;
  function photoForm(cat) {
    return `<form id="photo-upload" data-cat-id="${cat.id}"><label>${t('Zdjęcie JPEG lub PNG (do 5 MB, 24 megapikseli)','JPEG or PNG photo (up to 5 MB, 24 megapixels)')}<input type="file" name="file" accept="image/png,image/jpeg" required></label><button class="button primary" type="submit">${t('Zapisz zdjęcie','Save photo')}</button>${cat.hasPhoto?`<button class="button secondary" type="button" data-delete-photo="${cat.id}">${t('Usuń zdjęcie','Remove photo')}</button>`:''}</form>`;
  }
  function card(cat) {
    return `<article class="cat-card">${photo(cat)}<div class="cat-body"><div class="cat-title"><h3>${esc(cat.name)}</h3><span>${sex(cat.sex)}</span></div><p class="cat-meta">${esc(cat.breed)} · ${esc(cat.city)}</p><p>${cat.available?t('Dostępny do kontaktu','Available for contact'):t('Obecnie niedostępny','Currently unavailable')}</p><div class="cat-bottom">${button(t('Zobacz profil','View profile'),'cat/'+cat.id,'secondary')}</div></div></article>`;
  }
  function pager(result, type) {
    return `<div class="live-pages"><button class="button secondary" data-page="${Math.max(0,result.page-1)}" data-page-type="${type}" ${result.page===0?'disabled':''}>${t('Poprzednie','Previous')}</button><span>${t('Strona','Page')} ${result.page+1} · ${t('Wyników','Results')}: ${result.total}</span><button class="button secondary" data-page="${result.page+1}" data-page-type="${type}" ${(result.page+1)*result.size>=result.total?'disabled':''}>${t('Następne','Next')}</button></div>`;
  }
  async function notifications() {
    const items=await api('/notifications?limit=50');
    const cards=items.map(item=>{
      const date=new Date(item.createdAt).toLocaleString(language==='en'?'en-US':'pl-PL');
      const notificationTitle=item.kind==='MESSAGE'?t('Nowa wiadomość','New message'):item.title;
      const notificationBody=item.kind==='MESSAGE'?t('Otrzymano nową wiadomość w MatchCats.','You received a new MatchCats message.'):item.body;
      const status=item.readAt?'':' notification-unread';
      const badge=item.readAt?'':'<span class="unread-badge">'+t('Nowe','New')+'</span>';
      const action=item.readAt?'':'<button type="button" class="button secondary" data-notification-read="'+item.id+'">'+t('Oznacz jako przeczytane','Mark as read')+'</button>';
      return '<article class="panel notification-card'+status+'"><div class="notification-heading"><div><h2>'+esc(notificationTitle)+'</h2><p class="muted">'+esc(date)+'</p></div>'+badge+'</div><p>'+esc(notificationBody)+'</p>'+action+'</article>';
    }).join('');
    return title(t('Powiadomienia','Notifications'),t('Ważne informacje dotyczące Twoich rozmów i konta.','Important updates about your conversations and account.'))+(cards||'<section class="panel">'+t('Nie masz jeszcze powiadomień.','You have no notifications yet.')+'</section>');
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
    const options=(values,selected,labels)=>values.map(v=>`<option value="${v}" ${v===selected?'selected':''}>${esc(labels(v))}</option>`).join('');
    return title(cat?t('Edytuj kota','Edit cat'):t('Dodaj kota','Add a cat'))+`<form id="live-cat-form" class="panel live-form" data-cat-id="${cat?.id||''}" data-version="${cat?.version??''}">${formError}${field('name',t('Imię kota','Cat name'),'text',cat?.name,'required maxlength="100"')}${field('breed',t('Rasa','Breed'),'text',cat?.breed,'required maxlength="100"')}<label>${t('Płeć','Sex')}<select name="sex">${options(['FEMALE','MALE'],cat?.sex,sex)}</select></label><label>${t('Stan zdrowia zadeklarowany przez właściciela','Owner-declared health')}<select name="health">${options(['UNKNOWN','HEALTHY','SICK'],cat?.health,health)}</select></label>${field('birthDate',t('Data urodzenia','Date of birth'),'date',cat?.birthDate,'required max="'+new Date().toLocaleDateString('sv-SE')+'"')}${field('city',t('Miasto','City'),'text',cat?.city||breeder.city,'required maxlength="100"')}${field('country',t('Kraj','Country'),'text',cat?.country||breeder.country,'required maxlength="100"')}<label>${t('Opis','Description')}<textarea name="description" maxlength="2000">${esc(cat?.description)}</textarea></label><label class="check"><input name="available" type="checkbox" ${cat?.available?'checked':''}>${t('Dostępny do kontaktu (wymaga deklaracji zdrowia)','Available for contact (requires healthy declaration)')}</label><div class="live-tools"><button class="button primary" type="submit">${t('Zapisz kota','Save cat')}</button>${button(t('Anuluj','Cancel'),'cats','secondary')}</div></form>`;
  }
  async function catDetail(id) {
    const cat=await api('/cats/'+id);const owner=await api('/owners/'+cat.ownerId);const docs=await api('/cats/'+id+'/documents');
    const own=cat.ownerId===user.id;
    const contact=own?null:await api('/safety/contact/'+cat.ownerId);
    return title(cat.name,cat.breed)+`<div class="detail-layout"><section class="panel">${photo(cat)}${own?photoForm(cat):''}<h2>${t('Dokumenty','Documents')}</h2>${documentsHTML(docs,own)}${own?button(t('Zarządzaj dokumentami','Manage documents'),'documents/'+id,'secondary'):''}</section><section class="detail-box"><h2>${esc(owner.kennel)}</h2><p>${esc(cat.city)} · ${esc(cat.country)}</p><p>${sex(cat.sex)} · ${esc(cat.birthDate)}</p><p>${health(cat.health)}</p><p>${esc(cat.description)}</p><p class="notice">${t('Profil i załączniki nie oznaczają niezależnej weryfikacji zdrowia, pochodzenia ani zgodności genetycznej.','A profile and attachments do not mean independent verification of health, ancestry or genetic compatibility.')}</p><div class="live-tools">${own?button(t('Edytuj profil','Edit profile'),'edit-cat/'+id,'secondary')+button(t('Znajdź kandydatów','Find candidates'),'candidates/'+id)+`<button type="button" class="button secondary danger" data-delete-cat="${cat.id}">${t('Usuń kota','Delete cat')}</button>`:`<button type="button" class="button primary" data-contact="${cat.id}" ${!cat.available||contact.contactBlocked?'disabled':''}>${t('Napisz do hodowcy','Contact breeder')}</button>${button(t('Zgłoś profil','Report profile'),'report/CAT:'+cat.id,'secondary')}${blockButton(cat.ownerId,contact.blockedByYou)}`}</div></section></div>`;
  }
  const docKind=kind=>({PEDIGREE:t('Rodowód','Pedigree'),GENETIC_TEST:t('Badanie genetyczne','Genetic test'),AWARD:t('Osiągnięcie','Award'),HEALTH:t('Zdrowie','Health'),OTHER:t('Inny','Other')})[kind];
  function documentsHTML(docs,own) {
    const review=value=>({OWNER_UPLOADED:t('Nie sprawdzono','Not reviewed'),REVIEW_REQUESTED:t('Oczekuje na sprawdzenie','Awaiting review'),VERIFIED:t('Sprawdzony','Verified'),REJECTED:t('Odrzucony','Rejected')})[value]||t('Nie sprawdzono','Not reviewed');
    return docs.map(d=>`<div class="live-file-row"><strong>${esc(d.filename)}<small>${docKind(d.kind)} · ${d.visibility==='PRIVATE'?t('Prywatny','Private'):t('Widoczny dla hodowców','Visible to breeders')} · ${review(d.verification)}</small>${own&&d.verificationNote?`<small>${t('Uzasadnienie','Explanation')}: ${esc(d.verificationNote)}</small>`:''}${own&&d.verificationAppealNote?`<small>${t('Twoje odwołanie','Your appeal')}: ${esc(d.verificationAppealNote)}</small>`:''}</strong><button type="button" class="button secondary" data-download="${d.id}">${t('Pobierz','Download')}</button>${own?(d.verification==='REJECTED'?`<button type="button" class="button secondary" data-appeal-document="${d.id}">${t('Odwołaj się','Appeal decision')}</button>`:`<button type="button" class="button secondary" data-request-verification="${d.id}" ${d.verification==='REVIEW_REQUESTED'||d.verification==='VERIFIED'?'disabled':''}>${t('Poproś o sprawdzenie','Request review')}</button>`)+`<button type="button" class="button secondary" data-share="${d.id}" data-visibility="${d.visibility==='PRIVATE'?'BREEDERS':'PRIVATE'}">${d.visibility==='PRIVATE'?t('Udostępnij hodowcom','Share with breeders'):t('Ustaw prywatny','Make private')}</button><button type="button" class="button secondary danger" data-delete-document="${d.id}">${t('Usuń','Delete')}</button>`:''}</div>`).join('')||`<p>${t('Brak widocznych dokumentów.','No visible documents.')}</p>`;
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
    const other=chat?(chat.firstOwnerId===user.id?chat.secondOwnerId:chat.firstOwnerId):null;
    const contact=chat?await api('/safety/contact/'+other):null;
    return title(t('Rozmowy','Conversations'),t('Wiadomości zapisują się na serwerze. Odśwież rozmowę, aby zobaczyć nowe odpowiedzi.','Messages are stored on the server. Refresh the conversation to see new replies.'))+`<section class="chat-layout"><div class="chat-list">${list.items.map(c=>`<a class="conversation-row ${c.id===chat?.id?'selected':''}" href="#messages/${c.id}"><span class="initials">${esc(names.get(c.id)?.slice(0,1))}</span><strong>${esc(names.get(c.id))}</strong>${c.unreadMessages?`<span class="unread-badge" data-chat-unread="${c.id}" aria-label="${t('Nieprzeczytane wiadomości: ','Unread messages: ')+c.unreadMessages}">${c.unreadMessages}</span>`:''}</a>`).join('')||t('Brak rozmów. Otwórz profil kota, aby skontaktować się z hodowcą.','No conversations. Open a cat profile to contact a breeder.')}</div><div class="chat-pane">${chat?`<div class="chat-header"><strong>${esc(names.get(chat.id)||t('Rozmowa','Conversation'))}</strong><button type="button" class="button secondary" data-refresh>${t('Odśwież','Refresh')}</button></div><div class="messages" role="log">${history.items.map(m=>`<div data-message-id="${m.id}" ${m.authorId!==user.id?'data-received':''} class="bubble ${m.authorId===user.id?'mine':''}">${esc(m.text)}<small>${esc(new Date(m.createdAt).toLocaleString(language==='pl'?'pl-PL':'en-GB'))}</small>${m.authorId!==user.id?button(t('Zgłoś','Report'),'report/MESSAGE:'+m.id,'subtle'):''}</div>`).join('')||t('Przywitaj się z hodowcą.','Say hello to the breeder.')}</div>${chat.contactBlocked?`<p class="notice">${t('Kontakt jest zablokowany lub konto jest niedostępne. Historia pozostaje dostępna.','Contact is blocked or the account is unavailable. History remains accessible.')}</p>`:''}<div class="live-tools">${blockButton(other,contact.blockedByYou)}</div><form id="live-message-form" class="chat-compose" data-chat-id="${chat.id}"><input ${chat.contactBlocked?'disabled':''} name="text" required maxlength="4000" aria-label="${t('Treść wiadomości','Message text')}" placeholder="${t('Napisz wiadomość…','Write a message…')}"><button ${chat.contactBlocked?'disabled':''} class="button primary" type="submit">${t('Wyślij','Send')}</button></form>`:''}</div></section>`+(history?pager(history,'messages'):'')+pager(list,'cats');
  }
  function unreadBadge(){return `<span class="unread-badge" data-unread-total ${unreadCount?'':'hidden'} aria-label="${t('Nieprzeczytane wiadomości: ','Unread messages: ')+unreadCount}">${unreadCount>99?'99+':unreadCount}</span>`;}
  async function refreshUnread(){
    if(!user||document.hidden)return;
    if(unreadBusy){unreadAgain=true;return;}
    const account=user.id;unreadBusy=true;
    try{const summary=await api('/chats/unread');if(user?.id!==account)return;unreadCount=summary.unreadMessages;const badge=document.querySelector('[data-unread-total]');if(badge){badge.textContent=unreadCount>99?'99+':unreadCount;badge.hidden=unreadCount===0;badge.setAttribute('aria-label',t('Nieprzeczytane wiadomości: ','Unread messages: ')+unreadCount);}}
    catch(error){if(user?.id===account&&error.status===401)showError(error);}
    finally{unreadBusy=false;if(unreadAgain){unreadAgain=false;refreshUnread();}}
  }
  async function acknowledgeMessages(){
    if(document.hidden||!user)return;
    const form=main.querySelector('#live-message-form');if(!form)return;
    const messageIds=[...main.querySelector('[role="log"]').querySelectorAll('[data-received]')].map(el=>Number(el.dataset.messageId));
    if(!messageIds.length)return;
    try{await api('/chats/'+form.dataset.chatId+'/read',{method:'POST',body:{messageIds}});const updated=await api('/chats/'+form.dataset.chatId);if(!form.isConnected)return;const badge=main.querySelector('[data-chat-unread="'+form.dataset.chatId+'"]');if(badge){badge.hidden=updated.unreadMessages===0;badge.textContent=String(updated.unreadMessages);badge.setAttribute('aria-label',t('Nieprzeczytane wiadomości: ','Unread messages: ')+updated.unreadMessages);}}
    catch(error){if(form.isConnected)showError(t('Nie udało się zapisać odczytu wiadomości. Odśwież rozmowę.','Could not save message read status. Refresh the conversation.'));}
  }
  function settings() {
    return title(t('Twoje konto i hodowla','Your account and cattery'))+`<form id="breeder-form" class="panel live-form"><h2>${t('Profil hodowli','Cattery profile')}</h2>${formError}${field('kennel',t('Nazwa hodowli','Cattery name'),'text',breeder?.kennel||'','required maxlength="120"')}${field('city',t('Miasto','City'),'text',breeder?.city||'','required maxlength="100"')}${field('country',t('Kraj','Country'),'text',breeder?.country||'','required maxlength="100"')}<label>${t('Opis hodowli','Cattery description')}<textarea name="bio" maxlength="2000">${esc(breeder?.bio)}</textarea></label><button type="submit" class="button primary">${t('Zapisz hodowlę','Save cattery')}</button></form><form id="account-form" class="panel live-form live-page"><h2>${t('Dane konta','Account details')}</h2>${field('firstName',t('Imię','First name'),'text',user.firstName,'required maxlength="80"')}${field('surname',t('Nazwisko','Surname'),'text',user.surname,'required maxlength="80"')}${field('email',t('Adres e-mail','Email address'),'email',user.email,'required maxlength="254"')}${field('currentPassword',t('Obecne hasło (wymagane przy zmianie e-maila)','Current password (required when changing email)'),'password','','autocomplete="current-password"')}<button class="button primary" type="submit">${t('Zapisz dane konta','Save account details')}</button></form>${emailStatus()}${accountSecurity()}<div class="live-tools">${button(t('Bezpieczeństwo: zgłoszenia i blokady','Safety: reports and blocks'),'safety','secondary')}${safetyCapabilities.moderator?button(t('Panel moderatora','Moderator panel'),'moderation','secondary'):''}<button type="button" class="button secondary" data-logout>${t('Wyloguj się','Sign out')}</button></div>`;
  }
  function accountSecurity() {
    return `<form id="password-form" class="panel live-form live-page"><h2>${t('Zmień hasło','Change password')}</h2>${field('currentPassword',t('Obecne hasło','Current password'),'password','','required autocomplete="current-password"')}${field('newPassword',t('Nowe hasło','New password'),'password','','required minlength="8" maxlength="72" autocomplete="new-password"')}${field('confirmPassword',t('Powtórz nowe hasło','Repeat new password'),'password','','required autocomplete="new-password"')}<p>${t('Minimum 8 znaków, maksymalnie 72 bajty. Zmiana wyloguje wszystkie sesje konta.','At least 8 characters, maximum 72 bytes. Changing your password signs out all account sessions.')}</p><button type="submit" class="button primary">${t('Zmień hasło','Change password')}</button></form><form id="delete-account-form" class="panel live-form live-page"><h2>${t('Usuń konto','Delete account')}</h2><p class="notice">${t('Usunięcie konta usuwa również Twoje koty, zdjęcia, dokumenty i rozmowy, także ich historię widoczną u drugiego uczestnika. Zgłoszone treści mogą pozostać w prywatnych dokumentach moderacji do 90 dni. Tej operacji nie można cofnąć.','Deleting your account also removes your cats, photos, documents and conversations, including their history for the other participant. Reported content may remain in private moderation records for up to 90 days. This cannot be undone.')}</p>${field('currentPassword',t('Potwierdź obecnym hasłem','Confirm with your current password'),'password','','required autocomplete="current-password"')}<label class="check"><input type="checkbox" required>${t('Rozumiem i chcę trwale usunąć konto','I understand and want to permanently delete my account')}</label><button class="button secondary danger" type="submit">${t('Trwale usuń konto','Permanently delete account')}</button></form>`;
  }
  function blockButton(id,own){return `<button type="button" class="button secondary" ${own?'data-unblock-owner':'data-block-owner'}="${id}">${own?t('Odblokuj kontakt','Unblock contact'):t('Zablokuj kontakt','Block contact')}</button>`;}
  async function loadSafety(){safetyCapabilities=await api('/safety/capabilities');}
  async function loadBreeder(){try {breeder=await api('/owners/me');}catch(e){if(e.status!==404)throw e;breeder=null;}}
  document.addEventListener('click',async e=>{
    const el=e.target.closest('button');if(!el)return;
    if(el.dataset.language){ flash='';language=el.dataset.language;try{localStorage.setItem('matchcats-language',language);}catch{}render(); }
    if(el.id==='top-profile')location.hash='settings';
    if(el.hasAttribute('data-logout'))try {el.disabled=true;await api('/auth/logout',{method:'POST'});MatchCatsAPI.resetCSRF();user=null;breeder=null;unreadCount=0;location.hash='login';await render();}catch(error){showError(error);el.disabled=false;}
    if(el.hasAttribute('data-page')){if(el.dataset.pageType==='messages')chatPage=Number(el.dataset.page);else listPage=Number(el.dataset.page);render();}
    if(el.hasAttribute('data-refresh'))render();
    if(el.hasAttribute('data-notification-read'))try{el.disabled=true;await api('/notifications/'+el.dataset.notificationRead+'/read',{method:'POST'});await render();}catch(error){showError(error);el.disabled=false;}
    if(el.hasAttribute('data-start-checkout'))try{el.disabled=true;await api('/billing/checkout',{method:'POST'});}catch(error){showError(error);}finally{el.disabled=false;}
    if(el.hasAttribute('data-clear-search')){searchFilters={};listPage=0;render();}
    try {
      if(el.dataset.blockOwner && confirm(t('Zablokować nowe wiadomości i propozycje par z tą osobą?','Block new messages and pair proposals with this person?'))){el.disabled=true;await api('/safety/blocks/'+el.dataset.blockOwner,{method:'PUT'});await render();}
      if(el.dataset.unblockOwner && confirm(t('Wycofać Twoją blokadę kontaktu? Druga osoba może nadal mieć własną blokadę.','Remove your contact block? The other person may still have their own block.'))){el.disabled=true;await api('/safety/blocks/'+el.dataset.unblockOwner,{method:'DELETE'});await render();}
      if(el.dataset.contact){el.disabled=true;const chat=await api('/cats/'+el.dataset.contact+'/contact',{method:'POST'});chatPage=0;location.hash='messages/'+chat.id;}
      if(el.dataset.proposal && confirm(t('Zapisać tę odpowiedź na propozycję? Akceptacja dotyczy dalszego planowania, a nie potwierdzenia zdrowia kotów.','Save this response? Acceptance is for further planning, not confirmation of the cats’ health.'))){el.disabled=true;await api('/matches/'+el.dataset.proposal+'/decision',{method:'POST',body:{action:el.dataset.proposalAction}});await render(t('Odpowiedź zapisana.','Response saved.'));}
      if(el.dataset.pairSource){el.disabled=true;const pair=await api('/cats/'+el.dataset.pairSource+'/matches',{method:'POST',body:{candidateId:Number(el.dataset.pairCandidate)}});location.hash='messages/'+pair.conversationId;}
      if(el.dataset.deleteCat && confirm(t('Usunąć kota i jego dokumenty?','Delete this cat and its documents?'))){await api('/cats/'+el.dataset.deleteCat,{method:'DELETE'});location.hash='cats';}
      if(el.dataset.download||el.dataset.reviewDownload){const blob=await api((el.dataset.reviewDownload?'/moderation/documents/':'/documents/')+(el.dataset.reviewDownload||el.dataset.download)+'/download',{binary:true});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='document-'+(el.dataset.reviewDownload||el.dataset.download)+({ 'application/pdf':'.pdf','image/png':'.png','image/jpeg':'.jpg'}[blob.type]||'');a.click();setTimeout(()=>URL.revokeObjectURL(url),10000);}
      if(el.dataset.share){await api('/documents/'+el.dataset.share+'/visibility',{method:'PUT',body:{visibility:el.dataset.visibility}});await render();}
      if(el.dataset.deletePhoto && confirm(t('Usunąć zdjęcie kota?','Remove the cat photo?'))){await api('/cats/'+el.dataset.deletePhoto+'/photo',{method:'DELETE'});await render();}
      if(el.dataset.deleteDocument && confirm(t('Usunąć ten dokument?','Delete this document?'))){await api('/documents/'+el.dataset.deleteDocument,{method:'DELETE'});await render();}
      if(el.dataset.appealDocument){const note=prompt(t('Napisz odwołanie (co najmniej 10 znaków).','Write your appeal (at least 10 characters).'));if(note&&note.trim().length>=10){el.disabled=true;await api('/documents/'+el.dataset.appealDocument+'/verification-appeal',{method:'POST',body:{note:note.trim()}});await render(t('Odwołanie zostało zapisane.','Appeal saved.'));}}

      if(el.dataset.requestVerification && confirm(t('Prośba udostępni ten plik moderatorowi do sprawdzenia, także gdy jest prywatny. Nie przypisano jeszcze czasu odpowiedzi. Wysłać prośbę?','Requesting review gives the moderator access to this file, including private files. No response time has been assigned yet. Submit the request?'))){el.disabled=true;await api('/documents/'+el.dataset.requestVerification+'/verification-request',{method:'POST'});await render(t('Prośba o sprawdzenie dokumentu została zapisana.','The document review request was saved.'));}
    }catch(error){showError(error);el.disabled=false;}
  });
  document.addEventListener('change',e=>{if(e.target.hasAttribute('data-show-password')) main.querySelectorAll('input[name="password"],input[name="confirm"]').forEach(input=>input.type=e.target.checked?'text':'password');});
  document.addEventListener('input',e=>{e.target.removeAttribute('aria-invalid');const box=e.target.closest('form')?.querySelector('.live-error');if(box)box.hidden=true;});
  document.addEventListener('submit',async e=>{
    const form=e.target;if(!form.classList.contains('document-review-decision') && !form.classList.contains('moderation-decision') && !form.classList.contains('moderation-reinstate') && !['safety-report-form','login-form','register-form','forgot-password-form','reset-password-form','request-email-form','verify-email-form','breeder-form','account-form','password-form','delete-account-form','live-cat-form','live-search','live-message-form','document-upload','photo-upload'].includes(form.id))return;
    e.preventDefault();
    if(!form.checkValidity()){const input=form.querySelector(':invalid');showError(t('Sprawdź wymagane pola, ich długość i format.','Check required fields, their length and format.'),form);if(input){input.setAttribute('aria-invalid','true');input.focus();}return;}
    const data=Object.fromEntries(new FormData(form));const submit=form.querySelector('[type="submit"]');
    flash='';
    if(form.id==='reset-password-form'){
      if(data.password!==data.confirm){showError(t('Hasła nie są takie same.','Passwords do not match.'));return;}
      if(new TextEncoder().encode(data.password).length>72){showError(t('Hasło przekracza 72 bajty. Skróć je.','Password exceeds 72 bytes. Shorten it.'));return;}
    }
    if(form.id==='password-form'){
      if(data.newPassword!==data.confirmPassword){showError(t('Hasła nie są takie same.','Passwords do not match.'));return;}
      if(new TextEncoder().encode(data.newPassword).length>72){showError(t('Hasło przekracza 72 bajty. Skróć je.','Password exceeds 72 bytes. Shorten it.'));return;}
      delete data.confirmPassword;
    }
    if(form.id==='register-form') {
      if(!/^[a-zA-Z0-9._-]{3,40}$/.test(data.username)){showError(t('Login może zawierać 3-40 liter, cyfr i znaków . _ -','Username must contain 3-40 letters, digits or . _ -'));return;}
      if(new TextEncoder().encode(data.password).length>72){showError(t('Hasło przekracza 72 bajty. Skróć je.','Password exceeds 72 bytes. Shorten it.'));return;}
      if(data.password!==data.confirm){showError(t('Hasła nie są takie same.','Passwords do not match.'));return;}delete data.confirm;
    }
    submit.disabled=true;form.setAttribute('aria-busy','true');
    try {
      if(form.id==='safety-report-form'){
        if(data.details.trim().length<10){showError(t('Opisz problem w co najmniej 10 znakach.','Describe the concern in at least 10 characters.'),form);return;}
        await api('/safety/reports',{method:'POST',body:{targetType:form.dataset.targetType,targetId:Number(form.dataset.targetId),reason:data.reason,details:data.details.trim()}});location.hash='safety';await render(t('Zgłoszenie zapisane. Jego stan znajdziesz poniżej.','Report saved. Its status is shown below.'));
      }else if(form.classList.contains('document-review-decision')){
        await api('/moderation/documents/'+form.dataset.documentId+'/decision',{method:'POST',body:{status:data.status,requestedAt:form.dataset.requestedAt,note:data.note}});await render(t('Decyzja dotycząca dokumentu zapisana.','Document decision saved.'));
      }else if(form.classList.contains('moderation-decision')){
        const [status,action]=data.decision.split(':');
        if(action==='SUSPEND_ACCOUNT' && !confirm(t('Zawiesić zgłoszone konto? Zostanie wylogowane, a jego koty ukryte.','Suspend the reported account? It will be signed out and its cats hidden.')))return;
        await api('/moderation/reports/'+form.dataset.reportId+'/decision',{method:'POST',body:{status,action,note:data.note}});await render(t('Decyzja zapisana.','Decision saved.'));
      }else if(form.classList.contains('moderation-reinstate')){await api('/moderation/accounts/'+form.dataset.accountId+'/reinstate',{method:'POST',body:{note:data.note}});await render(t('Dostęp do konta przywrócony.','Account access reinstated.'));}
      else if(form.id==='register-form'){await api('/users',{method:'POST',body:data});location.hash='login';await render(t('Konto zostało utworzone. Możesz się zalogować.','Your account has been created. You can sign in.'));}
      else if(form.id==='request-email-form'){await api('/auth/email/request',{method:'POST',body:{language}});await render(t('Link wysłany. Sprawdź skrzynkę i folder spam.','Link sent. Check your inbox and spam folder.'));}
      else if(form.id==='verify-email-form'){await api('/auth/email/confirm',{method:'POST',body:{token:form.dataset.token}});if(user)user=await api('/users/me');await render(t('Adres konta, dla którego wysłano link, został potwierdzony.','The account email this link was sent for has been verified.'));}
      else if(form.id==='forgot-password-form'){await api('/auth/password/request',{method:'POST',body:{email:data.email,language}});await render(t('Jeśli konto istnieje, wysłaliśmy link. Sprawdź również folder spam.','If an account exists, a link has been sent. Check your spam folder too.'));}
      else if(form.id==='reset-password-form'){await api('/auth/password/reset',{method:'POST',body:{token:form.dataset.token,password:data.password}});MatchCatsAPI.resetCSRF();user=null;breeder=null;unreadCount=0;location.hash='login';await render(t('Hasło zmienione. Zaloguj się ponownie.','Password changed. Please sign in again.'));}
      else if(form.id==='login-form'){user=await api('/auth/login',{method:'POST',body:data});MatchCatsAPI.resetCSRF();await loadBreeder();await loadSafety();location.hash=breeder?'dashboard':'settings';await render();}
      else if(form.id==='breeder-form'){breeder=await api('/owners/me',{method:'PUT',body:data});await render();}
      else if(form.id==='account-form'){user=await api('/users/me',{method:'PUT',body:data});await render();}
      else if(form.id==='password-form' || form.id==='delete-account-form') {
        await api(form.id==='password-form'?'/users/me/password':'/users/me',{method:form.id==='password-form'?'POST':'DELETE',body:data});
        MatchCatsAPI.resetCSRF();user=null;breeder=null;unreadCount=0;location.hash='login';await render(form.id==='password-form'?t('Hasło zmienione. Zaloguj się ponownie.','Password changed. Please sign in again.'):t('Konto zostało usunięte.','Your account has been deleted.'));
      }
      else if(form.id==='live-search'){searchFilters={breed:data.breed.trim(),sex:data.sex,city:data.city.trim(),available:data.available?'true':''};listPage=0;await render();}
      else if(form.id==='live-cat-form') {
        data.available=!!data.available;
        if(data.available && data.health!=='HEALTHY'){showError(t('Dostępność wymaga zadeklarowania stanu zdrowia jako zdrowy.','Availability requires owner-declared healthy status.'));return;}
        const cat=form.dataset.catId?await api('/cats/'+form.dataset.catId,{method:'PUT',body:{version:Number(form.dataset.version),profile:data}}):await api('/cats',{method:'POST',body:data});
        location.hash='cat/'+cat.id;
      }
      else if(form.id==='live-message-form'){if(!data.text.trim()){showError(t('Wpisz treść wiadomości.','Enter a message.'),form);return;}await api('/chats/'+form.dataset.chatId+'/messages',{method:'POST',body:{text:data.text.trim()}});const history=await api('/chats/'+form.dataset.chatId+'/messages?size=1');chatPage=Math.floor(Math.max(0,history.total-1)/50);await render();}
      else if(form.id==='document-upload'){if(data.file.size>5*1024*1024){showError(message({code:'FILE_TOO_LARGE'}));return;}await api('/cats/'+form.dataset.catId+'/documents',{method:'POST',body:new FormData(form)});await render();}
      else if(form.id==='photo-upload'){if(data.file.size>5*1024*1024){showError(message({code:'FILE_TOO_LARGE'}));return;}await api('/cats/'+form.dataset.catId+'/photo',{method:'POST',body:new FormData(form)});await render();}
    }catch(error){showError(error,form);}finally{submit.disabled=false;form.removeAttribute('aria-busy');}
  });
  window.addEventListener('hashchange',event=>{
    const oldRoute=new URL(event.oldURL).hash.slice(1).split('/')[0];
    const newRoute=new URL(event.newURL).hash.slice(1).split('/')[0];
    if(oldRoute!=='messages' || newRoute!=='messages')listPage=0;
    chatPage=0;render();window.scrollTo(0,0);
  });
  async function start() {
    shell('login');main.innerHTML=`<div class="live-loading" role="status">${t('Sprawdzanie sesji…','Checking your session…')}</div>`;
    try {user=await api('/users/me');await loadBreeder();await loadSafety();await render();}
    catch(error){user=null;await render();if(error.status!==401)showError(error);}
  }
  setInterval(refreshUnread,30000);
  document.addEventListener('visibilitychange',()=>{if(!document.hidden){acknowledgeMessages().then(refreshUnread);}});
  window.addEventListener('online',refreshUnread);
  start();
})();
