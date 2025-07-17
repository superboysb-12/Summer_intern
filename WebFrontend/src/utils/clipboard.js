/**
 * 复制内容到剪贴板
 * @param {string} text 要复制的文本
 * @returns {Promise} 复制操作的Promise
 */
export const copyToClipboard = (text) => {
  // 优先使用现代的navigator.clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text);
  } else {
    // 兼容方案：创建临时textarea元素
    return new Promise((resolve, reject) => {
      try {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        
        // 设置样式使其不可见
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        
        // 选中文本并复制
        textArea.focus();
        textArea.select();
        const success = document.execCommand('copy');
        
        // 清理DOM
        document.body.removeChild(textArea);
        
        if (success) {
          resolve();
        } else {
          reject(new Error('复制失败'));
        }
      } catch (err) {
        reject(err);
      }
    });
  }
}; 