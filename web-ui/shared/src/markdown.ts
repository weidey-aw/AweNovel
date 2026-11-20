/**
 * 极简 Markdown 渲染（游戏简介 / 文章正文）
 *
 * 设计取舍：
 * - 先整体转义 HTML，再生成受控标签，避免后端/用户内容注入脚本；
 * - 支持标题、粗体、斜体、删除线、行内代码、代码块（含语法高亮）、有序/无序列表、引用、分割线、链接、图片；
 * - `![alt](xxx.mp4)` 会被渲染成 <video>，用于简介内嵌视频；
 * - 代码块使用 highlight.js 高亮，输出 `hljs` 类，配合主题变量着色。
 */
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import yaml from 'highlight.js/lib/languages/yaml'
import markdownLang from 'highlight.js/lib/languages/markdown'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'

/** 注册常用语言（体积可控，覆盖 Galgame 站点常见场景） */
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('json', json)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('markdown', markdownLang)
hljs.registerLanguage('md', markdownLang)
hljs.registerLanguage('c', c)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)

export const CODE_LANGUAGES: { value: string; label: string }[] = [
  { value: 'javascript', label: 'JavaScript' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'java', label: 'Java' },
  { value: 'python', label: 'Python' },
  { value: 'sql', label: 'SQL' },
  { value: 'bash', label: 'Shell' },
  { value: 'json', label: 'JSON' },
  { value: 'html', label: 'HTML' },
  { value: 'css', label: 'CSS' },
  { value: 'yaml', label: 'YAML' },
  { value: 'cpp', label: 'C++' },
  { value: 'go', label: 'Go' },
  { value: 'rust', label: 'Rust' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'text', label: '纯文本' },
]

const ESCAPE_MAP: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
}

export function escapeHtml(input: string): string {
  return input.replace(/[&<>"']/g, (ch) => ESCAPE_MAP[ch] ?? ch)
}

const VIDEO_RE = /\.(mp4|webm|ogg|ogv|mov|m4v)(\?.*)?$/i

/** 高亮代码块内容（未知语言则自动识别，失败退回纯文本转义） */
export function highlightCode(code: string, lang?: string): string {
  const language = (lang ?? '').trim().toLowerCase()
  if (language && language !== 'text' && hljs.getLanguage(language)) {
    try {
      return hljs.highlight(code, { language, ignoreIllegals: true }).value
    } catch {
      /* 高亮失败时退回纯文本 */
    }
  }
  try {
    return hljs.highlightAuto(code).value
  } catch {
    return escapeHtml(code)
  }
}

function codeBlock(code: string, lang?: string): string {
  const language = (lang ?? '').trim().toLowerCase()
  const cls = language && language !== 'text' ? ` class="hljs language-${escapeHtml(language)}"` : ' class="hljs"'
  return `<pre class="md-code"><code${cls}>${highlightCode(code, language)}</code></pre>`
}

function inline(text: string): string {
  let t = text

  // 行内代码（先处理，避免内部内容被其它规则改写）
  t = t.replace(/`([^`]+)`/g, (_, code) => `<code>${code}</code>`)

  // 图片 / 视频
  t = t.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_m, alt: string, url: string) => {
    const src = url.replace(/"/g, '%22')
    if (VIDEO_RE.test(url)) {
      return `<video class="md-video" controls preload="metadata" src="${src}"></video>`
    }
    return `<img class="md-img" src="${src}" alt="${alt}" loading="lazy" />`
  })

  // 链接
  t = t.replace(
    /\[([^\]]+)\]\(([^)\s]+)\)/g,
    (_m, label: string, url: string) =>
      `<a href="${url}" target="_blank" rel="noopener noreferrer">${label}</a>`,
  )

  // 粗体、斜体、删除线
  t = t.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  t = t.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
  t = t.replace(/~~([^~\n]+)~~/g, '<del>$1</del>')

  return t
}

export function renderMarkdown(md?: string | null): string {
  if (!md) return ''
  const lines = md.replace(/\r\n/g, '\n').split('\n')
  const out: string[] = []
  let inCode = false
  let codeLang = ''
  let codeBuf: string[] = []
  let listType: 'ul' | 'ol' | null = null

  const closeList = () => {
    if (listType) {
      out.push(`</${listType}>`)
      listType = null
    }
  }

  for (const line of lines) {
    // 代码块
    if (line.trim().startsWith('```')) {
      if (!inCode) {
        inCode = true
        codeLang = line.trim().slice(3).trim()
        codeBuf = []
      } else {
        inCode = false
        out.push(codeBlock(codeBuf.join('\n'), codeLang))
      }
      continue
    }
    if (inCode) {
      codeBuf.push(line)
      continue
    }

    // 列表
    const ulMatch = line.match(/^\s*[-*+]\s+(.*)$/)
    const olMatch = line.match(/^\s*\d+[.)]\s+(.*)$/)
    if (ulMatch) {
      if (listType !== 'ul') {
        closeList()
        out.push('<ul>')
        listType = 'ul'
      }
      out.push(`<li>${inline(escapeHtml(ulMatch[1]))}</li>`)
      continue
    }
    if (olMatch) {
      if (listType !== 'ol') {
        closeList()
        out.push('<ol>')
        listType = 'ol'
      }
      out.push(`<li>${inline(escapeHtml(olMatch[1]))}</li>`)
      continue
    }
    closeList()

    if (!line.trim()) continue

    // 标题
    const heading = line.match(/^(#{1,6})\s+(.*)$/)
    if (heading) {
      const level = heading[1].length
      out.push(`<h${level}>${inline(escapeHtml(heading[2]))}</h${level}>`)
      continue
    }

    // 分割线
    if (/^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(line)) {
      out.push('<hr />')
      continue
    }

    // 引用
    if (/^\s*>\s?/.test(line)) {
      out.push(`<blockquote>${inline(escapeHtml(line.replace(/^\s*>\s?/, '')))}</blockquote>`)
      continue
    }

    out.push(`<p>${inline(escapeHtml(line))}</p>`)
  }

  if (inCode) {
    out.push(codeBlock(codeBuf.join('\n'), codeLang))
  }
  closeList()

  return out.join('\n')
}

/** 生成图片 Markdown */
export function mdImage(url: string, alt = 'image'): string {
  return `![${alt}](${url})`
}

/** 生成视频 Markdown（渲染为 <video>） */
export function mdVideo(url: string, alt = 'video'): string {
  return `![${alt}](${url})`
}

/** 生成代码块 Markdown（插入编辑器用） */
export function mdCodeBlock(lang = 'text', code = ''): string {
  return `\n\`\`\`${lang}\n${code}\n\`\`\`\n`
}
