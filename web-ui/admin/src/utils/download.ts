/** 浏览器端文件下载工具 */

/**
 * 保存二进制响应（Excel 导出/模板下载）
 * @param blob 响应体
 * @param filename 文件名
 */
export function saveBlob(blob: Blob, filename: string): void {
  // 后端异常时会返回 JSON 而不是文件，这里做个友好提示
  if (blob.type.includes('application/json')) {
    blob.text().then((text) => {
      try {
        const res = JSON.parse(text) as { msg?: string }
        console.error('下载失败：', res.msg || text)
      } catch {
        console.error('下载失败：', text)
      }
    })
    return
  }
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
