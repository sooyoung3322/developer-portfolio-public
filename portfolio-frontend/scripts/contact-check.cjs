const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const assert = require('node:assert/strict')
const contact = require('../src/data/public-contact.json')
const source = fs.readFileSync(require('node:path').join(__dirname, '../src/components/ContactEmail.tsx'), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX,
} }).outputText
async function check(clipboard, expected) {
  let message = ''
  const exports = {}
  vm.runInNewContext(compiled, { exports, navigator: { clipboard }, require: name =>
    name === 'react' ? { useState: () => ['', value => { message = value }] }
    : name === '../data/profile' ? { profile: contact } : require(name) })
  const tree = exports.ContactEmail()
  const [link, button] = tree.props.children
  assert.equal(link.props.href, `mailto:${contact.email}`)
  assert.equal(link.props.children, contact.email)
  button.props.onClick()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(message, expected)
}
;(async () => {
  await check({ writeText: async value => assert.equal(value, contact.email) }, '복사했습니다.')
  const failure = '복사하지 못했습니다. 이메일을 직접 선택해 주세요.'
  await check({ writeText: async () => { throw new Error('denied') } }, failure)
  await check(undefined, failure)
  console.log('Contact checks passed: mailto, copy success, denied permission and unavailable Clipboard API')
})().catch(error => { console.error(error); process.exitCode = 1 })
