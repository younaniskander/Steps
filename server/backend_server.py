"""
Next-OCR (thelamapi/next-ocr) Local Backend Server for StepSight Android App.

This FastAPI server runs the exact Python code provided by the user using `transformers`
and serves an OpenAI-compatible /v1/chat/completions endpoint that the StepSight app
can directly communicate with.

Requirements:
    pip install fastapi uvicorn transformers torch pillow torchvision accelerate

Run command:
    python backend_server.py
"""

import base64
import io
import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
from PIL import Image
from transformers import AutoProcessor, AutoModelForMultimodalLM

app = FastAPI(title="StepSight Next-OCR Server", version="1.0")

MODEL_ID = "thelamapi/next-ocr"
print(f"Loading {MODEL_ID} processor and model...")
try:
    processor = AutoProcessor.from_pretrained(MODEL_ID)
    model = AutoModelForMultimodalLM.from_pretrained(
        MODEL_ID,
        device_map="auto",
        torch_dtype=torch.float16 if torch.cuda.is_available() else torch.float32
    )
    print(f"Successfully loaded {MODEL_ID} on {model.device}")
except Exception as e:
    print(f"Warning: Model could not load immediately: {e}")
    processor = None
    model = None


class MessageContent(BaseModel):
    type: str
    text: Optional[str] = None
    image_url: Optional[Dict[str, str]] = None


class ChatMessage(BaseModel):
    role: str
    content: List[Dict[str, Any]]


class ChatRequest(BaseModel):
    model: str = "thelamapi/next-ocr"
    messages: List[ChatMessage]
    max_tokens: int = 256


@app.get("/")
def health_check():
    return {
        "status": "online",
        "model": MODEL_ID,
        "device": str(model.device) if model else "not_loaded"
    }


@app.post("/v1/chat/completions")
def chat_completions(req: ChatRequest):
    if model is None or processor is None:
        raise HTTPException(status_code=503, detail="Model is not loaded on this server.")

    try:
        # Extract image and prompt from messages
        prompt_text = "Perform OCR and describe the key visual elements."
        pil_image = None

        for msg in req.messages:
            for item in msg.content:
                item_type = item.get("type")
                if item_type == "text":
                    prompt_text = item.get("text", prompt_text)
                elif item_type == "image_url":
                    img_data = item.get("image_url", {}).get("url", "")
                    if img_data.startswith("data:image"):
                        # Extract base64
                        header, base64_str = img_data.split(",", 1)
                        image_bytes = base64.b64decode(base64_str)
                        pil_image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
                    elif img_data.startswith("http"):
                        import requests
                        resp = requests.get(img_data, timeout=10)
                        pil_image = Image.open(io.BytesIO(resp.content)).convert("RGB")

        if pil_image is None:
            raise HTTPException(status_code=400, detail="No image provided in request.")

        # Build messages for transformers processor as requested:
        hf_messages = [
            {
                "role": "user",
                "content": [
                    {"type": "image", "image": pil_image},
                    {"type": "text", "text": prompt_text}
                ]
            }
        ]

        inputs = processor.apply_chat_template(
            hf_messages,
            add_generation_prompt=True,
            tokenize=True,
            return_dict=True,
            return_tensors="pt"
        ).to(model.device)

        with torch.no_grad():
            outputs = model.generate(**inputs, max_new_tokens=req.max_tokens)

        generated_text = processor.decode(outputs[0][inputs["input_ids"].shape[-1]:], skip_special_tokens=True).strip()

        return {
            "id": "chatcmpl-next-ocr",
            "object": "chat.completion",
            "model": MODEL_ID,
            "choices": [
                {
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": generated_text
                    },
                    "finish_reason": "stop"
                }
            ]
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    # Listens on all interfaces so emulator (10.0.2.2) and local WiFi devices can connect
    uvicorn.run(app, host="0.0.0.0", port=8000)
