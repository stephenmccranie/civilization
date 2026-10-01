"""Export the current Blockbench project through native codecs, preserving an editable source.

python tools/modeling/export_project.py art/assets/example --name example
Use --replace only when intentionally updating an existing exported version.
"""
import argparse
import base64
import json
from pathlib import Path
import re
from mcp import Client
from proof import evaluate, export, load_project

ROOT = Path(__file__).resolve().parents[2]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--name', required=True)
    parser.add_argument('--replace', action='store_true')
    parser.add_argument('--project', type=Path, help='Open this saved project before exporting; source is preserved')
    args = parser.parse_args()
    folder = args.directory.resolve()
    if not folder.is_relative_to(ROOT) or not re.fullmatch('[a-z0-9_]+', args.name):
        raise ValueError('Use a project directory and lowercase resource name')
    client = Client()
    try:
        if args.project:
            project = args.project.resolve()
            if not project.is_relative_to(ROOT): raise ValueError('Keep project source in the workspace')
            source = json.loads(project.read_text(encoding='utf-8'))
            fmt = source['meta']['model_format']
            if fmt not in ('java_block', 'geckolib_model'): raise ValueError('Unsupported project format')
            client.call('create_project', {'name': args.name+'_export', 'format': fmt})
            load_project(client, json.dumps(source))
        info = evaluate(client, '({format: Format.id, textures: Texture.all.map(t => t.uuid)})')
        if info['format'] not in ('java_block', 'geckolib_model'):
            raise ValueError('Choose Java Block/Item or GeckoLib format before export')
        data = {f'{args.name}.bbmodel': export(client, 'project').encode()}
        if info['format'] == 'java_block':
            data[f'{args.name}.json'] = export(client, 'java_block').encode()
        else:
            data[f'{args.name}.geo.json'] = export(client, 'bedrock').encode()
            animation = evaluate(client, 'Animator.buildFile(null, Animation.all.map(a => a.name))')
            data[f'{args.name}.animation.json'] = json.dumps(animation, indent=2).encode()
        textures = evaluate(client, 'Texture.all.map(t => ({name:t.name, data:t.getDataURL()}))')
        for index, texture in enumerate(textures):
            texture_name = args.name if len(textures) == 1 else f'{args.name}_{index}'
            data[f'{texture_name}.png'] = base64.b64decode(texture['data'].split(',', 1)[1])
        for name in data:
            if (folder / name).exists() and not args.replace:
                raise ValueError(f'{name} exists; use a new version directory or --replace')
        folder.mkdir(parents=True, exist_ok=True)
        for name, content in data.items(): (folder / name).write_bytes(content)
        print(json.dumps({'format': info['format'], 'files': list(data),
              'next': 'Check exported texture resource references, declare outputs in asset.json and review before publishing.'}))
    finally:
        client.close()

if __name__ == '__main__': main()
