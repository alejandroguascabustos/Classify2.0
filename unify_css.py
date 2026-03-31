import os

css_dir = 'src/main/resources/static/css/'
output_file = os.path.join(css_dir, 'style_unified.css')

files_to_merge = [
    ('style.css', 'GLOBAL BASE STYLES'),
    ('aprende.css', 'MODULO APRENDE'),
    ('chatbot.css', 'CHATBOT SETTINGS'),
    ('contacto.css', 'CONTACTO PAGE'),
    ('error-debug.css', 'ERROR DEBUG STYLES'),
    ('error-pages.css', 'ERROR PAGES'),
    ('izada.css', 'IZADA SECTION'),
    ('nosotros.css', 'NOSOTROS PAGE'),
    ('notas-styles.css', 'NOTAS STYLES')
]

with open(output_file, 'w', encoding='utf-8') as outfile:
    for filename, label in files_to_merge:
        filepath = os.path.join(css_dir, filename)
        if os.path.exists(filepath):
            outfile.write(f"\n\n/* {'='*73}\n   {label} - {filename}\n   {'='*73} */\n\n")
            with open(filepath, 'r', encoding='utf-8') as infile:
                outfile.write(infile.read())
            print(f"Merged {filename}")
        else:
            print(f"Skipping {filename} (not found)")

# Final rename and cleanup
style_original = os.path.join(css_dir, 'style.css')
os.replace(output_file, style_original)
print(f"Updated {style_original}")

for filename, _ in files_to_merge:
    if filename != 'style.css':
        filepath = os.path.join(css_dir, filename)
        if os.path.exists(filepath):
            os.remove(filepath)
            print(f"Deleted {filename}")
