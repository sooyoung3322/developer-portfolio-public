// Effect-level checks for progress math, scheduling and lifecycle cleanup.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync(require('node:path').join(__dirname, '../src/components/ProjectProgress.tsx'), 'utf8')
const code = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX,
} }).outputText
let effect, transform, resize, disconnected = 0
const frames = new Map(), listeners = new Map()
let serial = 0
const target = prefix => ({
  addEventListener: (name, callback) => listeners.set(prefix + name, callback),
  removeEventListener: (name, callback) => {
    assert.equal(listeners.get(prefix + name), callback)
    listeners.delete(prefix + name)
  },
})
const win = { ...target('window:'), scrollY: 0, innerHeight: 800 }
const doc = { ...target('document:'), documentElement: { scrollHeight: 2400 }, body: { style: {} } }
const exportsObject = {}
vm.runInNewContext(code, {
  exports: exportsObject,
  require: name => {
    if (name === 'react') return {
      useRef: () => ({ current: { style: { setProperty: (_, value) => { transform = value } } } }),
      useEffect: callback => { effect = callback },
    }
    if (name === 'react/jsx-runtime') return { jsx: () => null }
    throw Error(name)
  },
  window: win, document: doc,
  requestAnimationFrame: callback => { frames.set(++serial, callback); return serial },
  cancelAnimationFrame: id => frames.delete(id),
  ResizeObserver: class { constructor(callback) { resize = callback } observe() {} disconnect() { disconnected++ } },
})
function flush() { const callbacks = [...frames.values()]; frames.clear(); callbacks.forEach(callback => callback()) }
function scroll(y, expected) { win.scrollY = y; listeners.get('window:scroll')(); flush(); assert.equal(transform, `scaleX(${expected})`) }
for (let mount = 0; mount < 3; mount++) {
  win.scrollY = 0; doc.documentElement.scrollHeight = 2400
  exportsObject.ProjectProgress()
  const cleanup = effect()
  flush(); assert.equal(transform, 'scaleX(0)')
  scroll(800, .5); scroll(1600, 1); scroll(-10, 0); scroll(1800, 1)
  listeners.get('window:scroll')(); listeners.get('window:scroll')()
  assert.equal(frames.size, 1, 'Coalesce scroll events into one animation frame')
  flush()
  doc.body.style.position = 'fixed'; scroll(0, 1); doc.body.style.position = ''
  win.scrollY = 800; listeners.get('window:pageshow')(); flush(); assert.equal(transform, 'scaleX(0.5)')
  doc.documentElement.scrollHeight = 4000; resize(); flush(); assert.equal(transform, 'scaleX(0.25)')
  doc.documentElement.scrollHeight = 800; resize(); flush(); assert.equal(transform, 'scaleX(0)')
  listeners.get('window:scroll')(); cleanup()
  assert.equal(frames.size, 0); assert.equal(listeners.size, 0)
}
assert.equal(disconnected, 3)
console.log('Project progress checks passed: bounds, short pages, restoration, resized content, dialog lock, frame coalescing, remount and cleanup')
