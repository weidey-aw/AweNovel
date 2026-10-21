/**
 * 极简 Markdown 渲染（游戏简介用）
 *
 * 设计取舍：
 * - 先整体转义 HTML，再生成受控标签，避免后端/用户内容注入脚本；
 * - 支持标题、粗体、斜体、删除线、行内代码、代码块、有序/无序列表、引用、分割线、链接、图片；
 * - `![alt](xxx.mp4)` 会被渲染成 <video>，用于简介内嵌视频。
 */
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
        codeBuf = []
      } else {
        inCode = false
        out.push(`<pre class="md-code"><code>${escapeHtml(codeBuf.join('\n'))}</code></pre>`)
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
    out.push(`<pre class="md-code"><code>${escapeHtml(codeBuf.join('\n'))}</code></pre>`)
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
