'use strict';
window.MatchCatsLegal=(()=>{
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const t=(pl,en,language)=>language==='en'?en:pl;
  function page(kind,language){
    const privacy=kind==='privacy';
    const title=privacy?t('Polityka prywatności','Privacy policy',language):t('Regulamin MatchCats','MatchCats terms of use',language);
    const paragraphs=privacy?[
      t('MatchCats pomaga hodowcom kontaktować się w sprawie kotów. Konto przechowuje dane profilu, kotów, rozmów i dokumentów potrzebne do działania aplikacji.','MatchCats helps breeders communicate about cats. An account stores profile, cat, conversation and document data needed to operate the app.',language),
      t('Dokumenty są własnością użytkownika. Udostępnienie dokumentu hodowcom jest osobną decyzją. Sprawdzenie przez moderatora nie oznacza potwierdzenia zdrowia, pochodzenia ani przydatności kota do rozmnażania.','Documents belong to the user. Sharing a document with breeders is a separate choice. Moderator review does not confirm health, ancestry or breeding suitability.',language),
      t('Dane są używane do logowania, bezpieczeństwa, rozmów, propozycji par i obsługi zgłoszeń. Zgłoszone treści mogą pozostać w prywatnych danych moderacji przez okres opisany w procedurach bezpieczeństwa.','Data is used for sign-in, safety, conversations, pair proposals and reports. Reported content may remain in private moderation records for the period described in the safety procedures.',language),
      t('Przed uruchomieniem produkcyjnym właściciel projektu musi uzupełnić administratora danych, adres kontaktowy, podstawę prawną, okresy przechowywania i procedurę usuwania danych.','Before production launch, the project owner must add the data controller, contact address, legal basis, retention periods and deletion procedure.',language)
    ]:[
      t('Korzystaj z MatchCats zgodnie z prawem i dobrem zwierząt. Podawaj prawdziwe informacje, szanuj innych hodowców i nie przesyłaj danych, do których nie masz prawa.','Use MatchCats lawfully and with the welfare of animals in mind. Provide truthful information, respect other breeders and do not upload data you do not have the right to share.',language),
      t('Propozycja pary służy do rozmowy między hodowcami. Akceptacja nie jest gwarancją zdrowia, zgodności genetycznej ani bezpiecznego rozmnażania. Skonsultuj decyzje z weterynarzem.','A pair proposal starts a conversation between breeders. Acceptance is not a guarantee of health, genetic compatibility or safe breeding. Consult a veterinarian about decisions.',language),
      t('Moderator może rozpatrywać zgłoszenia i dokumenty zgodnie z ustaloną procedurą. Nie wolno obchodzić blokad, próbować uzyskać cudzych plików ani podszywać się pod moderatora.','Moderators may review reports and documents under the established procedure. Do not bypass blocks, seek other users’ files or impersonate a moderator.',language),
      t('Przed publikacją właściciel projektu musi dodać dane operatora, procedurę reklamacji i właściwe wymagania prawne dla kraju działania.','Before publication, the project owner must add operator details, an appeal procedure and the legal requirements for each country of operation.',language)
    ];
    return `<section class="panel live-page legal-page"><h1>${esc(title)}</h1><p class="muted">${esc(t('Wersja robocza do uzupełnienia przed publikacją.','Draft to complete before publication.',language))}</p>${paragraphs.map(p=>`<p>${esc(p)}</p>`).join('')}<p class="notice">${esc(t('Ta strona nie zastępuje porady prawnej.','This page is not legal advice.',language))}</p><div class="live-tools"><a class="button secondary" href="#login">${esc(t('Wróć do logowania','Back to sign in',language))}</a></div></section>`;
  }
  return {page};
})();

