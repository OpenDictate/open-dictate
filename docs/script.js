const languageButton = document.querySelector("[data-language-switch]");
const translatableNodes = document.querySelectorAll("[data-en][data-ru]");
const preview = document.querySelector(".interface-preview");

let language = localStorage.getItem("opendictate-language")
  || (navigator.language.toLowerCase().startsWith("ru") ? "ru" : "en");

function setLanguage(nextLanguage) {
  language = nextLanguage;
  document.documentElement.lang = language;
  translatableNodes.forEach((node) => {
    node.textContent = node.dataset[language];
  });
  preview.setAttribute("aria-label", preview.dataset[language + "Label"]);
  languageButton.textContent = language === "en" ? "RU" : "EN";
  languageButton.setAttribute(
    "aria-label",
    language === "en" ? "Переключить на русский" : "Switch to English",
  );
  localStorage.setItem("opendictate-language", language);
}

languageButton.addEventListener("click", () => setLanguage(language === "en" ? "ru" : "en"));
setLanguage(language);
