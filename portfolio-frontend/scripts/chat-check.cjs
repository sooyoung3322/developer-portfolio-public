// Component logic checks using a deterministic hook host, without a browser DOM.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const React = require('react')
require.extensions['.css'] = () => {}
for (const extension of ['.ts', '.tsx']) require.extensions[extension] = (module, filename) => {
  const source = fs.readFileSync(filename, 'utf8').replaceAll('import.meta.env', '({ PROD: true })')
  module._compile(ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX,
  } }).outputText, filename)
}
const { PortfolioChat } = require('../src/components/PortfolioChat.tsx')
const { chatNotice } = require('../src/components/chat-notices.ts')
const original = { useRef: React.useRef, useState: React.useState, useEffect: React.useEffect }
global.HTMLElement = class {}
global.document = { activeElement: null }
let timers, nextTimer
global.window = { setTimeout(fn, ms) { const id = ++nextTimer; timers.set(id, { fn, ms }); return id } }
global.clearTimeout = id => timers.delete(id)
const response = (code, ok = false) => ({ ok, json: async () => ({ code, answer: 'private diagnostics', projects: [], limits: { retryAfterSeconds: 600 } }) })
const tick = () => new Promise(resolve => setImmediate(resolve))
function host() {
  timers = new Map(); nextTimer = 0
  const slots = []; let index, effects, dirty = true, tree
  React.useRef = value => { const i = index++; return slots[i] ??= { current: value } }
  React.useState = initial => {
    const i = index++; slots[i] ??= { value: initial }
    return [slots[i].value, value => { slots[i].value = typeof value === 'function' ? value(slots[i].value) : value; dirty = true }]
  }
  React.useEffect = (effect, deps) => {
    const i = index++; const previous = slots[i]
    if (!previous || deps.some((dep, n) => !Object.is(dep, previous.deps[n]))) {
      effects.push(() => { previous?.cleanup?.(); slots[i] = { deps, cleanup: effect() } })
    }
  }
  function render() {
    do { dirty = false; index = 0; effects = []; tree = PortfolioChat({ children: null }); effects.forEach(effect => effect()) } while (dirty)
    return tree
  }
  function nodes(node) {
    if (!node || typeof node !== 'object') return []
    if (Array.isArray(node)) return node.flatMap(nodes)
    return [node, ...nodes(node.props?.children)]
  }
  const find = predicate => nodes(tree).find(predicate)
  const textarea = () => find(node => node.type === 'textarea')
  const form = () => find(node => node.type === 'form')
  const alerts = () => nodes(tree).filter(node => node.props?.className?.includes('portfolio-chat-alert '))
  const messages = () => nodes(tree).filter(node => node.props?.className?.startsWith('portfolio-chat-message '))
  return {
    render, textarea, alerts, messages,
    warning: () => find(node => node.props?.className?.startsWith('portfolio-chat-availability')),
    suggestions: () => nodes(find(node => node.props?.className === 'portfolio-chat-suggestions')).filter(node => node.type === 'button'),
    retry() { find(node => node.props?.className?.startsWith('portfolio-chat-availability')).props.children[1].props.onClick(); render() },
    enter() { textarea().props.onKeyDown({ key: 'Enter', shiftKey: false, nativeEvent: { isComposing: false }, preventDefault() {} }); render() },
    write(text) { textarea().props.onChange({ target: { value: text } }); render() },
    async open() { render().props.value(); render(); await tick(); render() },
    async send(text = 'Question') {
      textarea().props.onChange({ target: { value: text } }); render()
      form().props.onSubmit({ preventDefault() {} }); render(); await tick(); render()
    },
    submit() { form().props.onSubmit({ preventDefault() {} }) },
    async reopen() {
      find(node => node.type === 'dialog').props.onClose(); render()
      await this.open()
    },
    loading: () => !!find(node => node.props?.className === 'portfolio-chat-loading'),
    dispose() { slots.forEach(slot => slot?.cleanup?.()); Object.assign(React, original) },
  }
}
async function scenario(run, status = { enabled: true }) {
  let calls = 0, reply = response('API_ERROR')
  global.fetch = async (url, options) => {
    if (url.endsWith('/status')) {
      if (typeof status === 'function') return status(options)
      if (status instanceof Error) throw status
      return { ok: true, json: async () => status }
    }
    calls++; return typeof reply === 'function' ? reply(options) : reply
  }
  const app = host()
  try { await app.open(); await run(app, value => { reply = value }, () => calls) }
  finally { app.dispose() }
}
async function main() {
  let resolveStatus
  await scenario(async (app, setReply, calls) => {
    assert.equal(app.textarea().props.disabled, true)
    app.write('Draft'); app.enter(); app.submit(); assert.equal(calls(), 0)
    resolveStatus({ ok: true, json: async () => ({ enabled: true }) })
    await tick(); app.render(); assert.equal(app.textarea().props.disabled, false)
  }, () => new Promise(resolve => { resolveStatus = resolve }))
  for (const first of ['disabled', 'offline']) {
    let checks = 0
    await scenario(async (app, setReply, calls) => {
      assert.ok(JSON.stringify(app.warning()).includes(first === 'disabled' ? '포트폴리오에서' : '서버에 연결'))
      assert.equal(app.textarea().props.disabled, true)
      app.write('Draft'); app.enter(); app.submit(); assert.equal(calls(), 0)
      app.retry(); await tick(); app.render()
      assert.equal(app.textarea().props.disabled, false)
      assert.equal(app.textarea().props.value, 'Draft')
      assert.equal(calls(), 0); assert.equal(checks, 2)
    }, () => {
      checks++
      if (checks === 1 && first === 'offline') throw new TypeError('offline')
      return { ok: true, json: async () => ({ enabled: checks > 1 }) }
    })
  }
  for (const code of ['APP_DAILY_LIMITED', 'GEMINI_RPD_LIMITED', 'SESSION_RATE_LIMITED', 'GEMINI_RATE_LIMITED', 'GEMINI_RPM_LIMITED', 'GEMINI_TPM_LIMITED', 'API_ERROR', 'SERVER_ERROR', 'DISABLED']) {
    await scenario(async (app, setReply, calls) => {
      setReply(response(code)); await app.send()
      assert.equal(calls(), 1); assert.equal(app.loading(), false)
      assert.equal(app.alerts().length, 1)
      assert.equal(app.textarea().props.disabled, chatNotice(code).daily || code === 'DISABLED')
      const text = JSON.stringify(app.alerts()[0].props.children)
      assert.ok(text.includes(chatNotice(code).text.replaceAll('\n', '\\n')))
      assert.ok(!text.includes('private diagnostics'))
      assert.equal(app.messages()[0].props.className, 'portfolio-chat-message is-user')
      app.render(); app.render(); assert.equal(calls(), 1)
    })
  }
  await scenario(async app => { assert.ok(app.warning()); assert.equal(app.suggestions().length, 0); assert.equal(app.textarea().props.disabled, true) }, { enabled: false })
  await scenario(async app => {
    assert.ok(app.warning()); assert.equal(app.textarea().props.disabled, true)
  }, new TypeError('status endpoint offline'))
  await scenario(async (app, setReply) => {
    setReply(response('APP_DAILY_LIMITED')); await app.send()
    await app.reopen(); assert.equal(app.textarea().props.disabled, true)
    const reset = [...timers.values()].find(timer => timer.ms > 590000)
    assert.ok(reset); reset.fn(); app.render(); assert.equal(app.textarea().props.disabled, false)
  })
  await scenario(async (app, setReply) => {
    setReply(response('SESSION_RATE_LIMITED')); await app.send()
    setReply(response('GEMINI_RPM_LIMITED')); await app.send()
    assert.equal(app.alerts().length, 1); assert.equal(app.messages().length, 3)
  })
  await scenario(async (app, setReply, calls) => {
    setReply(() => { throw new TypeError('private network detail') })
    await app.send(); assert.equal(app.loading(), false); assert.equal(app.textarea().props.disabled, true)
    assert.equal(app.textarea().props.value, 'Question')
    app.enter(); await app.send(); assert.equal(app.alerts().length, 1); assert.equal(calls(), 1)
    assert.equal(app.messages().length, 2) // Both questions retained, one error.
    assert.equal(app.messages().at(-1), app.alerts()[0])
  })
  await scenario(async (app, setReply, calls) => {
    let resolve
    setReply(() => new Promise(done => { resolve = done }))
    await app.send(); assert.equal(app.loading(), true)
    app.submit(); app.render(); app.submit(); assert.equal(calls(), 1)
    resolve({ ok: true, json: async () => ({ code: 'SUCCESS', answer: 'Public answer', projects: [], conversationStored: false }) })
    await tick(); app.render(); assert.equal(app.loading(), false)
    assert.equal(app.alerts().length, 0); assert.equal(app.messages().length, 2)
    setReply(response('GEMINI_RATE_LIMITED')); await app.send()
    assert.equal(app.messages().length, 4) // Successful history survives errors.
  })
  await scenario(async (app, setReply) => {
    setReply({ ok: false, json: async () => { throw new SyntaxError('HTML gateway error') } })
    await app.send(); assert.equal(app.alerts().length, 1); assert.equal(app.loading(), false)
    assert.equal(app.textarea().props.disabled, true)
  })
  await scenario(async (app, setReply) => {
    setReply(options => new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('aborted')))))
    await app.send(); [...timers.values()].find(timer => timer.ms === 30000).fn()
    await tick(); app.render(); assert.equal(app.loading(), false); assert.equal(app.alerts().length, 1)
  })
  await scenario(async (app, setReply, calls) => {
    assert.equal(app.textarea().props.disabled, false)
    const buttons = app.suggestions(); assert.equal(buttons.length, 4)
    buttons[0].props.onClick(); app.render()
    assert.ok(app.textarea().props.value.length > 0); assert.equal(calls(), 0)
    setReply(() => { throw new TypeError('offline') }); await app.send('Draft')
    assert.equal(app.textarea().props.value, 'Draft'); assert.equal(app.textarea().props.disabled, true)
    app.enter(); app.submit(); assert.equal(calls(), 1)
    app.retry(); assert.equal(app.textarea().props.disabled, true)
    await tick(); app.render(); assert.equal(app.textarea().props.disabled, false); assert.equal(calls(), 1)
  })
  for (const result of [{ ok: false }, { ok: true, json: async () => ({}) }, { ok: true, json: async () => { throw new Error('json') } }]) {
    await scenario(async (app, setReply, calls) => {
      assert.equal(app.textarea().props.disabled, true); assert.ok(app.warning())
      app.write('Draft'); app.enter(); app.submit(); assert.equal(calls(), 0)
    }, () => result)
  }
  await scenario(async (app, setReply, calls) => {
    assert.equal(app.textarea().props.disabled, true)
    app.write('Draft'); app.enter(); app.submit(); assert.equal(calls(), 0)
    ;[...timers.values()].find(timer => timer.ms === 10000).fn()
    await tick(); app.render(); assert.equal(app.textarea().props.disabled, true); assert.ok(app.warning())
  }, options => new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('timeout')))))
  console.log('Chat checks passed: daily/session/quota/service/network/disabled/success, history, deduplication, timeout and rerender/concurrent submission')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
