'use strict';
window.MatchCatsPlans=(()=>{
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const t=(pl,en,language)=>language==='en'?en:pl;
  function page(language,current){
    const plans=[
      {name:t('Plan bezpłatny','Free plan',language),price:t('0 zł','Free',language),items:[t('Profil hodowcy i kotów','Breeder and cat profiles',language),t('Wyszukiwanie kandydatów','Candidate search',language),t('Rozmowy i propozycje par','Conversations and pair proposals',language)]},
      {name:t('Plan Premium','Premium plan',language),price:t('W przygotowaniu','Coming soon',language),items:[t('Więcej profili i dokumentów','More profiles and documents',language),t('Rozszerzone filtry wyszukiwania','Extended search filters',language),t('Wyróżnienie hodowli','Cattery highlighting',language)]}
    ];
    const currentText=current?t('Twój plan: '+current.planId,'Your plan: '+current.planId,language):t('Zaloguj się, aby zobaczyć plan konta.','Sign in to see your account plan.',language);
    return `<section class="panel live-page plans-page"><h1>${t('Plany MatchCats','MatchCats plans',language)}</h1><p class="muted">${t('Podstawowe funkcje pozostają bezpłatne. Płatności nie są jeszcze aktywne.','Core features remain free. Payments are not active yet.',language)}</p><p class="notice" data-current-plan>${esc(currentText)}</p><div class="cat-grid">${plans.map((plan,index)=>`<article class="panel"><h2>${esc(plan.name)}</h2><p class="price">${esc(plan.price)}</p><ul>${plan.items.map(item=>`<li>${esc(item)}</li>`).join('')}</ul>${index===1?`<button class="button secondary" type="button" data-start-checkout>${t('Sprawdź dostępność Premium','Check Premium availability',language)}</button>`:''}</article>`).join('')}</div><p class="notice">${t('Przed uruchomieniem płatności właściciel projektu musi skonfigurować sklep, dane podatkowe, regulamin i bezpiecznego operatora płatności.','Before payments go live, the project owner must configure the store, tax details, terms and a secure payment provider.',language)}</p><div class="live-tools"><a class="button secondary" href="#login">${t('Wróć do logowania','Back to sign in',language)}</a></div></section>`;
  }
  return {page};
})();

