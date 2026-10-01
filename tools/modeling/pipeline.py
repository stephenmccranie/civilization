"""Asset briefs, generation requests, evidence reviews and publishing for Civilization art."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
from PIL import Image
from assets import confined, validate as validate_gecko, vector

ROOT = Path(__file__).resolve().parents[2]
RUNTIME = ROOT / 'civilization-mod/src/main/resources/assets/civilization'
KINDS = ('item', 'block', 'multiblock', 'animated_machine', 'ui')
CRITERIA = {
    'mockup': ('silhouette_scale', 'implementable_geometry', 'mechanics_clearance', 'period_materials', 'family_consistency', 'controls_readability', 'gameplay_scope'),
    'model': ('matches_direction', 'geometry_uvs', 'material_consistency', 'assembly_interfaces', 'animation_states', 'native_size_readability'),
    'ingame': ('photon_materials', 'scale_readability', 'states_motion', 'interaction_guides', 'inventory_appearance', 'integration_limits'),
}

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def read(path): return json.loads(path.read_text(encoding='utf-8'))
def write(path, value): path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
def fingerprint(asset):
    return hashlib.sha256(json.dumps({k: asset[k] for k in ('kind', 'brief', 'references')}, sort_keys=True).encode()).hexdigest()

def check_brief(asset):
    if asset['kind'] not in KINDS: raise ValueError('Unknown asset kind')
    for key in ('description', 'purpose', 'silhouette', 'bounds', 'materials', 'interfaces', 'states', 'construction', 'invariants'):
        if not asset['brief'].get(key): raise ValueError(f'Complete brief.{key} before generation')
    if not asset['references']: raise ValueError('Name at least one existing visual reference')
    for reference in asset['references']:
        if not reference.get('role'): raise ValueError('Reference needs a role')
        if not confined(ROOT, reference['path']).is_file(): raise ValueError(f'Missing reference {reference["path"]}')

def inputs(asset, folder, stage):
    paths = [confined(ROOT, ref['path']) for ref in asset['references']]
    if stage != 'mockup':
        paths += [confined(folder, source) for source in asset['sources']]
        paths += [confined(folder, output['source']) for output in asset['outputs']]
    return {str(path): sha(path) for path in paths}

def passed(asset, folder, stage):
    reviews = [r for r in asset['reviews'] if r['stage'] == stage]
    if not reviews or reviews[-1]['decision'] != 'pass': raise ValueError(f'{stage}: needs a passing visual review')
    review = reviews[-1]
    if review['brief_hash'] != fingerprint(asset) or review['inputs'] != inputs(asset, folder, stage):
        raise ValueError(f'{stage}: review is stale after source/brief changes')
    for file, checksum in review['evidence'].items():
        if sha(confined(folder, file)) != checksum: raise ValueError(f'{stage}: evidence changed')
    if stage == 'mockup':
        if not asset['generations'] or review['generation'] != asset['generations'][-1]['sha256']:
            raise ValueError('Latest generated iteration has not passed review')

def java_model(model):
    if not model.get('parent') and not model.get('elements'): raise ValueError('Java model needs a parent or elements')
    for cube in model.get('elements', []):
        for key in ('from', 'to'):
            vector(cube[key], 3, key)
            if any(v < -16 or v > 32 for v in cube[key]): raise ValueError('Java 1.21.1 element exceeds -16..32 bounds')
        dimensions = [b - a for a, b in zip(cube['from'], cube['to'])]
        if min(dimensions) < 0 or sum(d > 0 for d in dimensions) < 2: raise ValueError('Invalid cuboid/plane dimensions')
        if 'rotation' in cube:
            rotation = cube['rotation']
            if rotation['axis'] not in ('x', 'y', 'z') or rotation['angle'] not in (-45, -22.5, 0, 22.5, 45):
                raise ValueError('Unsupported Java 1.21.1 element rotation')
        for face in cube.get('faces', {}).values():
            if 'uv' in face:
                vector(face['uv'], 4, 'UV')
                if any(v < 0 or v > 16 for v in face['uv']): raise ValueError('Java UV must be within 0..16')

def validate_outputs(asset, folder):
    if not asset['outputs'] or not asset['sources']: raise ValueError('Declare editable/master sources and runtime outputs first')
    for path in asset['sources']:
        if not confined(folder, path).is_file(): raise ValueError(f'Missing source {path}')
    seen = set()
    formats = set()
    for output in asset['outputs']:
        source = confined(folder, output['source'])
        target = confined(RUNTIME, output['target'])
        if target in seen: raise ValueError('Duplicate runtime target')
        seen.add(target)
        fmt = output['format']; formats.add(fmt)
        if fmt == 'png':
            if source.suffix != '.png' or target.suffix != '.png': raise ValueError('PNG extension mismatch')
            with Image.open(source) as image:
                image.load()
                if list(image.size) != output['size']: raise ValueError(f'Wrong pixel size: {source}')
                if output.get('alpha') == 'opaque' and image.convert('RGBA').getextrema()[3][0] != 255:
                    raise ValueError(f'Unexpected transparency: {source}')
                if output.get('alpha') == 'transparent' and image.convert('RGBA').getextrema()[3][0] == 255:
                    raise ValueError(f'Expected transparent background: {source}')
        elif fmt in ('java_model', 'json', 'gecko_geometry', 'gecko_animation'):
            if target.suffix != '.json': raise ValueError('JSON extension mismatch')
            data = read(source)
            if fmt == 'java_model': java_model(data)
        else: raise ValueError(f'Unsupported output format {fmt}')
    if formats & {'gecko_geometry', 'gecko_animation'} and not asset['gecko_manifests']:
        raise ValueError('GeckoLib outputs need a paired model/animation/texture manifest')
    verified_gecko = set()
    for manifest in asset['gecko_manifests']:
        _, files, _ = validate_gecko(confined(folder, manifest))
        declared = {confined(folder, o['source']) for o in asset['outputs']}
        if not {files[k] for k in ('geometry', 'animation', 'texture')}.issubset(declared):
            raise ValueError('GeckoLib manifest files must be declared outputs')
        verified_gecko.update((files['geometry'], files['animation']))
    for output in asset['outputs']:
        if output['format'].startswith('gecko_') and confined(folder, output['source']) not in verified_gecko:
            raise ValueError('GeckoLib output was not validated by its manifest')

def prompt(asset):
    brief = '\n'.join(f'{key}: {value}' for key, value in asset['brief'].items())
    return f'''Create ONE coherent Minecraft {asset['kind']} mockup direction for Civilization.
{brief}
Visual setting: late nineteenth-century industry developing toward monumental electrical modernism within our Tesla-lifetime alternate history (1856–1943). Follow design_direction.md#setting-and-material-style and the brief’s technological stage. Status reads through precision, craftsmanship, scale and control of energy, not arbitrary gold trim. Raw resources remain materially credible without period ornament.
Use readable cast iron, steel, timber, restrained brass/copper and physical controls as appropriate to the brief.
Distinguish subject/shape, material and project-style references. Match each reference only according to its declared role; an unrelated style reference must not impose its material pattern. Avoid generic steampunk decoration.
For 3D forms: Blockbench-reproducible cuboids, restrained faceted rounds, supported parts, believable mechanical axes and clearance. Draw small seams/rivets in textures. No photoreal mesh detail, tiny modeled bolts, floating fittings or invented gameplay.
For item sprites: a single recognizable silhouette and two or three large landmarks; large connected color clusters, two or three tones per material, hard boundaries. As starting estimates rather than fixed laws, signature accents may occupy 10–15% of the silhouette and strong seams about 7%; choose by subject and native review. Include deliberate material-specific 1–3-final-pixel clusters on broad textured faces, subordinate in contrast to structural edges; do not leave them featureless. Avoid subpixel threads, random speckling, dithering and gradients. High-resolution master, small-icon information budget; inspect the native reduction. See art/textures/sprite-design.md. Optional held view for shaped tools. For UI: native Minecraft slots and the existing cabinet style.
Review against the named neighboring assets for material identity, style membership and visual distinction. Use the declared bounds and output route; multiblocks need an assembled view and consistent part relationships. One useful large view, plus only views needed to explain construction. Same asset in every view. No dimension labels masquerading as exact measurements.
This is a mockup, not a production texture or exported model. Final textures will be derived from high-resolution masters by the existing pipeline. Do not make alternative A/B designs unless requested.
'''

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['init', 'request', 'record', 'review', 'check', 'publish'])
    parser.add_argument('asset', type=Path, help='Asset directory containing asset.json')
    parser.add_argument('--kind', choices=KINDS)
    parser.add_argument('--description')
    parser.add_argument('--image', type=Path)
    parser.add_argument('--prompt', type=Path)
    parser.add_argument('--references', type=Path, nargs='*', default=[])
    parser.add_argument('--stage', choices=CRITERIA, default='mockup')
    parser.add_argument('--review', type=Path, help='JSON: decision, criteria findings, remaining_issues')
    parser.add_argument('--evidence', nargs='*', default=[], help='Paths relative to asset directory')
    parser.add_argument('--complete', action='store_true')
    args = parser.parse_args(); folder = args.asset.resolve(); file = folder / 'asset.json'
    if not folder.is_relative_to(ROOT): raise ValueError('Keep asset records in the project')
    if args.command == 'init':
        if file.exists(): raise ValueError('Asset already exists; edit its brief')
        if not args.kind or not args.description: parser.error('init needs --kind and --description')
        folder.mkdir(parents=True, exist_ok=True)
        write(file, {'schema': 1, 'kind': args.kind, 'brief': {key: args.description if key == 'description' else '' for key in
            ('description','purpose','silhouette','bounds','materials','interfaces','states','construction','invariants','technological_stage','material_behavior','signature_feature','neighboring_assets')},
            'references': [], 'sources': [], 'outputs': [], 'gecko_manifests': [], 'generations': [], 'reviews': []})
        print('Fill the brief and existing style references:', file); return
    asset = read(file); check_brief(asset)
    if args.command == 'request':
        print(json.dumps({'prompt': prompt(asset), 'references': [dict(ref, path=str(confined(ROOT, ref['path']))) for ref in asset['references']],
                          'review_criteria': CRITERIA['mockup']}, indent=2)); return
    if args.command == 'record':
        if not args.image or not args.prompt: parser.error('record needs --image and the exact --prompt file used')
        with Image.open(args.image) as image: image.verify()
        index = len(asset['generations']) + 1
        destination = folder / f'mockup-{index:02}{args.image.suffix.lower()}'
        if destination.exists(): raise ValueError('Refusing to overwrite prior image')
        shutil.copyfile(args.image, destination)
        asset['generations'].append({'file': destination.name, 'source': str(args.image.resolve()), 'sha256': sha(destination),
            'prompt': args.prompt.read_text(encoding='utf-8'), 'brief_hash': fingerprint(asset),
            'references': {str(p.resolve()): sha(p) for p in args.references}})
        write(file, asset); print(destination); return
    if args.command == 'review':
        if not args.review or not args.evidence: parser.error('review needs --review findings.json and --evidence image paths')
        review = read(args.review)
        if args.stage in review: review = review[args.stage]
        if review['decision'] not in ('pass', 'revise'): raise ValueError('Decision must be pass or revise')
        if any(not str(review['criteria'].get(key, '')).strip() for key in CRITERIA[args.stage]):
            raise ValueError(f'Record specific findings for {CRITERIA[args.stage]}')
        if review['decision'] == 'pass' and review['remaining_issues']: raise ValueError('Fix blocking issues before passing')
        if args.stage != 'mockup':
            passed(asset, folder, 'mockup'); validate_outputs(asset, folder)
        if args.stage == 'ingame': passed(asset, folder, 'model')
        if args.stage == 'mockup':
            if not asset['generations']: raise ValueError('Record the generated mockup first')
            latest = asset['generations'][-1]
            if latest['brief_hash'] != fingerprint(asset): raise ValueError('Mockup was generated for a different brief')
            if sha(confined(folder, latest['file'])) != latest['sha256']: raise ValueError('Recorded mockup image changed')
            for reference, checksum in latest['references'].items():
                if sha(Path(reference)) != checksum: raise ValueError('Generation reference changed; record a new iteration')
            if latest['file'] not in args.evidence: raise ValueError('Review the latest mockup image')
        review.update(stage=args.stage, brief_hash=fingerprint(asset), inputs=inputs(asset, folder, args.stage),
            evidence={p: sha(confined(folder, p)) for p in args.evidence},
            generation=asset['generations'][-1]['sha256'])
        asset['reviews'].append(review); write(file, asset); print(review['decision']); return
    passed(asset, folder, 'mockup'); validate_outputs(asset, folder); passed(asset, folder, 'model')
    if args.complete: passed(asset, folder, 'ingame')
    receipt = {'brief_hash': fingerprint(asset), 'inputs': inputs(asset, folder, 'model'),
               'outputs': {o['target']: sha(confined(folder, o['source'])) for o in asset['outputs']}}
    if args.command == 'publish':
        for output in asset['outputs']:
            target = confined(RUNTIME, output['target']); target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(confined(folder, output['source']).read_bytes())
        write(folder / 'publish-receipt.json', receipt)
    else:
        if read(folder / 'publish-receipt.json') != receipt: raise ValueError('Publish receipt is stale')
        for path, checksum in receipt['outputs'].items():
            if sha(confined(RUNTIME, path)) != checksum: raise ValueError(f'Runtime output changed: {path}')
    print('Validated', len(asset['outputs']), 'runtime files; visual review is agent-authored, not machine-assessed.')

if __name__ == '__main__': main()
