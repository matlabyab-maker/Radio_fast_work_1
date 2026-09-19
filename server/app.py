import os, asyncio, subprocess
from urllib.parse import urlparse
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import StreamingResponse, JSONResponse

app = FastAPI(title="Fast Radio Transcoder")
API_KEY = os.getenv("API_KEY", "")
MAX = int(os.getenv("MAX_CONCURRENT_TRANSCODES", "2"))
sem = asyncio.Semaphore(MAX)

FORMATS = {
    "mp3": [16,24,32,48,64,96,128],
    "opus": [16,24,32,48,64,96,128],
    "amr-nb": [4,5,6,7,8,10,12],
}

def check_key(key):
    if not API_KEY or key != API_KEY: raise HTTPException(401, "Invalid API key")

def valid_url(url):
    p=urlparse(url)
    return p.scheme in ("http","https") and bool(p.netloc)

@app.get('/health')
def health(): return {"ok": True}

@app.get('/formats')
def formats(x_api_key: str = Header(default="")):
    check_key(x_api_key); return FORMATS

@app.get('/stream')
async def stream(url: str, codec: str='mp3', bitrate: int=32, x_api_key: str = Header(default="")):
    check_key(x_api_key)
    if not valid_url(url): raise HTTPException(400,"Only http/https source URLs are allowed")
    if codec not in FORMATS or bitrate not in FORMATS[codec]: raise HTTPException(400,"Unsupported codec/bitrate")
    if codec == 'mp3': args=['-vn','-ac','1','-ar','22050','-c:a','libmp3lame','-b:a',f'{bitrate}k','-f','mp3','pipe:1']; media='audio/mpeg'
    elif codec == 'opus': args=['-vn','-ac','1','-ar','24000','-c:a','libopus','-b:a',f'{bitrate}k','-f','ogg','pipe:1']; media='audio/ogg'
    else: args=['-vn','-ac','1','-ar','8000','-c:a','libopencore_amrnb','-b:a',f'{bitrate}k','-f','amr','pipe:1']; media='audio/amr'
    cmd=['ffmpeg','-hide_banner','-loglevel','error','-reconnect','1','-reconnect_streamed','1','-reconnect_delay_max','5','-i',url]+args
    async with sem:
        proc=await asyncio.create_subprocess_exec(*cmd,stdout=asyncio.subprocess.PIPE,stderr=asyncio.subprocess.PIPE)
        async def gen():
            try:
                while True:
                    chunk=await proc.stdout.read(64*1024)
                    if not chunk: break
                    yield chunk
            finally:
                if proc.returncode is None:
                    proc.kill()
                    await proc.wait()
        return StreamingResponse(gen(), media_type=media, headers={'X-Fast-Radio-Transcoded':'1'})
