const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const ts = require('typescript')
require.extensions['.ts'] = (module, filename) => module._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, filename)
const { generatePortfolioMarkdown, downloadPortfolioMarkdown, portfolioFilename } = require('../src/portfolioMarkdown.ts')
const { projects, projectRepositories } = require('../src/data/projects.ts')
const { profile, experiences, skills } = require('../src/data/profile.ts')
const markdown = generatePortfolioMarkdown()
const withPrivateFields = structuredClone(projects)
withPrivateFields[0].privateResume = 'PRIVATE_RESUME_CANARY'
withPrivateFields[0].email = 'private-canary@example.test'
withPrivateFields[0].homeAddress = 'PRIVATE_ADDRESS_CANARY'
assert.equal(generatePortfolioMarkdown(withPrivateFields), markdown)
assert.doesNotMatch(markdown, /resume\.docx|PRIVATE_RESUME_CANARY|private-canary@example\.test|PRIVATE_ADDRESS_CANARY/i)
assert.equal(Buffer.from(markdown, 'utf8').toString('utf8'), markdown)
assert.ok(markdown.startsWith('# ' + profile.name + ' — ' + profile.title + '\n'))
for (const item of [...profile.introduction, ...experiences.flatMap(job => job.tasks), ...skills.flatMap(group => group.items)]) assert.ok(markdown.includes(item))
for (const project of projects) {
  assert.ok(markdown.includes(`### ${project.title}\n`))
  for (const item of project.sections.flatMap(section => section.paragraphs)) assert.ok(markdown.includes(item))
  for (const item of project.flow ?? []) assert.ok(markdown.includes(item))
  if (project.repository && !project.repository.public) assert.ok(!markdown.includes(project.repository.url))
}
assert.doesNotMatch(markdown, /<\/?[a-z][^>]*>|className=|(?:AKIA|ASIA)[A-Z0-9]{16}|-----BEGIN .*PRIVATE KEY-----|(?:mysql|jdbc):\/\//i)
const changed = structuredClone(projects[0])
changed.title = '수정된 프로젝트'
changed.sections = [{ title: '수정 내용', paragraphs: ['화면 원본 수정 반영'] }]
changed.flow = []; changed.highlights = []; changed.achievements = undefined
changed.repository = { url: 'https://github.com/example/public', public: true, kind: 'source' }
const updated = generatePortfolioMarkdown([changed])
assert.ok(updated.includes('### 수정된 프로젝트\n') && updated.includes('화면 원본 수정 반영'))
assert.ok(updated.includes('[GitHub 저장소](https://github.com/example/public)'))
assert.doesNotMatch(updated, /#### (주요 처리 흐름|핵심 구현|성과)\n/)
assert.ok(Object.values(projectRepositories).every(repository => !repository.public))
let blob, clicked = false, removed = false, revoked = false
const anchor = { click() { clicked = true; assert.equal(this.download, portfolioFilename); assert.equal(this.href, 'blob:test') }, remove() { removed = true } }
global.document = { createElement: tag => { assert.equal(tag, 'a'); return anchor }, body: { appendChild: element => assert.equal(element, anchor) } }
const originalCreate = URL.createObjectURL, originalRevoke = URL.revokeObjectURL
URL.createObjectURL = value => { blob = value; return 'blob:test' }
URL.revokeObjectURL = url => { assert.equal(url, 'blob:test'); revoked = true }
global.window = { setTimeout: (callback, delay) => { assert.ok(delay >= 0); callback() } }
;(async () => {
  try {
    let counted = 0
    downloadPortfolioMarkdown(() => { counted++ })
    assert.equal(counted, 1)
    downloadPortfolioMarkdown(() => { counted++ })
    assert.equal(counted, 2)
    downloadPortfolioMarkdown(() => { throw new Error('analytics unavailable') })
    const click = anchor.click
    anchor.click = () => { throw new Error('download initiation failed') }
    assert.throws(() => downloadPortfolioMarkdown(() => { counted++ }), /initiation failed/)
    assert.equal(counted, 2)
    anchor.click = click
    const create = URL.createObjectURL
    URL.createObjectURL = () => { throw new Error('Blob failed') }
    assert.throws(() => downloadPortfolioMarkdown(() => { counted++ }), /Blob failed/)
    assert.equal(counted, 2)
    URL.createObjectURL = create
    assert.ok(clicked && removed && revoked)
    assert.equal(blob.type, 'text/markdown;charset=utf-8')
    assert.equal(await blob.text(), markdown)
    if (process.argv.includes('--example')) {
      const target = path.resolve(__dirname, '../../docs/Portfolio.example.md')
      fs.writeFileSync(target, markdown, 'utf8')
    }
    console.log(`Markdown checks passed: ${projects.length} projects, shared data, UTF-8, public links, optional headings, Blob download and cleanup`)
  } finally { URL.createObjectURL = originalCreate; URL.revokeObjectURL = originalRevoke }
})().catch(error => { console.error(error); process.exitCode = 1 })
