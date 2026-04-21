/**
 * TRICARE Manuals — Web App
 *
 * Architecture:
 *   - Reads docs/data/manuals.json  (manual list + latestChange)
 *   - Reads docs/data/{CODE}/toc.json  (section list for a manual)
 *   - Reads docs/data/{CODE}/s/{id}.html  (individual section content)
 *
 * All files are served from the same origin (GitHub Pages), so no CORS
 * restrictions apply. Content is pre-fetched weekly by GitHub Actions.
 */

'use strict';

// ── Constants ──────────────────────────────────────────────────────────────
const DATA_ROOT = 'data';

const MANUAL_ICONS = {
  TOT5: '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z"/></svg>',
  TPT5: '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/></svg>',
  TRT5: '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z"/></svg>',
  TST5: '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>',
};

// ── State ──────────────────────────────────────────────────────────────────
const state = {
  manuals:       [],
  currentCode:   null,
  currentToc:    null,   // { change, sections: [{ id, title, chapter, isChapterToc }] }
  currentIdx:    -1,
  tocOpen:       false,
};

// ── Element references ──────────────────────────────────────────────────────
const $ = id => document.getElementById(id);
const els = {
  viewHome:       $('view-home'),
  viewReader:     $('view-reader'),
  manualGrid:     $('manual-grid'),
  tocPanel:       $('toc-panel'),
  tocOverlay:     $('toc-overlay'),
  tocList:        $('toc-list'),
  tocSearch:      $('toc-search'),
  readerContent:  $('reader-content'),
  readerManual:   $('reader-manual-name'),
  readerChangeBadge: $('reader-change-badge'),
  btnBack:        $('btn-back'),
  btnOpenToc:     $('btn-open-toc'),
  btnCloseToc:    $('btn-close-toc'),
  btnPrev:        $('btn-prev-section'),
  btnNext:        $('btn-next-section'),
  sectionCounter: $('section-counter'),
  toast:          $('toast'),
  btnDark:        $('btn-dark-toggle'),
  printContainer: $('print-container'),
};

// ── Initialise ──────────────────────────────────────────────────────────────
async function init() {
  // Restore dark mode preference
  if (localStorage.getItem('darkMode') === '1') {
    document.documentElement.setAttribute('data-dark', '');
  }

  els.btnDark.addEventListener('click', toggleDark);
  els.btnBack.addEventListener('click', showHome);
  els.btnOpenToc.addEventListener('click', () => openToc(true));
  els.btnCloseToc.addEventListener('click', () => openToc(false));
  els.tocOverlay.addEventListener('click', () => openToc(false));
  els.btnPrev.addEventListener('click', () => navigateSection(-1));
  els.btnNext.addEventListener('click', () => navigateSection(+1));
  els.tocSearch.addEventListener('input', filterToc);

  // Handle browser back/forward
  window.addEventListener('popstate', handlePopState);

  await loadManualList();
}

// ── Dark mode ───────────────────────────────────────────────────────────────
function toggleDark() {
  const on = document.documentElement.hasAttribute('data-dark');
  document.documentElement.toggleAttribute('data-dark', !on);
  localStorage.setItem('darkMode', on ? '0' : '1');
}

