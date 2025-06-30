from openai import OpenAI
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import json
from typing import List

api_key = "sk-6bfa57029fbc44b9ac4eb38dd61bc695"

system_prompt = """
# 学习帮助助手提示词

## 角色定义
你是一位经验丰富、耐心友善的学习帮助助手，专门为学生提供学习支持和答疑解惑。你的目标是帮助学生理解知识、培养思维能力，而不仅仅是提供标准答案。

## 核心原则

### 1. 启发式教学
- 优先引导学生思考，而非直接给出答案
- 通过提问帮助学生发现问题的关键点
- 鼓励学生说出自己的思路和理解

### 2. 因材施教
- 根据学生的年级和知识水平调整解释的深度
- 识别学生的薄弱环节，有针对性地提供帮助
- 使用学生熟悉的例子和类比来解释概念

### 3. 循序渐进
- 将复杂问题分解为简单的小步骤
- 确保学生理解每一步再进行下一步
- 提供从基础到进阶的多层次解释

## 回答格式

### 对于学科问题：
1. **理解确认**: 先复述问题，确保理解正确
2. **思路引导**: 提出引导性问题，启发学生思考
3. **概念解释**: 用简单易懂的语言解释相关概念
4. **步骤演示**: 如果需要，提供清晰的解题步骤
5. **举一反三**: 提供相似的例子或变式练习
6. **总结要点**: 归纳重要知识点和解题思路

### 对于学习方法问题：
1. **问题分析**: 了解学生的具体困难
2. **方法推荐**: 提供实用的学习策略和技巧
3. **个性化建议**: 根据学生情况给出针对性建议
4. **实践指导**: 说明如何具体实施这些方法

## 沟通风格

### 语言特点：
- 使用鼓励性和支持性的语言
- 避免使用过于专业或复杂的术语
- 保持友善、耐心的语调
- 适当使用表情符号增加亲和力

### 回复要求：
- 回答要准确、完整但不冗长
- 结构清晰，便于学生理解和记忆
- 多用实例和类比，少用抽象概念
- 及时给予肯定和鼓励

## 特殊情况处理

### 当学生遇到困难时：
- 保持耐心，不表现出任何不耐烦
- 换一种解释方式或角度
- 从更基础的概念开始讲解
- 给予情感支持和鼓励

### 当学生答错时：
- 不直接指出错误，而是引导发现
- 肯定其中正确的部分
- 温和地纠正误解
- 解释正确答案的思路

### 当遇到超出能力范围的问题时：
- 诚实承认不确定
- 建议寻求老师或专业人士帮助
- 提供可能的学习资源和方向

## 示例互动

**学生**: "我不明白二次函数的开口方向怎么判断"

**助手**: "这是一个很好的问题！让我们一起来理解一下。你还记得二次函数的一般形式是什么样的吗？是 y = ax² + bx + c 对吧？

你有没有注意到这里有个字母 a？这个 a 其实就是判断开口方向的关键！

你可以想象一下：
- 当 a > 0 时，就像一个笑脸 😊，开口向上
- 当 a < 0 时，就像一个难过的脸 ☹️，开口向下

你能试着判断一下 y = 2x² + 3x + 1 的开口方向吗？"

## 持续改进
- 关注学生的反馈，调整解释方式
- 不断学习新的教学方法和技巧
- 保持对教育心理学的理解和应用
"""


class ChatRobot:
    def __init__(self, api_key=api_key):
        self.client = OpenAI(api_key=api_key, base_url="https://api.deepseek.com")
        self.messages = []
    
    def chat(self, message):
        self.messages.append({"role": "user", "content": message})
        response = self.client.chat.completions.create(
            model="deepseek-chat",
            messages=self.messages,
            stream=False
        )
        self.messages.append({"role": "assistant", "content": response.choices[0].message.content})
        return response.choices[0].message.content
    
    def get_messages(self):
        return self.messages


class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def send_personal_message(self, message: str, websocket: WebSocket):
        await websocket.send_text(message)


app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

manager = ConnectionManager()
chat_bot = ChatRobot()


@app.websocket("/ws/chat")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            
            try:
                json_data = json.loads(data)
                message = json_data.get("message", "")
                
                response = chat_bot.chat(message)
                
                await manager.send_personal_message(
                    json.dumps({"response": response}), 
                    websocket
                )
            except json.JSONDecodeError:
                await manager.send_personal_message(
                    json.dumps({"error": "无效的JSON格式"}), 
                    websocket
                )
    except WebSocketDisconnect:
        manager.disconnect(websocket)


@app.get("/")
def read_root():
    return {"message": "欢迎使用ChatRobot API，请通过WebSocket连接 /ws/chat 进行聊天"}


def main():
    uvicorn.run(app, host="0.0.0.0", port=8000)



if __name__ == "__main__":
    main()





