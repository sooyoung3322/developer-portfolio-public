const fs = require('node:fs')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')
const { randomUUID } = require('node:crypto')
const source = fs.readFileSync(require('node:path').join(__dirname, '../src/analytics.tsx'), 'utf8').replaceAll('import.meta.env', '({ PROD: true })')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const requests = []
const storage = new Map()
function load(fetchMode) {
  let route = { pathname: '/', key: 'default' }
  let effects = []
  let listener
  class Element { closest() { return { getAttribute: () => '/projects/project-one' } } }
  const exports = {}
  vm.runInNewContext(compiled, {
    exports, require: name => name === 'react' ? { useEffect: fn => effects.push(fn) } : name === 'react-router-dom' ? { useLocation: () => route } : { projects: [{ id: 'project-one' }, { id: 'project-six' }] },
    crypto: { randomUUID }, localStorage: { getItem: k => storage.get(k), setItem: (k,v) => storage.set(k,v) },
    fetch: async (url, options) => { requests.push({ url, ...JSON.parse(options.body) }); if (fetchMode === 'reject') throw new Error('offline'); if (fetchMode === 'pending') return new Promise(() => {}); return { ok: fetchMode !== '500', status: fetchMode === '500' ? 500 : 200 } },
    AbortSignal, URL, Element, location: { origin: 'https://portfolio.example' }, window: { location: { href: 'https://portfolio.example/' } },
    document: { addEventListener: (_, fn) => { listener = fn }, removeEventListener: () => { listener = undefined } },
  })
  function render(pathname, key) {
    route = { pathname, key }; effects = []; exports.AnalyticsTracking()
    // StrictMode: setup, cleanup, setup. A plain rerender is also simulated.
    for (const effect of effects) { const cleanup = effect(); if (cleanup) cleanup(); effect() }
  }
  return { trackMarkdownDownload: exports.trackMarkdownDownload, render, click: () => listener({ button: 0, target: new Element() }) }
}
for (let i=0; i<3; i++) { const app = load(); app.render('/', 'default'); app.render('/', 'default') }
assert.equal(requests.filter(r => r.url.endsWith('/visits')).length, 3)
assert.equal(new Set(requests.map(r => r.browserId)).size, 1)
requests.length = 0
const app = load(); app.render('/', 'default')
for (let i=0; i<5; i++) app.click()
assert.equal(requests.filter(r => r.url.endsWith('/project-one/clicks')).length, 5)
assert.equal(new Set(requests.filter(r => r.url.endsWith('/clicks')).map(r => r.eventId)).size, 5)
assert.equal(requests.filter(r => r.url.includes('project-six')).length, 0)
app.render('/projects/project-one', 'a'); app.render('/projects/project-one', 'a')
assert.equal(requests.filter(r => r.url.endsWith('/views')).length, 1)
app.render('/', 'b'); app.render('/projects/project-one', 'a')
assert.equal(requests.filter(r => r.url.endsWith('/views')).length, 2)
console.log('Analytics checks passed: 3 visits, 5 clicks, StrictMode/rerender deduplication, back navigation.')

assert.equal(requests.filter(r => r.url.endsWith('/portfolio-markdown-download')).length, 0)
app.trackMarkdownDownload(); app.trackMarkdownDownload()
const downloads = requests.filter(r => r.url.endsWith('/portfolio-markdown-download'))
assert.equal(downloads.length, 2)
assert.notEqual(downloads[0].eventId, downloads[1].eventId)
console.log('Download analytics: page visits do not count; two initiations send two distinct events.')

for (const mode of ['reject', '500', 'pending']) {
  const failing = load(mode)
  assert.doesNotThrow(() => failing.trackMarkdownDownload())
}
console.log('Download analytics tolerate rejected, HTTP 500 and pending fetch without awaiting.')