// ── Load manual list ────────────────────────────────────────────────────────
async function loadManualList() {
  try {
    const res = await fetch(`${DATA_ROOT}/manuals.json`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    state.manuals = data.manuals;
    renderManualGrid();
  } catch (e) {
    els.manualGrid.innerHTML = `
      <div class="no-content-msg" style="grid-column:1/-1">
        <strong>Could not load manual list</strong>
        <p>Please try refreshing, or visit
          <a href="https://manuals.health.mil" target="_blank" rel="noopener">manuals.health.mil</a>
          directly.</p>
      </div>`;
  }
}

// ── Render home grid ────────────────────────────────────────────────────────
function renderManualGrid() {
  const html = state.manuals.map(m => {
    const icon        = MANUAL_ICONS[m.code] || MANUAL_ICONS.TPT5;
    const changeLabel = m.latestChange ? `Change ${m.latestChange}` : 'Current';
    const chip        = m.hasContent
      ? `<span class="chip">${changeLabel}</span>`
      : `<span class="chip no-content-chip">Content coming soon</span>`;
    const btnDisabled = m.hasContent ? '' : 'disabled';

    const exportRow = m.hasContent ? `
      <div class="manual-card-exports">
        <button class="btn-export-card" onclick="exportManual('${m.code}','md')"
          aria-label="Download ${escHtml(m.name)} as Markdown">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
          Download .md
        </button>
        <button class="btn-export-card" onclick="exportManual('${m.code}','print')"
          aria-label="Print or save ${escHtml(m.name)} as PDF">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z"/></svg>
          Print / PDF
        </button>
      </div>` : '';

    return `
      <article class="manual-card" role="listitem">
        <div style="display:flex;align-items:center;gap:12px">
          <div class="manual-card-icon">${icon}</div>
          <div>
            <div class="manual-card-name">${escHtml(m.name)}</div>
            <div class="manual-card-meta">${escHtml(m.code)}</div>
          </div>
        </div>
        <div class="manual-card-footer">
          ${chip}
          <button class="btn-primary" ${btnDisabled}
            onclick="openManual('${m.code}')"
            aria-label="Open ${escHtml(m.name)}">Open</button>
        </div>
        ${exportRow}
      </article>`;
  }).join('');
  els.manualGrid.innerHTML = html;
}

// ── Open a manual ───────────────────────────────────────────────────────────
async function openManual(code) {
  const manual = state.manuals.find(m => m.code === code);
  if (!manual) return;

  state.currentCode = code;
  state.currentToc  = null;
  state.currentIdx  = -1;

  // Switch to reader view
  els.viewHome.classList.remove('active');
  els.viewReader.classList.add('active');

  els.readerManual.textContent = manual.name;
  els.readerChangeBadge.textContent = manual.latestChange
    ? `Change ${manual.latestChange}` : 'Current Edition';

  // Clear content + TOC
  setReaderContent(spinnerHtml());
  els.tocList.innerHTML = '';
  updateSectionNav();

  history.pushState({ code }, '', `#${code}`);

  // Load TOC
  try {
    const res = await fetch(`${DATA_ROOT}/${code}/toc.json`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.currentToc = await res.json();
    renderToc(state.currentToc.sections);

    // Auto-load first content section
    const firstContent = state.currentToc.sections.findIndex(s => !s.isChapterToc);
    if (firstContent >= 0) {
      loadSection(firstContent);
    }
  } catch (e) {
    setReaderContent(`
      <div class="no-content-msg">
        <strong>Table of contents unavailable</strong>
        <p>Content for this manual has not been pre-fetched yet.<br>
          Visit
          <a href="https://manuals.health.mil/pages/ManualToc.aspx?Manual=${code}"
             target="_blank" rel="noopener">manuals.health.mil</a>
          to read it online.</p>
      </div>`);
  }
}

// ── Render TOC ──────────────────────────────────────────────────────────────
function renderToc(sections) {
  let currentChapter = null;
  const items = sections.map((s, idx) => {
    if (s.isChapterToc || (s.chapter !== currentChapter)) {
      currentChapter = s.chapter;
    }
    const cls = s.isChapterToc
      ? 'toc-item chapter-heading'
      : 'toc-item section-item';
    const label = escHtml(s.title);
    if (s.isChapterToc) {
      return `<div class="${cls}" aria-hidden="true">${label}</div>`;
    }
    return `<button class="${cls}" data-idx="${idx}"
              onclick="loadSection(${idx})">${label}</button>`;
  }).join('');
  els.tocList.innerHTML = items;
}

// ── Filter TOC ──────────────────────────────────────────────────────────────
function filterToc() {
  const q = els.tocSearch.value.trim().toLowerCase();
  if (!state.currentToc) return;
  const items = els.tocList.querySelectorAll('[data-idx]');
  items.forEach(el => {
    const match = el.textContent.toLowerCase().includes(q);
    el.style.display = match ? '' : 'none';
  });
  // Show/hide chapter headings only if they have visible children
  const headings = els.tocList.querySelectorAll('.chapter-heading');
  headings.forEach(h => {
    h.style.display = q ? 'none' : '';
  });
}

// ── Load a section ──────────────────────────────────────────────────────────
async function loadSection(idx) {
  if (!state.currentToc) return;
  const section = state.currentToc.sections[idx];
  if (!section) return;

  // If it's a chapter TOC page, skip to next real section
  if (section.isChapterToc) {
    const next = state.currentToc.sections.findIndex((s, i) => i > idx && !s.isChapterToc);
    if (next >= 0) { loadSection(next); return; }
  }

  state.currentIdx = idx;
  setReaderContent(spinnerHtml());
  highlightTocItem(idx);
  updateSectionNav();
  closeTocOnMobile();

  try {
    const res = await fetch(`${DATA_ROOT}/${state.currentCode}/s/${section.id}.html`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const html = await res.text();
    setReaderContent(`<h2 style="margin-bottom:1rem">${escHtml(section.title)}</h2>${html}`);
    scrollReaderToTop();
  } catch (e) {
    setReaderContent(`
      <div class="no-content-msg">
        <strong>Section unavailable</strong>
        <p>This section has not been cached yet. You can read it at<br>
          <a href="https://manuals.health.mil" target="_blank" rel="noopener">manuals.health.mil</a>.</p>
      </div>`);
  }
}

// ── Navigate prev/next section ──────────────────────────────────────────────
function navigateSection(delta) {
  if (!state.currentToc) return;
  const sections = state.currentToc.sections;
  let next = state.currentIdx + delta;
  // Skip chapter heading entries
  while (next >= 0 && next < sections.length && sections[next].isChapterToc) {
    next += delta;
  }
  if (next >= 0 && next < sections.length) loadSection(next);
}

// ── TOC open/close ──────────────────────────────────────────────────────────
function openToc(open) {
  state.tocOpen = open;
  els.tocPanel.classList.toggle('open', open);
  els.tocOverlay.classList.toggle('visible', open);
  els.btnOpenToc.setAttribute('aria-expanded', String(open));
}

function closeTocOnMobile() {
  if (window.innerWidth <= 768) openToc(false);
}

// ── Update section prev/next buttons + counter ──────────────────────────────
function updateSectionNav() {
  const sections = state.currentToc ? state.currentToc.sections : [];
  const idx = state.currentIdx;

  // Count non-heading sections only
  const contentSections = sections.filter(s => !s.isChapterToc);
  const contentIdx = idx >= 0
    ? contentSections.indexOf(sections[idx])
    : -1;

  els.btnPrev.disabled = idx <= 0;
  els.btnNext.disabled = idx < 0 || idx >= sections.length - 1;

  if (contentIdx >= 0) {
    els.sectionCounter.textContent =
      `${contentIdx + 1} / ${contentSections.length}`;
  } else {
    els.sectionCounter.textContent = '';
  }
}

// ── Highlight active TOC item ───────────────────────────────────────────────
function highlightTocItem(idx) {
  els.tocList.querySelectorAll('.toc-item').forEach(el => {
    el.classList.toggle('active', Number(el.dataset.idx) === idx);
  });
  // Scroll active item into view in TOC
  const active = els.tocList.querySelector('.toc-item.active');
  if (active) active.scrollIntoView({ block: 'nearest' });
}

// ── Show home ───────────────────────────────────────────────────────────────
function showHome() {
  state.currentCode  = null;
  state.currentToc   = null;
  state.currentIdx   = -1;
  openToc(false);
  els.viewReader.classList.remove('active');
  els.viewHome.classList.add('active');
  history.pushState({}, '', location.pathname);
}

// ── Handle browser back/forward ─────────────────────────────────────────────
function handlePopState(e) {
  const hash = location.hash.replace('#', '');
  if (hash && state.manuals.find(m => m.code === hash)) {
    openManual(hash);
  } else {
    showHome();
  }
}

// ── Helpers ─────────────────────────────────────────────────────────────────
function setReaderContent(html) {
  els.readerContent.innerHTML = html;
}

function scrollReaderToTop() {
  const body = document.getElementById('reader-body');
  if (body) body.scrollTop = 0;
}

function spinnerHtml() {
  return `<div class="loading-spinner">
    <div class="spinner"></div>
    <span>Loading…</span>
  </div>`;
}

function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

let toastTimer;
function showToast(msg) {
  clearTimeout(toastTimer);
  els.toast.textContent = msg;
  els.toast.classList.add('show');
  toastTimer = setTimeout(() => els.toast.classList.remove('show'), 2800);
}

// ── Export: Download Markdown / Print PDF ───────────────────────────────────

/**
 * Fetch every content section for a manual, then either:
 *   format='md'    → convert to Markdown and trigger a file download
 *   format='print' → inject into a print container and call window.print()
 *
 * Works from both the home screen and the reader (code is always explicit).
 */
async function exportManual(code, format) {
  const manual = state.manuals.find(m => m.code === code);
  if (!manual || !manual.hasContent) return;

  const name = manual.name;

  // Load TOC — reuse the already-loaded one if the reader has it open,
  // otherwise fetch it fresh (home screen path).
  let toc;
  if (state.currentCode === code && state.currentToc) {
    toc = state.currentToc;
  } else {
    showToast('Loading table of contents…');
    try {
      const r = await fetch(`${DATA_ROOT}/${code}/toc.json`);
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      toc = await r.json();
    } catch (e) {
      showToast('Could not load manual — try again');
      return;
    }
  }

  const sections = toc.sections.filter(s => !s.isChapterToc);

  showToast(`Preparing ${sections.length} sections…`);

  // Fetch all section HTML in parallel
  const fetched = await Promise.all(
    sections.map(async s => {
      try {
        const res = await fetch(`${DATA_ROOT}/${code}/s/${s.id}.html`);
        return res.ok ? { title: s.title, html: await res.text() } : null;
      } catch { return null; }
    })
  );
  const valid = fetched.filter(Boolean);

  if (format === 'md') {
    const lines = [`# ${name}\n`];
    if (manual && manual.latestChange) lines.push(`_Change ${manual.latestChange}_\n`);
    for (const { title, html } of valid) {
      lines.push(`\n## ${title}\n`);
      lines.push(htmlToMarkdown(html));
      lines.push('\n---\n');
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/markdown; charset=utf-8' });
    const url  = URL.createObjectURL(blob);
    const a    = Object.assign(document.createElement('a'), { href: url, download: `${code}.md` });
    a.click();
    URL.revokeObjectURL(url);
    showToast('Download started');

  } else {
    // Build print document
    const changeStr = (manual && manual.latestChange) ? `, Change ${manual.latestChange}` : '';
    const body = valid.map(({ title, html }) =>
      `<section class="ps"><h2>${escHtml(title)}</h2>${html}</section>`
    ).join('\n');

    els.printContainer.innerHTML =
      `<h1>${escHtml(name)}${escHtml(changeStr)}</h1>` +
      `<p class="pm">Defense Health Agency &middot; manuals.health.mil</p>` +
      body;

    window.print();
    els.printContainer.innerHTML = '';
    showToast('Print dialog opened');
  }
}

// ── HTML → Markdown converter ────────────────────────────────────────────────

function htmlToMarkdown(htmlStr) {
  const doc = new DOMParser().parseFromString(htmlStr, 'text/html');
  return domToMd(doc.body).replace(/\n{3,}/g, '\n\n').trim();
}

function domToMd(node) {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.textContent.replace(/[ \t]+/g, ' ');
  }
  if (node.nodeType !== Node.ELEMENT_NODE) return '';

  const tag   = node.tagName.toLowerCase();
  const inner = () => Array.from(node.childNodes).map(domToMd).join('');

  if (tag === 'table')  return mdTable(node);
  if (tag === 'script' || tag === 'style' || tag === 'noscript') return '';

  switch (tag) {
    case 'h1': return `\n\n# ${inner().trim()}\n\n`;
    case 'h2': return `\n\n## ${inner().trim()}\n\n`;
    case 'h3': return `\n\n### ${inner().trim()}\n\n`;
    case 'h4': return `\n\n#### ${inner().trim()}\n\n`;
    case 'h5': case 'h6': return `\n\n##### ${inner().trim()}\n\n`;
    case 'p':  return `\n\n${inner().trim()}\n\n`;
    case 'br': return '\n';
    case 'hr': return '\n\n---\n\n';
    case 'strong': case 'b':  return `**${inner()}**`;
    case 'em':     case 'i':  return `*${inner()}*`;
    case 'code': return `\`${node.textContent}\``;
    case 'pre':  return `\n\n\`\`\`\n${node.textContent.trim()}\n\`\`\`\n\n`;
    case 'blockquote': return `\n\n> ${inner().trim().replace(/\n/g, '\n> ')}\n\n`;
    case 'a': {
      const href = node.getAttribute('href') || '';
      const text = inner().trim();
      return href ? `[${text}](${href})` : text;
    }
    case 'ul': {
      const items = Array.from(node.children)
        .filter(c => c.tagName === 'LI')
        .map(li => `- ${domToMd(li).trim()}`)
        .join('\n');
      return `\n\n${items}\n\n`;
    }
    case 'ol': {
      const items = Array.from(node.children)
        .filter(c => c.tagName === 'LI')
        .map((li, i) => `${i + 1}. ${domToMd(li).trim()}`)
        .join('\n');
      return `\n\n${items}\n\n`;
    }
    case 'li': return inner();
    default:   return inner();
  }
}

function mdTable(table) {
  const rows = Array.from(table.querySelectorAll('tr'));
  if (!rows.length) return '';
  const cells = rows.map(r =>
    Array.from(r.querySelectorAll('th, td'))
      .map(c => c.textContent.trim().replace(/\|/g, '\\|').replace(/\s+/g, ' '))
  );
  const cols  = Math.max(...cells.map(r => r.length));
  const pad   = row => { while (row.length < cols) row.push(''); return row; };
  const fmt   = row => `| ${pad(row).join(' | ')} |`;
  const [hdr, ...body] = cells;
  const sep   = Array(cols).fill('---');
  return `\n\n${fmt(hdr || sep)}\n${fmt(sep)}\n${body.map(fmt).join('\n')}\n\n`;
}

// ── Easter egg visitor counter ───────────────────────────────────────────────
// Uses CounterAPI (counterapi.dev) for persistent cross-device counts.
// localStorage tracks whether THIS browser has visited before so we only
// bump the "unique" counter once per browser.
(async function initCounter() {
  const NS   = 'appsbydan-manualbridge-v1';
  const isNew = !localStorage.getItem('mb_visited');
  if (isNew) localStorage.setItem('mb_visited', '1');

  const base = 'https://api.counterapi.dev/v1';

  try {
    // Always increment total hits; only increment unique on first visit.
    const [hitRes, uniqRes] = await Promise.all([
      fetch(`${base}/${NS}/hits/up`),
      isNew
        ? fetch(`${base}/${NS}/unique/up`)
        : fetch(`${base}/${NS}/unique`),
    ]);

    if (!hitRes.ok || !uniqRes.ok) return;

    const { count: hits   } = await hitRes.json();
    const { count: unique } = await uniqRes.json();

    const el = document.getElementById('visitor-counts');
    if (el) {
      el.textContent =
        `👁 ${unique.toLocaleString()} unique  ·  🔁 ${hits.toLocaleString()} total`;
    }
  } catch {
    // Counter service unavailable — fail silently, easter egg stays hidden
  }
})();

// ── Bootstrap ───────────────────────────────────────────────────────────────
init().catch(console.error);
