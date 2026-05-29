import urllib.request
import base64
import os

# Download logo from URL
url = "https://i.postimg.cc/XXMVPK72/file-000000008edc7207a5b7bdd04383d445.png"
logo_path = "new_logo.png"

print("Downloading logo...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    data = response.read()

with open(logo_path, 'wb') as f:
    f.write(data)

print(f"Downloaded: {len(data)} bytes")

# Convert to base64
b64 = base64.b64encode(data).decode('utf-8')
print(f"Base64 length: {len(b64)}")

# Write to logo_base64.txt
with open(os.path.join('src', 'logo_base64.txt'), 'w') as f:
    f.write(b64)

print("Done! logo_base64.txt updated.")
