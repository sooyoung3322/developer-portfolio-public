import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'
import assert from 'node:assert/strict'
import ts from 'typescript'
const root = resolve(import.meta.dirname, '..')
const source = readFileSync(resolve(root, 'src/data/projects.ts'), 'utf8')
const code = ts.transpile(source, { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 })
const { projects } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'))
assert.equal(new Set(projects.map(project => project.id)).size, projects.length)
for (const project of projects) {
  assert.ok(project.title && project.period && project.team && project.stack.length)
  for (const image of project.images) assert.ok(existsSync(resolve(root, 'public', image.src.slice(1))), image.src)
}
console.log('Public content checks passed: unique routes, complete fixtures and local placeholders')
