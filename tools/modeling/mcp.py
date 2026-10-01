"""Small HTTP MCP client for the pinned Blockbench plugin; usable without a task restart.

python tools/modeling/mcp.py list
python tools/modeling/mcp.py call tool_name arguments.json --out .tools/modeling/result
"""
import argparse
import base64
import json
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ENDPOINT = 'http://127.0.0.1:3000/bb-mcp'

class Client:
    def __init__(self):
        self.session = None
        self.sequence = 0
        self.protocol = '2024-11-05'
        result = self.request('initialize', {
            'protocolVersion': self.protocol,
            'capabilities': {},
            'clientInfo': {'name': 'civilization-model-tools', 'version': '1.0.0'},
        })
        self.protocol = result.get('protocolVersion', self.protocol)
        self.request('notifications/initialized', notification=True)

    def request(self, method, params=None, notification=False):
        self.sequence += 1
        payload = {'jsonrpc': '2.0', 'method': method}
        if params is not None:
            payload['params'] = params
        if not notification:
            payload['id'] = self.sequence
        headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream',
                   'MCP-Protocol-Version': self.protocol}
        if self.session:
            headers['Mcp-Session-Id'] = self.session
        request = Request(ENDPOINT, data=json.dumps(payload).encode(), headers=headers)
        with urlopen(request, timeout=45) as response:
            self.session = response.headers.get('Mcp-Session-Id', self.session)
            if notification or response.status == 202:
                return None
            if 'text/event-stream' in response.headers.get('Content-Type', ''):
                lines = []
                message = None
                for raw in response:
                    line = raw.decode().rstrip('\r\n')
                    if line.startswith('data:'):
                        lines.append(line[5:].lstrip())
                    elif not line and lines:
                        candidate = json.loads('\n'.join(lines))
                        lines = []
                        if candidate.get('id') == payload['id']:
                            message = candidate
                            break
                if message is None:
                    raise RuntimeError('MCP stream closed without a matching response')
            else:
                message = json.load(response)
        if 'error' in message:
            raise RuntimeError(json.dumps(message['error']))
        return message['result']

    def call(self, name, arguments):
        result = self.request('tools/call', {'name': name, 'arguments': arguments})
        if result.get('isError'):
            raise RuntimeError(json.dumps(result))
        return result

    def close(self):
        if self.session:
            try:
                with urlopen(Request(ENDPOINT, method='DELETE', headers={'Mcp-Session-Id': self.session}), timeout=5):
                    pass
            except (HTTPError, URLError):
                pass

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['list', 'call'])
    parser.add_argument('tool', nargs='?')
    parser.add_argument('arguments', type=Path, nargs='?')
    parser.add_argument('--out', type=Path, help='Save full response and extracted images here')
    args = parser.parse_args()
    if args.action == 'call' and not args.tool:
        parser.error('call requires a tool name')
    client = Client()
    try:
        if args.action == 'list':
            result = client.request('tools/list')
        else:
            arguments = json.loads(args.arguments.read_text(encoding='utf-8')) if args.arguments else {}
            result = client.call(args.tool, arguments)
        if args.out:
            args.out.mkdir(parents=True, exist_ok=True)
            (args.out / 'response.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        for i, block in enumerate(result.get('content', [])):
            if block.get('type') == 'image':
                data = block.pop('data')
                if args.out:
                    extension = {'image/png': '.png', 'image/jpeg': '.jpg'}.get(block['mimeType'], '.bin')
                    path = args.out / f'image-{i}{extension}'
                    path.write_bytes(base64.b64decode(data))
                    block['saved_path'] = str(path.resolve())
                else:
                    block['note'] = 'Use --out to save image data'
        print(json.dumps(result, indent=2))
    finally:
        client.close()

if __name__ == '__main__':
    try:
        main()
    except URLError as error:
        raise SystemExit(f'Cannot connect to Blockbench at {ENDPOINT}. Open the editor with MCP loaded. {error}')
