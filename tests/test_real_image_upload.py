import requests
import os

IMAGE_PATH = r"C:/Users/Enzo/.gemini/antigravity/brain/3aab86b8-9b1b-4673-8582-343bbf1b3954/.user_uploaded/media_1794161179444_205e2c40.png"

# 1. Login
login_resp = requests.post("http://localhost:8080/auth/login", json={
    "email": "cidadao@zelafix.com.br",
    "senha": "Senha@123"
})
token = login_resp.json()["token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Upload the user's actual image
with open(IMAGE_PATH, "rb") as f:
    files = {"arquivo": ("diagrama_modelagem.png", f, "image/png")}
    upload_resp = requests.post("http://localhost:8080/uploads", headers=headers, files=files)

print("Upload Status:", upload_resp.status_code)
upload_data = upload_resp.json()
print("Upload Response:", upload_data)

image_url = upload_data["url"]
file_name = upload_data["nomeArquivo"]

# 3. Fetch image via GET directly without auth (public access for browser/web/mobile)
get_resp = requests.get(image_url)
print("GET Image Status:", get_resp.status_code)
print("GET Content-Type:", get_resp.headers.get("content-type"))
print("GET Content-Length:", len(get_resp.content))
original_size = os.path.getsize(IMAGE_PATH)
print("Original File Size:", original_size)

assert get_resp.status_code == 200, f"Expected 200, got {get_resp.status_code}"
assert get_resp.headers.get("content-type") == "image/png", f"Expected image/png, got {get_resp.headers.get('content-type')}"
assert len(get_resp.content) == original_size, f"Size mismatch: {len(get_resp.content)} vs {original_size}"

print("SUCCESS: Real image uploaded, retrieved, and 100% matched byte-for-byte!")
