"""
Client for interacting with the RAG API.
"""

import aiohttp
import asyncio
import json
from typing import Dict, Any, Optional, List
from urllib.parse import urlencode

from config import RAG_API_URL


class RagClient:
    """Client for interacting with the RAG API."""
    
    def __init__(self, api_url: str = RAG_API_URL):
        """Initialize the RAG client.
        
        Args:
            api_url: URL of the RAG API.
        """
        self.api_url = api_url
        self.session = None
    
    async def __aenter__(self):
        """Create an aiohttp session when entering context manager."""
        self.session = aiohttp.ClientSession()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Close the aiohttp session when exiting context manager."""
        if self.session:
            await self.session.close()
            self.session = None
    
    async def get_rag_results(self, query: str) -> Dict[str, Any]:
        """Get RAG results for a query.
        
        Args:
            query: The search query.
            
        Returns:
            Dictionary containing the RAG results.
        """
        if not self.session:
            self.session = aiohttp.ClientSession()
            need_to_close = True
        else:
            need_to_close = False
        
        params = {'query': query}
        url = f"{self.api_url}?{urlencode(params)}"
        
        try:
            async with self.session.get(url) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    error_text = await response.text()
                    raise Exception(f"RAG API error: {response.status} - {error_text}")
        finally:
            if need_to_close:
                await self.session.close()
                self.session = None
    
    @staticmethod
    def extract_results(response: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Extract relevant results from the RAG API response.
        
        Args:
            response: The raw response from the RAG API.
            
        Returns:
            List of relevant results.
        """
        if not response.get("success", False):
            raise Exception(f"RAG API error: {response.get('message', 'Unknown error')}")
        
        return response.get("results", [])


async def fetch_rag_results(query: str) -> Dict[str, Any]:
    """Helper function to fetch RAG results for a query.
    
    Args:
        query: The search query.
        
    Returns:
        Dictionary containing the RAG results.
    """
    async with RagClient() as client:
        return await client.get_rag_results(query) 