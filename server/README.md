# Live server

Everything the dedicated server runs besides the game and its mods: its services, scripts and the settings this repository decides. [`deploy.py`](deploy.py) is the only way they reach the server, so the server always matches a commit.

| Directory | What it runs |
| --- | --- |
| [`minecraft/`](minecraft) | The game server: its service, the start script that feeds the console pipe `/run/minecraft/console`, the JVM arguments, the world-generation CPU cap, the VM's swappiness and the world datapacks (the scale-4 world preset and the river and woodland biome tags). |
| [`pregeneration/`](pregeneration) | The controller that pregenerates chunks and LODs outward from spawn while nobody is online ([details](pregeneration/README.md)). |
| [`world-map/`](world-map) | The public read-only map and its nginx entry point ([details](world-map/README.md)). |

`deploy.py` also holds the few keys this repository decides in files the game and Distant Horizons write themselves: `server.properties` and `config/DistantHorizons.toml`. What players see of them is in the [player pack's server settings](../modpack/README.md#starting-settings).

## Deploying

`server/deploy.local.json`, never committed, names the host, the SSH key and the map's public port:

```json
{"host": "user@address", "key": "~/.ssh/key-file", "map_port": 12345}
```

- `python server/deploy.py check` lists every managed file and setting that differs on the server, and exits 1 if any does.
- `python server/deploy.py install` writes what differs, owned by `minecraft` under `/srv/minecraft` and by root elsewhere, then reloads systemd, nginx or sysctl as needed and checks again.

It never restarts a service, because restarting the game disconnects players. The map and the pregeneration controller can restart at any time; the controller pauses while players are online and rides through a game restart. Restart the game by writing `stop` to the console pipe, so the world saves, then `systemctl start minecraft`. A Civilization or Terra Diffusion jar is replaced in `/srv/minecraft/mods` while the game is stopped.

Run the components' tests on Linux, as the server does:

```sh
python3 -m unittest discover -s server/pregeneration -v
python3 -m unittest discover -s server/world-map -p 'test_*.py' -v
```

## What stays local

The server's address, the map port, SSH keys and passwords, the world seed and the rest of `server.properties`, worlds, player data, logs and the host firewall rules are not in this repository.
