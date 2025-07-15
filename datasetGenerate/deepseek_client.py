"""
Client for interacting with the DeepSeek API using OpenAI's SDK.
"""

import asyncio
from typing import Dict, Any, Optional, List
import logging
import json

from openai import AsyncOpenAI
from openai.types.chat import ChatCompletion

from config import DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL


class DeepSeekClient:
    """Client for interacting with the DeepSeek API."""
    
    def __init__(
        self,
        api_key: str = DEEPSEEK_API_KEY,
        base_url: str = DEEPSEEK_BASE_URL,
        model: str = DEEPSEEK_MODEL
    ):
        """Initialize the DeepSeek client.
        
        Args:
            api_key: API key for the DeepSeek API.
            base_url: Base URL for the DeepSeek API.
            model: Model to use for generating completions.
        """
        self.client = AsyncOpenAI(api_key=api_key, base_url=base_url)
        self.model = model
    
    async def generate_question(
        self,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.7,
        max_tokens: int = 2000,
        top_p: float = 0.95
    ) -> str:
        """Generate a question using the DeepSeek API.
        
        Args:
            system_prompt: System prompt to guide the model.
            user_prompt: User prompt containing the query and RAG results.
            temperature: Sampling temperature (higher = more creative).
            max_tokens: Maximum number of tokens to generate.
            top_p: Nucleus sampling parameter.
            
        Returns:
            Generated question text.
        """
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=temperature,
                max_tokens=max_tokens,
                top_p=top_p,
                stream=False
            )
            
            # 确保我们得到了一个有效的响应
            if not isinstance(response, ChatCompletion):
                raise ValueError(f"Unexpected response type: {type(response)}")
                
            if not hasattr(response, 'choices') or len(response.choices) == 0:
                raise ValueError(f"No choices in response: {response}")
            
            content = response.choices[0].message.content
            
            # 打印出原始返回内容以便调试
            logging.debug(f"Raw DeepSeek API response content: {content}")
            
            # 确保内容不为空
            if not content:
                raise ValueError("Empty response from DeepSeek API")
                
            return content
            
        except Exception as e:
            logging.error(f"DeepSeek API error: {str(e)}")
            # 返回一个错误消息而不是直接抛出异常，这样我们的流程可以继续
            return f"ERROR: {str(e)}"


async def generate_question(
    system_prompt: str,
    user_prompt: str,
    temperature: float = 0.7,
    max_tokens: int = 2000,
    top_p: float = 0.95
) -> str:
    """Helper function to generate a question using the DeepSeek API.
    
    Args:
        system_prompt: System prompt to guide the model.
        user_prompt: User prompt containing the query and RAG results.
        temperature: Sampling temperature (higher = more creative).
        max_tokens: Maximum number of tokens to generate.
        top_p: Nucleus sampling parameter.
        
    Returns:
        Generated question text.
    """
    client = DeepSeekClient()
    return await client.generate_question(
        system_prompt,
        user_prompt,
        temperature,
        max_tokens,
        top_p
    ) 