import os
import re
from docx import Document
import docx2txt
from pathlib import Path
import win32com.client
import pythoncom
import time
from tqdm import tqdm
import json

class TextSegmenter:
    def __init__(self, text_dir="source_article", output_dir="segmented_output"):

        self.text_dir = Path(text_dir)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
    def _doc_to_docx(self, doc_path):
        docx_path = doc_path.with_suffix('.docx')
        
        if docx_path.exists():
            return docx_path
            
        print(f"转换 {doc_path} 为 DOCX 格式...")
        try:
            # 初始化COM对象
            pythoncom.CoInitialize()
            word = win32com.client.Dispatch("Word.Application")
            word.Visible = False
            
            # 打开文档
            doc = word.Documents.Open(str(doc_path.absolute()))
            # 保存为docx
            doc.SaveAs(str(docx_path.absolute()), 16)  # 16表示docx格式
            doc.Close()
            word.Quit()
            
            return docx_path
        except Exception as e:
            print(f"转换文档时出错: {e}")
            return None
    
    def extract_text_from_file(self, file_path):
        """从Word文件中提取文本"""
        file_path = Path(file_path)
        
        if file_path.suffix.lower() == '.doc':
            docx_path = self._doc_to_docx(file_path)
            if docx_path is None:
                print(f"无法从 {file_path} 提取文本")
                return ""
            file_path = docx_path
        
        try:
            doc = Document(file_path)
            full_text = []
            
            # 提取标题
            title = file_path.stem
            full_text.append(f"# {title}\n")
            
            # 提取正文段落
            for para in doc.paragraphs:
                if para.text.strip():
                    full_text.append(para.text)
            
            return "\n".join(full_text)
        except Exception as e:
            print(f"读取文件 {file_path} 时出错: {e}")
            # 尝试使用docx2txt作为备选方案
            try:
                return docx2txt.process(file_path)
            except:
                return ""
    
    def segment_by_paragraph(self, text, min_length=50):
        """按段落分块，过滤掉太短的段落"""
        paragraphs = text.split('\n')
        return [p for p in paragraphs if len(p.strip()) >= min_length]
            
    def segment_by_heading(self, text):
        """按标题（章节、小节）分块"""
        # 使用正则表达式匹配常见的标题格式
        # 例如：第一章、1.1、## 标题、Title等
        heading_patterns = [
            r'第[一二三四五六七八九十零〇百千万亿\d]+[章节篇].*?(?=第[一二三四五六七八九十零〇百千万亿\d]+[章节篇]|$)',
            r'\d+\.\d+.*?(?=\d+\.\d+|$)',
            r'#+\s.*?(?=#+\s|$)',
            r'[一二三四五六七八九十]+、.*?(?=[一二三四五六七八九十]+、|$)'
        ]
        
        chunks = []
        remaining_text = text
        
        # 尝试所有标题模式，累积匹配结果
        for pattern in heading_patterns:
            pattern_matches = list(re.finditer(pattern, remaining_text, re.DOTALL))
            
            if pattern_matches:
                for match in pattern_matches:
                    chunks.append(match.group(0).strip())
        
        # 如果没有找到标题结构，退回到段落分块
        if not chunks:
            chunks = self.segment_by_paragraph(text)
        
        return chunks
    
    def process_files(self):
        
        result = {}
        
        doc_files = list(self.text_dir.glob('*.doc')) + list(self.text_dir.glob('*.docx'))
        
        for file_path in tqdm(doc_files, desc="处理文件"):
            file_name = file_path.stem
            print(f"\n处理文件: {file_name}")
            
            # 提取文本
            text = self.extract_text_from_file(file_path)
            if not text:
                continue
                
            # 分块
            chunks = self.segment_by_heading(text)
            
            # 保存结果
            result[file_name] = {
                'file_path': str(file_path),
                'chunk_count': len(chunks),
                'chunks': chunks
            }
            
            # 将每个文件的分块结果保存为单独的JSON文件
            output_file = self.output_dir / f"{file_name}.json"
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'file_name': file_name,
                    'segmentation_method': 'heading',
                    'chunk_count': len(chunks),
                    'chunks': chunks
                }, f, ensure_ascii=False, indent=2)
                
            print(f"已分块: {len(chunks)} 个片段，保存至 {output_file}")
        
            
        return result

