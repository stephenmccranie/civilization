"""Create a disposable native Blockbench model, animate, export, reopen, and capture views.

Requires the editor with both pinned plugins loaded. Does not overwrite an existing proof.
"""
import base64
import argparse
import json
from pathlib import Path
from mcp import Client

ROOT = Path(__file__).resolve().parents[2]
ASSET = ROOT / 'art/models/engine_proof'

def text_result(result):
    text = '\n'.join(block['text'] for block in result.get('content', []) if block['type'] == 'text')
    if text.startswith('Error executing code:'):
        raise RuntimeError(text)
    return json.loads(text)

def evaluate(client, code):
    return text_result(client.call('risky_eval', {'code': code}))

def export(client, codec):
    result = text_result(client.call('export_model', {'codec_id': codec, 'max_content_length': 2_000_000}))
    if result.get('truncated') or not result.get('content'):
        raise RuntimeError(f'Incomplete native {codec} export')
    return result['content']


def load_project(client, source):
    """Load native project JSON without one enormous JavaScript object literal.

    Detailed sources can freeze the editor when sent as eval syntax. Transfer
    bounded string chunks and let JSON.parse read the data before the native codec.
    The caller selects/creates the destination project first.
    """
    evaluate(client, "globalThis.civilizationProjectSource='';true")
    for start in range(0, len(source), 60000):
        chunk = json.dumps(source[start:start+60000]).replace('/', '\\u002f')
        evaluate(client, 'globalThis.civilizationProjectSource+='+chunk+';true')
    return evaluate(client, '(() => {try {Codecs.project.parse(JSON.parse(globalThis.civilizationProjectSource));return true;} finally {delete globalThis.civilizationProjectSource;}})()')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify', action='store_true', help='Reopen existing source and check native exports without replacing it')
    args = parser.parse_args()
    if args.verify:
        client = Client()
        try:
            verify(client)
        finally:
            client.close()
        return
    if (ASSET / 'engine_proof.bbmodel').exists():
        raise SystemExit('Proof already exists. Open/edit that project; do not overwrite authored work.')
    client = Client()
    try:
        client.call('create_project', {'name': 'engine_proof', 'format': 'geckolib_model'})
        source = Path(__file__).with_name('engine_proof.js').read_text()
        # Upstream eval rejects comments; this fixture only uses whole-line comments.
        source = '\n'.join(line for line in source.splitlines() if not line.lstrip().startswith('//'))
        evaluate(client, source)
        client.call('create_animation', {'name': 'engine_proof.cycle', 'loop': True, 'animation_length': 2,
            'bones': {'flywheel': [{'time': t, 'rotation': [t * 180, 0, 0]} for t in [0, .5, 1, 1.5, 2]],
                      'piston': [{'time': t, 'position': [x, 0, 0]} for t, x in [(0, 0), (.5, 1.5), (1, 0), (1.5, -1.5), (2, 0)]]}})
        project = export(client, 'project')
        geometry = export(client, 'bedrock')
        # Animation export is an Animator API rather than a model Codec in this editor.
        animation = evaluate(client, 'Animator.buildFile(null, Animation.all.map(a => a.name))')
        texture = evaluate(client, 'Texture.all[0].getDataURL()')
        ASSET.mkdir(parents=True, exist_ok=True)
        (ASSET / 'engine_proof.bbmodel').write_text(project, encoding='utf-8')
        (ASSET / 'engine_proof.geo.json').write_text(geometry, encoding='utf-8')
        (ASSET / 'engine_proof.animation.json').write_text(json.dumps(animation, indent=2), encoding='utf-8')
        (ASSET / 'engine_proof.png').write_bytes(base64.b64decode(texture.split(',', 1)[1]))
        verify(client)
        print('Native model/animation exports and editor views saved:', ASSET)
    finally:
        client.close()

def verify(client):
    project = (ASSET / 'engine_proof.bbmodel').read_text(encoding='utf-8')
    client.call('create_project', {'name': 'engine_proof_review', 'format': 'geckolib_model'})
    # JSON-escape slashes because upstream's comment filter also matches embedded PNG data.
    safe = project.replace('/', '\\u002f')
    counts = evaluate(client, f'Codecs.project.parse({safe}); ({{bones: Group.all.length, cubes: Cube.all.length}})')
    if counts != {'bones': 3, 'cubes': 17}:
        raise RuntimeError(f'Unexpected round-trip geometry: {counts}')
    if json.loads(export(client, 'bedrock')) != json.loads((ASSET / 'engine_proof.geo.json').read_text()):
        raise RuntimeError('Geometry changed during native project round trip')
    animation = evaluate(client, 'Animator.buildFile(null, Animation.all.map(a => a.name))')
    if animation != json.loads((ASSET / 'engine_proof.animation.json').read_text()):
        raise RuntimeError('Animation changed during native project round trip')
    (ASSET / 'manifest.json').write_text(json.dumps({'id': 'engine_proof', 'project': 'engine_proof.bbmodel',
        'geometry': 'engine_proof.geo.json', 'animation': 'engine_proof.animation.json', 'texture': 'engine_proof.png',
        'required_animations': ['animation.engine_proof.cycle'],
        'purpose': 'Diagnostic toolchain fixture; not approved engine art', 'texels_per_block': 32}, indent=2) + '\n')
    for name, position in [('front', [0, 18, -65]), ('side', [-65, 18, 0]), ('isometric', [-55, 40, -55])]:
        result = client.call('set_camera_angle', {'position': position, 'target': [0, 10, 0], 'projection': 'orthographic'})
        for block in result.get('content', []):
            if block['type'] == 'image':
                (ASSET / f'{name}.png').write_bytes(base64.b64decode(block['data']))
    print('Native project round trip verified:', counts)

if __name__ == '__main__':
    main()
