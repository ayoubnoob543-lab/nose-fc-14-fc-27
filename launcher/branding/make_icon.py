from PIL import Image
from pathlib import Path

src = Image.open(Path(__file__).with_name('fc27-logo-source.png')).convert('RGBA')
pix = src.load()
xs, ys = [], []
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = pix[x, y]
        if a > 0 and min(r, g, b) < 120:
            xs.append(x); ys.append(y)
box = (max(0, min(xs)-30), max(0, min(ys)-30), min(src.width, max(xs)+31), min(src.height, max(ys)+31))
logo = src.crop(box)
logo.thumbnail((460, 460), Image.Resampling.LANCZOS)
canvas = Image.new('RGBA', (512, 512), (245, 247, 250, 255))
canvas.alpha_composite(logo, ((512-logo.width)//2, (512-logo.height)//2))
canvas.save(Path(__file__).parent.parent / 'app/src/main/res/drawable/fc27_logo.png', optimize=True)
