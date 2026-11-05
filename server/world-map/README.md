# Read-only world map

The public map shows saved terrain, online-player names/positions, observed coal/oil markers, and generated villages. No password is required. Uranium is not mapped. Rendering reads saved chunks without generating terrain or calling a game administration API. [`server/README.md`](../README.md) covers how it is deployed.

The compact 256 px desktop sidebar groups view choices, layer checkboxes and navigation together at the top. Villages, Deposits, Contours and the Coordinate grid share one Layers group, with village/coal/oil counts beside their options. The grid, on by default, draws faint lines and their coordinates at power-of-two block spacings at least 100 screen pixels apart; the origin cross and scale bar always show. Players, World data (including side-by-side World/LOD sizes) and Server usage follow. Controls stay expanded. Sidebar labels use 14 px text, with 16 px button/input text and larger headings and statistics.

## Rendering

One pass per Overworld region file feeds every saved-chunk layer: terrain tiles, heights, deposit markers and village markers. Passes run nearest to 0, 0 first, so the map fills outward like the world, and repeat every 30 seconds. A pass skips a region whose modification time and size are unchanged; otherwise it reads the whole file in one sequential read and redraws exactly the chunks whose region-header location or save time changed since its last pass, all of them in that pass, so a region is never left half-drawn. Only `minecraft:full` chunks are drawn; other saved chunks leave their area empty until they finish. A chunk that fails to decode, typically because Minecraft rewrote it during the read, keeps its previous stamp and is retried on the next pass while the rest of the region is drawn.

Drawing writes a region's colours and heights. Its relief, elevation and contour layers are then derived from those heights inside a one-pixel border of its four neighbours' heights, so slopes and contours run on across region edges; only unknown ground is shaded as level. After each pass's drawing, the regions drawn, added or deleted and their neighbours are derived again, and the tile version clients and overviews key on is the time of that derivation. Startup derives every region again from its stored heights, which also finishes a pass interrupted before this step. A new region is listed once derived.

`regions.sqlite` in the map state directory holds each region's last pass: its signature, header stamps, and per-chunk deposit cells and village starts. The region's tile files hold its pixels and heights, so a pass starts from them. Startup rebuilds the feeds from these rows, the same path as a restart. A pass with a different color or deposit version redraws the whole region; until then its existing tile stays on display.

Regions are independent, so eight worker processes draw them in parallel while the service thread checks signatures and commits each finished region; about 1,800 chunks per second on the server VM. The service runs at `Nice=10`, so Minecraft keeps first claim on the CPU, with `CPUQuota=900%` and `MemoryMax=1G` in [`minecraft-map.service`](minecraft-map.service) leaving room for the workers, HTTP and the overview builder.

The size display has two values in decimal GB (two decimal places): World includes saved world files and terrain cache across all dimensions, excluding Distant Horizons files; LODs includes each dimension's DistantHorizons.sqlite database and its WAL/SHM sidecars. A metadata-only filesystem walk measures allocated disk bytes every five minutes, skips symlink targets and counts hardlinks once. No combined total is displayed.

## Server usage

The sidebar displays server-VM usage (Minecraft, generation, the map and other VM work combined): CPU as a percent of all CPU capacity, RAM used/total in decimal GB, and incoming/outgoing network throughput in decimal MB/s. A small unprivileged `/proc` sampler updates an in-memory `/api/metrics` snapshot every five seconds, independently of rendering; browser polling uses the same interval and pauses while the page is hidden. HTTP requests do not trigger sampling.

CPU is an interval counter delta excluding idle and I/O wait, without double-counting guest counters. Used RAM is MemTotal minus MemAvailable, so reclaimable cache is not treated as committed application usage. Network rates use the active default IPv4 route's interface (lowest route metric); when none exists, they aggregate non-loopback interfaces. Counter resets and interface changes wait for a fresh interval instead of producing spikes. The first CPU/network reading needs two samples. The browser clears failed or more-than-15-second-old samples and bounds each request to four seconds. Only numeric readings and the sample timestamp are public; process lists, raw counters, interface names and routing data are not served. [Kernel counter definitions](https://www.kernel.org/doc/html/latest/filesystems/proc.html).

## Village markers

Blue house markers identify vanilla and modded village-named structure starts recorded in saved full Overworld chunks. The Villages checkbox is independent of Deposits; nearby houses, like deposit markers, cluster when zoomed out on a grid anchored to world coordinates whose cell doubles per zoom step, so panning never merges or splits a cluster, and hovering or tapping shows village coordinates. The position is the center of the saved starting jigsaw piece (usually the town center), with its base Y. These are generated-structure locations, not a census of living villagers or player-built settlements; a demolished village can retain its marker.

Village starts are read in the same region pass as terrain, so a village appears with its terrain. Deleted region files lose their markers on the next pass. Feed failures and empty results have a short status. No seed prediction, game commands, terrain generation or world edits are involved.

## Public boundary

The Python service runs as the unprivileged `minecraft-map` account, listening only on `127.0.0.1:8080`. [`nginx.conf`](nginx.conf) is the public entry point, on the map port from the local deploy configuration, which the router forwards; the distribution's default site is removed. Never forward the Python backend. An unusual port reduces incidental noise but is not an access control.

nginx permits only GET/HEAD for the explicit map page, JavaScript, six public JSON feeds and bounded tile paths. It adds a content security policy, disables framing and MIME sniffing, limits header/body sizes and timeouts, and limits each IP to 60 requests/second with a 512-request burst. The burst accommodates a complete viewport with two layers. Connections are limited to 20 per IP and 32 across the map; the backend independently bounds its threads and socket timeout. These limits constrain ordinary overload; they do not establish resistance to a distributed flood or guarantee capacity for a given player count. No forwarded client address is trusted.

The backend rejects paths outside the tile cache, including symlinks escaping it. Public job/player feeds project only the fields used by the UI. Player labels use text rather than HTML insertion. Source files and their directories must be owned by root and must not be writable by the service or other users. Keep the cache writable only by its service account.

[`minecraft-map.service`](minecraft-map.service) adds to strict filesystem protection, a private temporary directory, the unprivileged user and its resource limits: loopback-only networking, hidden game configuration/logs/mods, an empty capability set and kernel/device restrictions. The existing state directory is the service's writable cache; Minecraft worlds remain read-only in its mount namespace.

The deployment uses a default-deny incoming host firewall, retaining the existing SSH, game and mod UDP ports plus the map port. Do not copy another host's unrelated firewall rules or change SSH/game settings as part of map maintenance.

## Transport and verification

The current entry point is **HTTP**. Trusted HTTPS is still an unfinished deployment step; obtaining a suitable certificate and routing its validation requires a domain or a separately arranged IP-certificate setup. No credentials should be submitted over this HTTP service. Router forwarding, public IPv6 and an actual off-network request require separate verification. This focused hardening is not a vulnerability-free certification; keep Ubuntu/nginx/Python/Pillow security updates current.

[`server/deploy.py`](../deploy.py) installs the source in `/opt/minecraft-map`, with `world.json` (the dimension height and minimum Y that heightmap decoding depends on). Use the Ubuntu security-supported nginx package. Check `nginx -t` before reload, run `python3 -m unittest discover -s server/world-map -p 'test_*.py' -v` with Pillow installed, and verify both allowed routes and rejected traversal/write requests through nginx. The tests use temporary files and real compressed region fixtures, with no game process. Check old ports from another LAN machine, confirm rate limiting returns 429 under a bounded burst, and load the real page to check the CSP permits terrain/layer/player interactions. Host addresses, the map port and deployment credentials stay in the local deploy configuration.
