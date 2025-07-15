"""
Script to extract queries from JSON chunks and process them using DeepSeek API.
"""

import json
import asyncio
import logging
from typing import List, Dict, Any
import os

from deepseek_client import DeepSeekClient
from config import DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL, MAX_CONCURRENT_REQUESTS

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('query_extraction.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

async def extract_queries_from_chunk(chunk: str, client: DeepSeekClient) -> List[str]:
    """
    Extract relevant queries from a text chunk using DeepSeek API.
    
    Args:
        chunk: The text chunk to extract queries from
        client: DeepSeekClient instance
        
    Returns:
        List of extracted queries
    """
    # System prompt to guide the model
    system_prompt = "You are a helpful AI assistant that extracts relevant search queries from text content. These queries will be used for retrieving knowledge and generating educational content."
    
    # User prompt with instructions and the chunk text
    user_prompt = f"""
Extract 1-3 relevant search queries from the following text. These queries should:
1. Capture key topics or concepts in the text
2. Be specific enough to return useful information
3. Cover different aspects of the content if possible
4. Be 2-10 words in length (preferably short and precise)

Return ONLY the list of queries in this format:
["query 1", "query 2", "query 3"]

Here is the text:
{chunk}
"""
    
    try:
        # Generate queries using DeepSeek API
        response = await client.generate_question(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            temperature=0.3,  # Lower temperature for more focused outputs
            max_tokens=200,   # Limit token count as we only need short queries
            top_p=0.95
        )
        
        # Try to extract the JSON list from the response
        try:
            # Look for text within square brackets
            import re
            match = re.search(r'\[(.*)\]', response, re.DOTALL)
            if match:
                json_str = f"[{match.group(1)}]"
                queries = json.loads(json_str)
                return queries
            else:
                # If no match, try parsing the entire response as JSON
                queries = json.loads(response)
                return queries if isinstance(queries, list) else []
        except json.JSONDecodeError:
            logger.warning(f"Failed to parse JSON response: {response[:100]}...")
            # Return empty list if parsing fails
            return []
            
    except Exception as e:
        logger.error(f"Error extracting queries from chunk: {e}")
        return []

async def process_json_file(file_path: str) -> List[str]:
    """
    Process a JSON file containing chunks and extract queries from each chunk.
    
    Args:
        file_path: Path to the JSON file
        
    Returns:
        List of extracted queries
    """
    # Initialize DeepSeek client
    client = DeepSeekClient(
        api_key=DEEPSEEK_API_KEY,
        base_url=DEEPSEEK_BASE_URL,
        model=DEEPSEEK_MODEL
    )
    
    try:
        # Load JSON file
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Extract chunks from the JSON file
        chunks = data.get('chunks', [])
        if not chunks:
            logger.warning(f"No chunks found in {file_path}")
            return []
        
        logger.info(f"Processing {len(chunks)} chunks from {file_path}")
        
        # Process chunks in batches to limit concurrency
        all_queries = []
        batch_size = MAX_CONCURRENT_REQUESTS
        
        for i in range(0, len(chunks), batch_size):
            batch = chunks[i:i+batch_size]
            logger.info(f"Processing batch {i//batch_size + 1}/{(len(chunks) + batch_size - 1)//batch_size} ({len(batch)} chunks)")
            
            # Process chunks concurrently
            tasks = [extract_queries_from_chunk(chunk, client) for chunk in batch]
            batch_results = await asyncio.gather(*tasks)
            
            # Flatten results and add to all_queries
            for queries in batch_results:
                all_queries.extend(queries)
        
        # Remove duplicates while preserving order
        unique_queries = []
        seen = set()
        for query in all_queries:
            if query not in seen:
                seen.add(query)
                unique_queries.append(query)
        
        logger.info(f"Extracted {len(unique_queries)} unique queries from {file_path}")
        return unique_queries
        
    except Exception as e:
        logger.error(f"Error processing file {file_path}: {e}")
        return []

async def process_all_files(directory: str) -> List[str]:
    """
    Process all JSON files in a directory and extract queries from each.
    
    Args:
        directory: Directory containing JSON files
        
    Returns:
        List of extracted queries from all files
    """
    all_queries = []
    
    try:
        # List all JSON files in the directory
        json_files = [f for f in os.listdir(directory) if f.endswith('.json')]
        
        if not json_files:
            logger.warning(f"No JSON files found in {directory}")
            return []
        
        logger.info(f"Found {len(json_files)} JSON files in {directory}")
        
        # Process each file
        for file_name in json_files:
            file_path = os.path.join(directory, file_name)
            queries = await process_json_file(file_path)
            all_queries.extend(queries)
        
        return all_queries
        
    except Exception as e:
        logger.error(f"Error processing directory {directory}: {e}")
        return []

def append_queries_to_jsonl(queries: List[str], output_file: str) -> None:
    """
    Append queries to a JSONL file.
    
    Args:
        queries: List of queries to append
        output_file: Path to the output JSONL file
    """
    try:
        with open(output_file, 'a', encoding='utf-8') as f:
            for query in queries:
                json_line = json.dumps({"query": query}, ensure_ascii=False)
                f.write(f"{json_line}\n")
        
        logger.info(f"Appended {len(queries)} queries to {output_file}")
        
    except Exception as e:
        logger.error(f"Error appending to {output_file}: {e}")

async def main():
    """Main function to run the query extraction process."""
    try:
        # Define input and output paths
        raw_data_dir = "RawData"
        output_file = "test_input.jsonl"
        
        # Process all JSON files in the directory
        queries = await process_all_files(raw_data_dir)
        
        if queries:
            # Append unique queries to the output file
            append_queries_to_jsonl(queries, output_file)
            logger.info(f"Successfully processed all files and extracted {len(queries)} unique queries")
        else:
            logger.warning("No queries extracted from any files")
            
    except Exception as e:
        logger.error(f"Error in main process: {e}")

if __name__ == "__main__":
    asyncio.run(main()) 