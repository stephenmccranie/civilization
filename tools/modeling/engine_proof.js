// Disposable authoring fixture, not the engine's final art. Run via the MCP plugin's risky_eval.
// No filesystem/network access: native Blockbench objects and codecs only.
(() => {
    if (!Formats.geckolib_model) throw new Error('Load the pinned GeckoLib Blockbench plugin first');
    Project.name = 'engine_proof';
    Project.geometry_name = 'engine_proof';
    Project.geckolib_model_type = 'block';
    Project.texture_width = 128;
    Project.texture_height = 128;
    Project.box_uv = false;
    const root = new Group({name: 'root', origin: [0, 0, 0]}).init();
    const wheel = new Group({name: 'flywheel', origin: [-10, 13, 0]}).addTo(root).init();
    const piston = new Group({name: 'piston', origin: [0, 13, 0]}).addTo(root).init();
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 128;
    const ctx = canvas.getContext('2d');
    // Deliberate diagnostic atlas: seams/density are visible. Replace with reviewed art later.
    for (let y = 0; y < 128; y++) for (let x = 0; x < 128; x++) {
        ctx.fillStyle = (Math.floor(x / 8) + Math.floor(y / 8)) % 2 ? '#65757b' : '#87979b';
        if (x % 32 === 0 || y % 32 === 0) ctx.fillStyle = '#c7a568';
        ctx.fillRect(x, y, 1, 1);
    }
    const texture = new Texture({name: 'engine_proof.png'}).fromDataURL(canvas.toDataURL()).add(false);
    const cube = (name, from, to, group = root, rotation = [0, 0, 0], pivot = [0, 0, 0]) => {
        const element = new Cube({name, from, to, rotation, origin: pivot, autouv: 0, box_uv: false}).addTo(group).init();
        const [x, y, z] = to.map((v, i) => v - from[i]);
        const sizes = {north: [x, y], south: [x, y], east: [z, y], west: [z, y], up: [x, z], down: [x, z]};
        for (const [face, [u, v]] of Object.entries(sizes)) {
            element.faces[face].texture = texture.uuid;
            element.faces[face].uv = [0, 0, u * 2, v * 2];
        }
        return element;
    };
    cube('bed', [-14, 0, -9], [14, 3, 9]);
    cube('bearing', [-12, 3, -3], [-8, 11, 3]);
    cube('cylinder', [-2, 5, -5], [10, 17, 5]);
    cube('rod', [-10, 12, -1], [2, 14, 1], piston);
    cube('hub', [-12, 11, -2], [-8, 15, 2], wheel);
    for (let i = 0; i < 8; i++) {
        cube('rim_' + i, [-11.5, 20.5, -3.3], [-8.5, 22.5, 3.3], wheel, [i * 45, 0, 0], [-10, 13, 0]);
        if (i % 2 === 0) cube('spoke_' + i, [-10.6, 13, -0.6], [-9.4, 21, 0.6], wheel, [i * 45, 0, 0], [-10, 13, 0]);
    }
    Canvas.updateAll();
    return {project: Project.name, bones: Group.all.length, cubes: Cube.all.length, texture: texture.uuid};
})()
