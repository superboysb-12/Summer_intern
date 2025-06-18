import json
import os
import re
from pathlib import Path

class HeadingChunksCleaner:
    def __init__(self, input_dir="segmented_output", output_dir="wash_dir"):
        """初始化标题分块数据清洗器"""
        self.input_dir = Path(input_dir)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
    def clean_chunk(self, chunk_text):
        """清洗单个文本块"""
        # 1. 去除多余的空白字符
        chunk_text = re.sub(r'\s+', ' ', chunk_text).strip()
        
        # 2. 移除图片说明和图表引用
        chunk_text = re.sub(r'图\d+-\d+.*?\n', '', chunk_text)
        chunk_text = re.sub(r'表\d+-\d+.*?\n', '', chunk_text)
        
        # 3. 移除重复的标题
        lines = chunk_text.split('\n')
        if len(lines) > 1 and lines[0].strip() in lines[1]:
            chunk_text = '\n'.join(lines[1:])
            
        # 4. 移除URL和文件路径
        chunk_text = re.sub(r'https?://\S+', '[URL]', chunk_text)
        chunk_text = re.sub(r'file:///\S+', '[FILE_PATH]', chunk_text)
        
        # 5. 处理代码段，保留代码但标记它们
        code_pattern = re.compile(r'```.*?```', re.DOTALL)
        chunk_text = code_pattern.sub(lambda m: '[CODE_BLOCK]' + m.group(0) + '[/CODE_BLOCK]', chunk_text)
        
        # 6. 规范化标题格式
        chunk_text = re.sub(r'^(\d+\.\d+\.\d+)\s+', r'\1 ', chunk_text)
        chunk_text = re.sub(r'^(\d+\.\d+)\s+', r'\1 ', chunk_text)
        
        # 7. 移除不必要的符号和特殊格式
        chunk_text = chunk_text.replace('$ ', '')
        chunk_text = chunk_text.replace(' $', '')
        
        # 8. 移除非必要的注释标记
        chunk_text = re.sub(r'<!--.*?-->', '', chunk_text)
        
        # 9. 修复错误的标点
        chunk_text = chunk_text.replace('，.', '。')
        chunk_text = chunk_text.replace(',.', '。')
        chunk_text = chunk_text.replace('..', '。')
        
        # 10. 修复中英文混排空格问题
        chunk_text = re.sub(r'([a-zA-Z])([\u4e00-\u9fa5])', r'\1 \2', chunk_text)
        chunk_text = re.sub(r'([\u4e00-\u9fa5])([a-zA-Z])', r'\1 \2', chunk_text)
        
        return chunk_text.strip()

    def clean_chunks_file(self, file_path):
        """清洗一个分块文件中的所有文本块"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                
            # 清洗每个文本块
            cleaned_chunks = []
            for chunk in data['chunks']:
                cleaned_chunk = self.clean_chunk(chunk)
                
                # 过滤掉太短或者无意义的块
                if len(cleaned_chunk) > 20 and not cleaned_chunk.isspace():
                    cleaned_chunks.append(cleaned_chunk)
            
            # 更新分块数据
            data['chunk_count'] = len(cleaned_chunks)
            data['chunks'] = cleaned_chunks
            
            # 保存清洗后的数据
            output_file = self.output_dir / file_path.name
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
                
            print(f"已清洗: {file_path.name}, 清洗前: {len(data['chunks'])}, 清洗后: {len(cleaned_chunks)}")
            return len(cleaned_chunks)
        except Exception as e:
            print(f"处理文件 {file_path} 时出错: {e}")
            return 0
            
    def process_directory(self):
        """处理目录中的所有分块文件"""
        total_cleaned = 0
        total_files = 0
        
        # 获取所有JSON文件
        json_files = list(self.input_dir.glob('*.json'))
        
        for file_path in json_files:
            cleaned_count = self.clean_chunks_file(file_path)
            total_cleaned += cleaned_count
            total_files += 1
        
        print(f"\n清洗完成，总共处理了 {total_files} 个文件，保留了 {total_cleaned} 个文本块。")
        print(f"结果保存在 {self.output_dir} 目录中。")

def main():
    wash_dir = "wash_dir"
    os.makedirs(wash_dir, exist_ok=True)
    input_dir = "segmented_output"

    cleaner = HeadingChunksCleaner(input_dir, wash_dir)
    cleaner.process_directory()

if __name__ == "__main__":
    main() 