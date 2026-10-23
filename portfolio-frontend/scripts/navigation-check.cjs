// Exercise the actual DOM tracker with deterministic browser scroll frames.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
require.extensions['.ts'] = (module, filename) => module._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText, filename)
const { trackSections, sectionAtPosition } = require('../src/components/useSectionNavigation.ts')
const ids = ['about', 'experience', 'work', 'skills', 'contact']
function browser(offset, height = 800) {
  let y = 0, frameId = 0, destination, observed, active
  const listeners = new Map(), frames = new Map(), history = []
  const tops = [500, 1200, 2200, 3400, 3540] // Deliberately short Skills.
  const sections = ids.map((id, i) => ({ id,
    getBoundingClientRect: () => ({ top: tops[i] - y }),
    scrollIntoView: () => { destination = Math.min(4200 - height, tops[i] - offset) },
  }))
  global.Element = class {}
  global.document = { documentElement: { scrollHeight: 4200 }, querySelectorAll: () => sections, querySelector: () => ({}) }
  global.getComputedStyle = () => ({ scrollPaddingTop: `${offset}px` })
  global.ResizeObserver = class { constructor(fn) { observed = fn } observe() {} disconnect() { observed = null } }
  global.requestAnimationFrame = fn => { frames.set(++frameId, fn); return frameId }
  global.cancelAnimationFrame = id => frames.delete(id)
  global.window = { get scrollY() { return y }, innerHeight: height,
    scrollTo: ({ top }) => { y = top; destination = undefined },
    addEventListener: (name, fn) => listeners.set(name, fn), removeEventListener: name => listeners.delete(name),
  }
  const tick = () => { const batch = [...frames.values()]; frames.clear(); batch.forEach(fn => fn()) }
  const event = (name, value) => { listeners.get(name)?.(value); tick() }
  const scroll = value => { y = value; event('scroll') }
  let cleanup = () => {}
  const click = id => { cleanup(); cleanup = trackSections(value => { active = value; history.push(value) }, id) }
  return { click, scroll, tick, event, history, get active() { return active }, get destination() { return destination },
    layout: () => { observed(); tick() }, dispose: () => { cleanup(); assert.equal(frames.size, 0); assert.equal(listeners.size, 0) },
  }
}
for (const offset of [156, 88]) {
  const b = browser(offset)
  for (const [from, to] of [['about', 'skills'], ['work', 'skills'], ['skills', 'contact'], ['work', 'contact'], ['contact', 'experience']]) {
    b.click(from); b.scroll(b.destination)
    assert.equal(b.active, from)
    b.click(to); assert.equal(b.active, to)
    const target = b.destination, start = from === 'contact' ? 3400 : from === 'work' ? 2200 - offset : 500 - offset
    for (let i = 1; i < 10; i++) { b.scroll(start + (target - start) * i / 10); assert.equal(b.active, to) }
    b.scroll(target); assert.equal(b.active, to)
  }
  // Slow bidirectional scrolling, fast jumps, and staying at every boundary.
  b.click('');
  for (const y of [0, 500 - offset, 1200 - offset, 2200 - offset, 3400 - offset, 3540 - offset, 3400,
    3540 - offset - 3, 3400 - offset, 2200 - offset, 1200 - offset, 500 - offset]) {
    const previous = b.active
    b.scroll(y)
    const expected = y >= 3400 ? 'contact' : sectionAtPosition(ids.map((id, i) => ({ id, top: [500, 1200, 2200, 3400, 3540][i] - y })), offset, false, previous)
    assert.equal(b.active, expected)
    for (let n = 0; n < 10; n++) { b.event('scroll'); assert.equal(b.active, expected) }
  }
  b.click('skills'); b.scroll(b.destination); assert.equal(b.active, 'skills') // Direct hash / refresh.
  b.click('about'); b.scroll(1800); b.event('wheel'); assert.equal(b.active, 'experience')
  b.click('contact'); b.scroll(2500); b.event('keydown', { key: 'ArrowUp' }); assert.equal(b.active, 'work')
  b.click('skills'); // Native scroll cancellation fallback must not hold the highlight forever.
  for (let n = 0; n < 9; n++) b.tick()
  assert.equal(b.active, 'work')
  b.click(''); b.scroll(3400 - offset - 4)
  assert.equal(b.active, 'work')
  for (const delta of [-0.5, 0.5, -0.2, 0.8]) { b.scroll(3400 - offset + delta); assert.equal(b.active, 'work') }
  b.scroll(3400 - offset + 3); assert.equal(b.active, 'skills')
  for (const delta of [0.5, -0.5, 0.2, -0.8]) { b.scroll(3400 - offset + delta); assert.equal(b.active, 'skills') }
  b.layout(); b.dispose()
}
console.log('Navigation checks passed: desktop/mobile offsets, click lock, reverse/rapid/slow scroll, short Skills, boundaries, bottom, hashes, interruption, layout and cleanup')

// Tall viewport: Skills and Contact can share the same clamped scroll destination.
const tall = browser(88, 1100)
tall.click('skills'); tall.scroll(tall.destination)
assert.equal(tall.active, 'skills')
tall.event('scroll'); assert.equal(tall.active, 'skills')
tall.event('wheel'); assert.equal(tall.active, 'contact')
tall.click('contact'); tall.scroll(tall.destination); assert.equal(tall.active, 'contact')
tall.dispose()

// Independent expected sections across small viewports and both header offsets.
for (const height of [360, 480, 640]) {
  for (const offset of [88, 156, 170]) {
    const b = browser(offset, height)
    b.click('')
    for (const [top, expected] of [[500, 'about'], [1200, 'experience'], [2200, 'work'], [3400, 'skills'], [3540, 'contact']]) {
      b.scroll(top - offset + 4)
      assert.equal(b.active, expected, `height ${height}, offset ${offset}, ${expected}`)
    }
    b.click('work'); b.scroll(b.destination)
    b.click('skills'); assert.equal(b.active, 'skills')
    b.scroll(3400 - offset - 0.5)
    b.event('wheel')
    assert.equal(b.active, 'skills') // Within the 1px landing tolerance.
    b.click('work'); b.scroll(b.destination)
    b.click('skills'); b.scroll(3400 - offset - 1.5)
    b.event('touchstart')
    assert.equal(b.active, 'work', 'interrupted click must not seed boundary hysteresis')
    b.dispose()
  }
}
console.log('Additional checks passed: Projects → Skills → Contact, short viewports and interrupted-click boundaries')
