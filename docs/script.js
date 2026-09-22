const languageButton = document.querySelector("[data-language-switch]");
const translatableNodes = document.querySelectorAll("[data-en][data-ru]");
const transcript = document.querySelector("[data-transcript]");
const replayButton = document.querySelector("[data-replay]");
const modeButtons = document.querySelectorAll("[data-mode]");
const modeName = document.querySelector("[data-mode-name]");
const modeDetail = document.querySelector("[data-mode-detail]");
const liveField = document.querySelector(".live-field");
const mobileDownload = document.querySelector(".mobile-download");
const fieldStatus = document.querySelector(".field-status span");
const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

const modeCopy = {
  en: {
    live: [
      "Text arrives while you speak.",
      "24 kHz audio streams in small chunks for the shortest path to first text.",
    ],
    accurate: [
      "Finish the thought, then transcribe.",
      "A temporary WAV is sent after you stop, then deleted from the device cache.",
    ],
  },
  ru: {
    live: [
      "Текст появляется, пока вы говорите.",
      "Аудио 24 кГц отправляется короткими фрагментами, чтобы первый текст пришёл быстрее.",
    ],
    accurate: [
      "Закончите мысль, затем распознайте.",
      "Временный WAV отправляется после остановки, а затем удаляется из кэша устройства.",
    ],
  },
};

let language = localStorage.getItem("opendictate-language")
  || (navigator.language.toLowerCase().startsWith("ru") ? "ru" : "en");
let activeMode = "live";
let replayTimer = null;

function setReplayStatus(listening) {
  fieldStatus.textContent = listening
    ? (language === "en" ? "Listening" : "Слушает")
    : fieldStatus.dataset[language];
}

function setMode(mode) {
  activeMode = mode;
  modeButtons.forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.mode === mode));
  });
  [modeName.textContent, modeDetail.textContent] = modeCopy[language][mode];
}

function setLanguage(nextLanguage) {
  language = nextLanguage;
  document.documentElement.lang = language;
  translatableNodes.forEach((node) => {
    const copy = node.dataset[language];
    if (copy.includes("<br>")) {
      node.innerHTML = copy;
    } else {
      node.textContent = copy;
    }
  });
  languageButton.querySelector("span").textContent = language === "en" ? "RU" : "EN";
  languageButton.setAttribute(
    "aria-label",
    language === "en" ? "Переключить на русский" : "Switch to English",
  );
  replayButton.setAttribute(
    "aria-label",
    language === "en" ? "Replay the dictation demonstration" : "Повторить демонстрацию диктовки",
  );
  mobileDownload.setAttribute(
    "aria-label",
    language === "en" ? "Download the latest OpenDictate APK" : "Скачать последний APK OpenDictate",
  );
  localStorage.setItem("opendictate-language", language);
  setReplayStatus(replayButton.classList.contains("is-replaying"));
  setMode(activeMode);
}

function replayTranscript() {
  window.clearTimeout(replayTimer);
  replayButton.classList.add("is-replaying");
  liveField.classList.add("is-replaying");
  setReplayStatus(true);
  const phrase = transcript.dataset[language];

  if (reducedMotion.matches) {
    transcript.textContent = phrase;
    replayTimer = window.setTimeout(() => {
      replayButton.classList.remove("is-replaying");
      liveField.classList.remove("is-replaying");
      setReplayStatus(false);
    }, 240);
    return;
  }

  const words = phrase.split(" ");
  let index = 0;
  transcript.textContent = "";

  const appendWord = () => {
    transcript.textContent = words.slice(0, index + 1).join(" ");
    index += 1;
    if (index < words.length) {
      replayTimer = window.setTimeout(appendWord, 145 + (index % 3) * 45);
    } else {
      replayTimer = window.setTimeout(() => {
        replayButton.classList.remove("is-replaying");
        liveField.classList.remove("is-replaying");
        setReplayStatus(false);
      }, 420);
    }
  };

  replayTimer = window.setTimeout(appendWord, 120);
}

languageButton.addEventListener("click", () => setLanguage(language === "en" ? "ru" : "en"));
replayButton.addEventListener("click", replayTranscript);

modeButtons.forEach((button) => {
  button.addEventListener("click", () => setMode(button.dataset.mode));
  button.addEventListener("keydown", (event) => {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
    event.preventDefault();
    const next = button.dataset.mode === "live" ? "accurate" : "live";
    const nextButton = document.querySelector(`[data-mode="${next}"]`);
    setMode(next);
    nextButton.focus();
  });
});

setLanguage(language);

window.setTimeout(() => {
  if (!reducedMotion.matches) replayTranscript();
}, 650);
