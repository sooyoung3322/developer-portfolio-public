// Effect-level regression check; browser layout and image decoding are not simulated.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.join(__dirname, '../src/components/ScrollMotion.tsx'), 'utf8')
const code = ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022,
} }).outputText
let effect
let pathname = '/projects/project-one'
const observers = []
function element() {
  const classes = new Set()
  const properties = new Map()
  return {
    dataset: {}, classes, properties,
    classList: { add: name => classes.add(name), remove: name => classes.delete(name) },
    style: { setProperty: (name, value) => properties.set(name, value), removeProperty: name => properties.delete(name) },
    contains: () => false,
    getBoundingClientRect: () => ({ top: 2000, bottom: 2200 }),
  }
}
const heading = element()
const hero = element()
const notes = element()
const elements = [heading, hero, notes]
class Observer {
  constructor(callback) { this.callback = callback; this.targets = new Set(); observers.push(this) }
  observe(target) { this.targets.add(target) }
  disconnect() { this.targets.clear() }
  deliver(isIntersecting) {
    this.callback([...this.targets].map(target => ({ target, isIntersecting })))
  }
}
const preference = { matches: false, addEventListener() {}, removeEventListener() {} }
const exportsObject = {}
vm.runInNewContext(code, {
  exports: exportsObject,
  require: name => {
    if (name === 'react') return { useLayoutEffect: callback => { effect = callback } }
    if (name === 'react-router-dom') return { useLocation: () => ({ pathname }) }
    throw new Error(`Unexpected import: ${name}`)
  },
  window: { IntersectionObserver: Observer, innerHeight: 800, matchMedia: () => preference, addEventListener() {}, removeEventListener() {} },
  document: {
    activeElement: null, addEventListener() {}, removeEventListener() {},
    querySelectorAll: selector => elements.filter(item =>
      (item === heading && selector.includes('.detail-heading')) ||
      (item === hero && selector.includes('.detail > .project-visual')) ||
      (item === notes && selector.includes('.detail-body > aside'))),
  },
  IntersectionObserver: Observer,
})
function mount() { exportsObject.ScrollMotion(); return effect() }
function assertCoreVisible() {
  for (const target of [heading, hero]) {
    assert.equal(target.dataset.motion, undefined, 'Core detail content must never enter the opacity:0 motion state')
  }
}
function check() {
  const cleanup = mount()
  assertCoreVisible()
  assert.equal(notes.dataset.motion, undefined, 'Detail content must not receive entrance animations')
  assert.equal(observers.length, 0, 'Detail pages must not create entrance observers')
  cleanup?.()
  for (const target of elements) {
    assert.deepEqual(target.dataset, {})
    assert.equal(target.classes.size + target.properties.size, 0)
  }
}
check() // Initial effect setup and cleanup.
check() // StrictMode replay.
pathname = '/projects/project-two'
check() // Parameter change, retaining the detail DOM nodes.
pathname = '/projects/project-one'
check() // Re-entry.
console.log('Scroll motion checks passed: initial setup, missing/reordered callbacks, StrictMode cleanup, project change and re-entry')
