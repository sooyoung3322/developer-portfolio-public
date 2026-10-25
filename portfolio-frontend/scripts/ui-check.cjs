// Render real routes without a browser. This does not verify DOM interactions.
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const ts = require('typescript')
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
const { MemoryRouter } = require('react-router-dom')

require.extensions['.css'] = () => {}
for (const extension of ['.ts', '.tsx']) {
  require.extensions[extension] = (module, filename) => {
    const source = fs.readFileSync(filename, 'utf8').replaceAll('import.meta.env', '({ DEV: false })')
    module._compile(ts.transpileModule(source, { compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true,
    } }).outputText, filename)
  }
}
const root = path.resolve(__dirname, '..')
const App = require(path.join(root, 'src/App.tsx')).default
const { projects, projectRepositories } = require(path.join(root, 'src/data/projects.ts'))
const render = route => renderToStaticMarkup(React.createElement(MemoryRouter, { initialEntries: [route] }, React.createElement(App)))
const home = render('/')
assert.equal((home.match(/href="https:\/\/github.com\/sooyoung3322"/g) || []).length, 1)
assert.match(home, /target="_blank" rel="noopener noreferrer"/)
assert.match(home, /class="chat-introduction"/)
assert.equal((home.match(/class="chat-question"/g) || []).length, 0)
assert.equal((home.match(/id="portfolio-chat"/g) || []).length, 1)
assert.doesNotMatch(home, /class="portfolio-chat-trigger"/)
assert.doesNotMatch(home, /<dialog[^>]*\sopen(?:\s|=|>)/)
assert.doesNotMatch(home, /type="checkbox"[^>]*checked/)
assert.equal((home.match(/class="project-card"/g) || []).length, projects.length)
assert.match(home, /id="contact"/)
assert.doesNotMatch(home, /(?:href|src)="[^"]*\.docx(?:[?#"][^"]*)?/i)
assert.match(home, /class="header-download"/)
assert.equal((home.match(/class="header-download"/g) || []).length, 1)
const contact = home.match(/<section id="contact"[\s\S]*?<\/section>/)[0]
const { profile } = require('../src/data/profile.ts')
assert.ok(contact.includes(`href="mailto:${profile.email}"`))
assert.match(contact, /aria-label="이메일 주소 복사"/)
assert.match(contact, /class="contact-download"/)
for (const project of projects) {
  const html = render(`/projects/${project.id}`)
  assert.doesNotMatch(html, /(?:href|src)="[^"]*\.docx(?:[?#"][^"]*)?/i)
  assert.ok(home.includes(project.cardRole), `${project.id}: card role`)
  assert.ok(home.includes(project.period), `${project.id}: card period`)
  assert.ok(html.includes(project.period), `${project.id}: detail period`)
  if (project.highlights) {
    assert.ok(project.highlights.length >= 3 && project.highlights.length <= 5)
    for (const item of project.highlights) assert.ok(html.includes(item.replaceAll('&', '&amp;')), `${project.id}: implementation summary`)
    assert.ok(html.indexOf('핵심 구현') < html.indexOf('PROJECT STORY'))
  }
  assert.equal(html.includes('DATA FLOW / OVERVIEW'), !!project.flow?.length)
  assert.equal(html.includes('IMPLEMENTATION'), !!project.highlights?.length)
  for (const step of project.flow ?? []) assert.ok(html.includes(step))
  for (const image of project.images) assert.ok(html.includes(image.src))
  const savedHighlights = project.highlights, savedFlow = project.flow
  try {
    project.highlights = []; project.flow = []
    const empty = render('/projects/' + project.id)
    assert.ok(!empty.includes('IMPLEMENTATION') && !empty.includes('DATA FLOW / OVERVIEW'))
    project.highlights = savedHighlights
    assert.equal(render('/projects/' + project.id).includes('IMPLEMENTATION'), !!savedHighlights?.length)
  } finally { project.highlights = savedHighlights; project.flow = savedFlow }
  assert.match(html, new RegExp(`<h1>${project.title}</h1>`))
  assert.match(html, /aria-label="프로젝트 이동"/)
  if (project.repository) {
    assert.equal(project.repository.public, false)
    assert.ok(!html.includes(`href="${project.repository.url}"`))
    assert.ok(!html.includes('EXTERNAL LINKS'))
    project.repository.public = true
    try { assert.ok(render(`/projects/${project.id}`).includes(`href="${project.repository.url}"`)) }
    finally { project.repository.public = false }
  }
}
assert.equal(Object.values(projectRepositories).every(repository => !repository.public), true)
assert.match(render('/projects/unknown'), /페이지를 찾을 수 없습니다/)
console.log(`UI render checks passed: home, ${projects.length} project routes, 404, private/public links, chatbot entry and default consent`)
